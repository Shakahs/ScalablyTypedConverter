package org.scalablytyped.converter.internal
package importer

import com.olvind.logging.{Formatter, Logger}
import org.scalablytyped.converter.Selection
import org.scalablytyped.converter.internal.maps._
import org.scalablytyped.converter.internal.phases.{GetDeps, IsCircular, Phase, PhaseRes}
import org.scalablytyped.converter.internal.ts.TsTreeScope.LoopDetector
import org.scalablytyped.converter.internal.ts.modules.ModuleAsGlobalNamespace
import org.scalablytyped.converter.internal.ts.{transforms => T, _}
import ox.mapPar

import scala.annotation.tailrec
import scala.collection.immutable.{SortedMap, SortedSet}
import scala.collection.mutable

/**
  * This phase parses files, implements the module system, and "implements" a bunch of typescript features by rewriting the tree.
  * For instance defaulted parameters are filled in. The point is to go from a complex tree to a simpler tree
  */
class Phase1ReadTypescript(
    resolve:                 LibraryResolver,
    calculateLibraryVersion: CalculateLibraryVersion,
    ignored:                 Set[TsIdentLibrary],
    ignoredModulePrefixes:   Set[List[String]],
    pedantic:                Boolean,
    parser:                  InFile => Either[String, TsParsedFile],
    parseParallelism:        Int,
    expandTypeMappings:      Selection[TsIdentLibrary],
) extends Phase[LibTsSource, LibTsSource, LibTs] {

  implicit val InFileFormatter: Formatter[InFile] =
    inFile =>
      if (inFile.path.segments.length > 3)
        inFile.path.segments.toList.takeRight(3).mkString("../", "/", "")
      else inFile.path.segments.mkString("/")

  implicit val InFolderFormatter: Formatter[InFolder] =
    _.path.toString

  def ignoreModule(modName: TsIdentModule): Boolean =
    (1 to modName.fragments.length).exists(n => ignoredModulePrefixes(modName.fragments.take(n)))

  /** The files within the same library which `file` imports from or re-exports */
  def referencedLocalFiles(file: InFile): IArray[InFile] =
    parser(file) match {
      case Left(_) => Empty
      case Right(parsed) =>
        val specs: IArray[TsIdentModule] =
          parsed.imports.collect {
            case TsImport(_, _, TsImportee.From(from))     => from
            case TsImport(_, _, TsImportee.Required(from)) => from
          } ++ parsed.exports.collect {
            case TsExport(_, _, _, TsExportee.Names(_, Some(from))) => from
            case TsExport(_, _, _, TsExportee.Star(_, from))        => from
          }

        specs.mapNotNone(spec => LibraryResolver.file(file.folder, spec.value))
    }

  /** The files `file` inlines through its `reference` directives, and the directives which didn't resolve */
  def inlinedFiles(
      source: LibTsSource,
      file:   InFile,
      parsed: TsParsedFile,
  ): IArray[Either[Directive.Ref, InFile]] =
    parsed.directives.collect {
      case dir @ Directive.PathRef(stringPath) =>
        LibraryResolver.file(file.folder, stringPath).toRight(dir)
      case dir @ Directive.LibRef(value) if source.libName === TsIdent.std =>
        LibraryResolver.file(resolve.stdLib.folder, s"lib.$value.d.ts").toRight(dir)
    }

  /**
    * Every file the preprocessing in [[apply]] will ask for, parsed, `parseParallelism` files at a time.
    *
    * Parsing a file needs nothing from any other file, while the preprocessing which follows it cannot be
    * spread out the same way: a file inlines the files its directives point to, and [[Lazy]] tells a cycle
    * from an ordinary revisit by finding a computation already in progress, which only holds within one
    * thread. So the parsing is lifted out of it and done first, and the preprocessing finds the files it
    * asks for already parsed.
    *
    * Only a parsed file says which files it inlines, so the set is taken in waves: the roots, then what
    * those point to, and so on. A file outside `included` is left out, just as the preprocessing refuses
    * to inline one. This is the same set of files the preprocessing would have parsed by itself.
    */
  def parseFiles(
      source:   LibTsSource,
      included: Set[InFile],
      roots:    IArray[InFile],
  ): Map[InFile, Either[String, TsParsedFile]] = {
    @tailrec
    def go(
        pending: Vector[InFile],
        parsed:  Map[InFile, Either[String, TsParsedFile]],
    ): Map[InFile, Either[String, TsParsedFile]] =
      pending.distinct.filterNot(parsed.contains) match {
        case Vector() => parsed
        case wave =>
          val parseOne: InFile => (InFile, Either[String, TsParsedFile]) = file => file -> parser(file)

          val parsedWave: Vector[(InFile, Either[String, TsParsedFile])] =
            if (wave.sizeIs < 2 || parseParallelism < 2) wave.map(parseOne)
            else wave.mapPar(parseParallelism)(parseOne)

          val referenced: Vector[InFile] =
            parsedWave.flatMap {
              case (file, Right(parsed)) => inlinedFiles(source, file, parsed).mapNotNone(_.toOption).toVector
              case (_, Left(_))          => Vector.empty
            }

          go(referenced.filter(included), parsed ++ parsedWave)
      }

    go(roots.toVector.filter(included), Map.empty)
  }

  override def apply(
      source:     LibTsSource,
      _1:         LibTsSource,
      getDeps:    GetDeps[LibTsSource, LibTs],
      isCircular: IsCircular,
      logger:     Logger[Unit],
  ): PhaseRes[LibTsSource, LibTs] = {
    source match {
      case source if ignored(source.libName) || isCircular => PhaseRes.Ignore()
      case source =>
        val (includedFiles, listMs) = timed[IArray[InFile]] {
          source match {
            case LibTsSource.StdLibSource(_, files, _) =>
              PathsFromTsLibSource.filesFrom(files.head.folder)
            case f @ LibTsSource.FromFolder(_, TsIdentLibrarySimple("typescript")) =>
              /* don't include std */
              f.shortenedFiles
            case f: LibTsSource.FromFolder =>
              /* There are often whole trees parallel to what is specified in `typings` (or similar). This ignores them */
              val bound = f.shortenedFiles.headOption.map(_.folder).getOrElse(f.folder)
              /* ... except when the declared entry only re-exports a tree beside it, like `@stripe/stripe-js` whose
               * `lib/index.d.ts` is nothing but `export * from '../dist'`. That tree is the library, not a parallel
               * copy of it, so the whole package is included instead of an entry which declares nothing. */
              if (f.shortenedFiles.forall(file => referencedLocalFiles(file).forall(_.path.startsWith(bound.path))))
                PathsFromTsLibSource.filesFrom(bound)
              else PathsFromTsLibSource.filesFrom(f.folder)
          }
        }
        logger.warn(s"listed ${includedFiles.length} files in $listMs ms")

        val (parsedFiles, parseMs) = timed {
          /* For the stdlib only the files the wanted `lib.*.d.ts` pull in are preprocessed, not every
           * file beside them in typescript's `lib` folder, so those are the roots to follow. */
          val roots = source match {
            case LibTsSource.StdLibSource(_, files, _) => files
            case _: LibTsSource.FromFolder => includedFiles
          }
          parseFiles(source, includedFiles.toSet, roots)
        }
        logger.warn(s"parsed ${parsedFiles.size} files in $parseMs ms")

        val includedViaDirective = mutable.Set.empty[InFile]

        lazy val preparingFiles: SortedMap[InFile, Lazy[(TsParsedFile, Set[LibTsSource])]] =
          includedFiles.sorted
            .map { file =>
              file -> Lazy {
                parsedFiles.getOrElse(file, parser(file)) match {
                  case Left(msg) =>
                    logger.withContext(file).fatal(s"Couldn't parse: $msg")
                  case Right(parsed) =>
                    val deps = Set.newBuilder[LibTsSource]

                    val fileLogger = logger.withContext(file)
                    fileLogger.info(s"Preprocessing")

                    val toInline: IArray[Either[Directive.Ref, InFile]] =
                      inlinedFiles(source, file, parsed)

                    val moduleNames = LibraryResolver.moduleNameFor(source, file)

                    val withInferredModule = modules.InferredDefaultModule(parsed, moduleNames.head, logger)

                    withInferredModule.directives.foreach {
                      case dir @ Directive.TypesRef(value) =>
                        resolve.module(source, file.folder, value) match {
                          case Some(ResolvedModule.NotLocal(depSource, _)) =>
                            deps += depSource
                          case Some(ResolvedModule.Local(depSource, _)) =>
                            logger.warn(s"unexpected typeref from local file $depSource")
                          case _ =>
                            logger.warn(s"directives: couldn't resolve $dir")
                        }
                      case _ => ()
                    }

                    /* Resolve all references to other modules in `from` clauses, rename modules */
                    val ResolveExternalReferences.Result(withExternals, resolvedModules, unresolvedModules) =
                      ResolveExternalReferences(resolve, source, file.folder, withInferredModule, logger)

                    resolvedModules.foreach {
                      case ResolvedModule.NotLocal(source, _) => deps += source
                      case _                                  => ()
                    }

                    val withOrigin = {
                      source match {
                        case LibTsSource.StdLibSource(_, _, _) =>
                          val shortName = file.path.last.split("\\.").drop(1).dropRight(2).mkString(".")
                          if (shortName.nonEmpty) {
                            val stdComment = Comments(List(Comment(s"/* standard $shortName */\n")))
                            T.AddComments(stdComment).visitTsParsedFile(())(withExternals)
                          } else withExternals

                        case _ =>
                          withExternals
                      }
                    }

                    val inferredDepNames: Set[TsIdentLibrary] =
                      modules.InferredDependency(
                        source.libName,
                        withOrigin,
                        unresolvedModules,
                        logger,
                      )

                    inferredDepNames.foreach { libraryName =>
                      resolve.module(source, file.folder, libraryName.value) match {
                        case Some(ResolvedModule.NotLocal(dep, _)) =>
                          deps += dep
                        case _ =>
                          logger.warn(s"Couldn't resolve inferred dependency ${libraryName.value}")
                      }
                    }

                    val withInlined: TsParsedFile =
                      toInline.distinct.foldLeft(withOrigin) {
                        case (parsed, Right(referencedFile)) =>
                          val referencedFileLogger = fileLogger.withContext(referencedFile)

                          preparingFiles.get(referencedFile).flatMap(_.get) match {
                            case Some((toInline, depsForInline)) if !toInline.isModule =>
                              includedViaDirective += referencedFile
                              deps ++= depsForInline
                              FlattenTrees.mergeFile(parsed, toInline)
                            case Some((modFile, _)) if modFile.isModule =>
                              referencedFileLogger.warn("directives: referenced file was a module")
                              parsed
                            case _ =>
                              referencedFileLogger.warn("directives: reference caused circular graph")
                              parsed
                          }
                        case (parsed, Left(dir)) =>
                          fileLogger.warn(s"directives: couldn't resolve $dir")
                          parsed
                      }

                    val _3 = moduleNames match {
                      case IArray.exactlyOne(_) => withInlined
                      case more =>
                        withInlined.copy(members = withInlined.members.map {
                          case m: TsDeclModule if more.contains(m.name) =>
                            m.copy(comments = m.comments + Marker.ModuleAliases(more.filterNot(_ === m.name)))
                          case other => other
                        })
                    }
                    val _4 = T.SetCodePath.visitTsParsedFile(CodePath.HasPath(source.libName, TsQIdent.empty))(_3)

                    (_4, deps.result())
                }
              }
            }
            .toMap
            .toSorted

        val (preparedFiles, preprocessMs) = timed {
          // evaluate all, don't refactor and combine this with other steps
          val base: SortedMap[InFile, (TsParsedFile, Set[LibTsSource])] =
            source match {
              case LibTsSource.StdLibSource(_, files, _) =>
                val b = SortedMap.newBuilder[InFile, (TsParsedFile, Set[LibTsSource])]
                files.foreach { file =>
                  for {
                    found <- preparingFiles.get(file)
                    evaluated <- found.get
                  } b += ((file, evaluated))
                }
                b.result()
              case LibTsSource.FromFolder(_, _) =>
                preparingFiles.mapNotNone { case (_, v) => v.get }
            }

          base.flatMapToIArray {
            case (file, _) if includedViaDirective(file) => Empty
            case (_, fileResult)                         => IArray(fileResult)
          }
        }
        logger.warn(s"preprocessed ${preparedFiles.length} files in $preprocessMs ms")

        if (preparedFiles.isEmpty) {
          logger.warn(s"No typescript definitions files found for library ${source.libName.value}")
          return PhaseRes.Ignore()
        }

        val (flattened, flattenMs) = timed(FlattenTrees(preparedFiles.map(_._1)))
        logger.warn(s"flattened ${preparedFiles.length} files in $flattenMs ms")
        val depsFromFiles = preparedFiles.foldLeft(Set.empty[LibTsSource]) { case (acc, (_, deps)) => acc ++ deps }

        val (withExportedModules, exportsMs) = timed {
          source.packageJsonOpt.flatMap(_.parsedExported).foldLeft(flattened) {
            case (file, exports) =>
              val proxyModules = ProxyModule.fromExports(
                source,
                logger,
                resolve,
                existing = file.membersByName.contains,
                exports,
              )
              file.copy(members = IArray.fromTraversable(proxyModules).map(_.asModule) ++ file.members)
          }
        }
        logger.warn(s"added exported modules in $exportsMs ms")

        val withFilteredModules: TsParsedFile =
          if (ignoredModulePrefixes.nonEmpty)
            withExportedModules.copy(members = withExportedModules.members.filterNot {
              case x: TsDeclModule      => ignoreModule(x.name)
              case x: TsAugmentedModule => ignoreModule(x.name)
              case _ => false
            })
          else withExportedModules

        val stdlibSourceOpt: Option[LibTsSource] =
          if (includedFiles.exists(_.path === resolve.stdLib.path)) None else Option(resolve.stdLib)

        val (depsDeclared, declaredMs) = timed[SortedSet[LibTsSource]] {
          if (stdlibSourceOpt.isEmpty) SortedSet.empty
          else
            source.packageJsonOpt.getOrElse(PackageJson.Empty).allLibs(dev = false, peer = true).keySet.flatMap {
              depName =>
                resolve.library(depName) match {
                  case LibraryResolver.Found(source) =>
                    Some(source)
                  case LibraryResolver.Ignored(_) =>
                    None
                  case LibraryResolver.NotAvailable(name) =>
                    logger.warn(s"Could not find typescript definitions for dependency ${name.value}")
                    None
                }
            }
        }
        logger.warn(s"resolved declared dependencies in $declaredMs ms")

        val (linkedDeps, linkMs) = timed(getDeps(depsDeclared ++ stdlibSourceOpt ++ depsFromFiles))
        logger.warn(s"linked dependencies in $linkMs ms")

        linkedDeps.map { deps =>
          val ((transitiveDeps, scope), scopeMs) = timed {
            val transitiveDeps = deps.foldLeft(deps) {
              case (acc, (_, lib)) => acc ++ lib.transitiveDependencies
            }
            val scope: TsTreeScope.Root =
              TsTreeScope(
                source.libName,
                pedantic,
                transitiveDeps.map { case (source, lib) => source -> lib.parsed },
                logger,
              )
            (transitiveDeps, scope)
          }
          logger.warn(s"built the scope over ${transitiveDeps.size} dependencies in $scopeMs ms")

          val involvesReact = {
            val react = TsIdentLibrarySimple("react")
            source.libName === react || deps.exists { case (s, _) => s.libName === react }
          }
          val (finished, transformMs) = timed {
            Phase1ReadTypescript
              .Pipeline(scope, source.libName, expandTypeMappings, involvesReact)
              .foldLeft(withFilteredModules) {
                case (acc, (name, f)) =>
                  val (ret, elapsed) = timed(f(acc))
                  /* Most of the twenty-odd passes are quick, and the few which aren't are what a slow
                   * conversion is made of, so those get a line of their own in a normal run. */
                  if (elapsed > 1000) logger.warn(s"transform $name took $elapsed ms")
                  else logger.info(s"transform $name took $elapsed ms")
                  ret
              }
          }
          logger.warn(s"typescript transforms took $transformMs ms")

          val (version, versionMs) = timed {
            calculateLibraryVersion(
              source.folder,
              source.isInstanceOf[LibTsSource.StdLibSource],
              source.packageJsonOpt,
              finished.comments,
            )
          }
          logger.warn(s"calculated the version in $versionMs ms")

          LibTs(source)(version, finished, deps)
        }
    }
  }
}

object Phase1ReadTypescript {

  /** Each pass paired with the name it is timed under, the way [[Phase2ToScalaJs]] names its own */
  def Pipeline(
      scope:              TsTreeScope.Root,
      libName:            TsIdentLibrary,
      expandTypeMappings: Selection[TsIdentLibrary],
      involvesReact:      Boolean,
  ): List[(String, TsParsedFile => TsParsedFile)] =
    List(
      "LibrarySpecific" -> T
        .LibrarySpecific(libName)
        .fold[TsParsedFile => TsParsedFile](identity)(
          _.visitTsParsedFile(scope),
        ),
      "SetJsLocation" -> T.SetJsLocation.visitTsParsedFile(JsLocation.Global(TsQIdent.empty)),
      "SimplifyParents+RemoveStubs+InferTypeFromExpr+InferEnumTypes+NormalizeFunctions+MoveStatics" -> (T.SimplifyParents >>
        T.RemoveStubs >> //before HandleCommonJsModules and QualifyReferences
        T.InferTypeFromExpr >>
        T.InferEnumTypes /* before InlineConstEnum */ >>
        T.NormalizeFunctions /* before FlattenTrees */ >>
        T.MoveStatics).visitTsParsedFile(scope.enableUnqualifiedLookup.caching),
      "HandleCommonJsModules+RewriteExportStarAs" -> (modules.HandleCommonJsModules >> modules.RewriteExportStarAs)
        .visitTsParsedFile(scope), //before QualifyReferences
      "QualifyReferences" -> new T.QualifyReferences(skipValidation = false)
        .visitTsParsedFile(scope.enableUnqualifiedLookup.caching),
      "AugmentModules" -> modules.AugmentModules(scope.caching),
      "ResolveTypeQueries" -> T.ResolveTypeQueries
        .visitTsParsedFile(scope.enableUnqualifiedLookup.caching), // before ReplaceExports
      "ReplaceExports" -> new modules.ReplaceExports(LoopDetector.initial)
        .visitTsParsedFile(scope.enableUnqualifiedLookup.caching),
      "ModuleAsGlobalNamespace" -> (file => ModuleAsGlobalNamespace(scope.root.libName, file)),
      "MoveGlobals" -> modules.MoveGlobals.apply,
      "FlattenTrees" -> FlattenTrees.apply,
      "DefaultedTypeArguments+TypeAliasIntersection+RejiggerIntersections" -> (
        T.DefaultedTypeArguments >> //after FlattenTrees
          T.TypeAliasIntersection >> // before ExpandTypeMappings
          T.RejiggerIntersections
      ).visitTsParsedFile(scope.caching),
      "ExpandTypeMappings" -> (
        if (expandTypeMappings(libName)) T.ExpandTypeMappings.visitTsParsedFile(scope.caching) else identity
      ), // before ExtractInterfaces
      "ExpandTypeMappings.After" -> (
        if (expandTypeMappings(libName)) T.ExpandTypeMappings.After.visitTsParsedFile(scope.caching) else identity
      ), // before ExtractInterfaces
      "TypeAliasToConstEnum+ForwardCtors+ExpandTypeParams+UnionTypesFromKeyOf+DropProperties+InferReturnTypes+RewriteTypeThis+InlineConstEnum+InlineTrivial" -> (
        T.TypeAliasToConstEnum >>
          T.ForwardCtors >>
          T.ExpandTypeParams >>
          T.UnionTypesFromKeyOf >>
          T.DropProperties >>
          T.InferReturnTypes >>
          T.RewriteTypeThis >>
          T.InlineConstEnum >>
          T.InlineTrivial
      ).visitTsParsedFile(scope.caching),
      "ResolveTypeLookups" -> T.ResolveTypeLookups
        .visitTsParsedFile(scope.caching), //before ExpandCallables and ExtractInterfaces, after InlineTrivialTypeAlias and ExpandKeyOfTypeParams
      "PreferTypeAlias" -> (x => T.PreferTypeAlias(x, scope)), // before extract interfaces
      "ExtractInterfaces" -> T.ExtractInterfaces(
        libName,
        TsIdent("anon"),
        scope.caching,
      ), // before things which break initial ordering of members, like `ExtractClasses`
      "ExtractClasses+ExpandCallables" -> (
        if (involvesReact) T.ExtractClasses
        else T.ExtractClasses >> T.ExpandCallables
      ).visitTsParsedFile(scope.caching),
      "SplitMethods+RemoveDifficultInheritance+VarToNamespace" -> (T.SplitMethods /* after ExpandCallables */ >>
        T.RemoveDifficultInheritance >>
        T.VarToNamespace // after ExtractClasses
      ).visitTsParsedFile(scope.caching),
    )
}
