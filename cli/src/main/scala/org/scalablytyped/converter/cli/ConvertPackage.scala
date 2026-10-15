package org.scalablytyped.converter.cli

import com.olvind.logging.{stdout, LogLevel}
import org.scalablytyped.converter.Flavour
import org.scalablytyped.converter.internal.importer.{ConversionOptions, PackageConversion}
import org.scalablytyped.converter.internal.scalajs.Versions
import org.scalablytyped.converter.internal.sets.SetOps
import org.scalablytyped.converter.internal.ts.TsIdentLibrary
import org.scalablytyped.converter.internal.{IArray, InFolder}
import scopt.{OParser, OParserBuilder}

import java.nio.file.attribute.FileTime
import java.util.zip.{ZipEntry, ZipOutputStream}
import scala.collection.immutable.SortedSet

/**
  * Converts one npm package, for a build that converts each package in an action of its own.
  *
  * It reads the package's declarations and the interfaces of the libraries it reaches, and writes the package's Scala
  * sources as a srcjar and its own interface for the packages written against it.
  */
object ConvertPackage {
  case class Config(
      conversion:   ConversionOptions,
      library:      Option[TsIdentLibrary],
      packageDir:   Option[os.Path],
      dependencies: Vector[os.Path],
      srcjar:       Option[os.Path],
      interface:    Option[os.Path],
  )

  /* The placeholder a library with no declarations compiles from: the compiler refuses no sources at all. */
  private val NoDeclarations = "package typings\n"

  /* Entries are stamped with this rather than the clock, so equal sources make equal bytes. */
  private val EntryTime = FileTime.fromMillis(315532800000L)

  val Parser: OParser[Unit, Config] = {
    import Main.{ReadsFlavour, ReadsOsPath, ReadsTsIdentLibrary, ReadsVersionsScala, ReadsVersionsScalaJs}
    val builder: OParserBuilder[Config] = OParser.builder[Config]
    import builder._
    OParser.sequence(
      programName("stc-package"),
      opt[TsIdentLibrary]("library")
        .required()
        .action((x, c) => c.copy(library = Some(x)))
        .text("The library the package holds: its npm name, `@types/` names included, or `std` for typescript"),
      opt[os.Path]("package")
        .required()
        .action((x, c) => c.copy(packageDir = Some(x)))
        .text("The package's directory"),
      opt[os.Path]("dependency")
        .unbounded()
        .action((x, c) => c.copy(dependencies = c.dependencies :+ x))
        .text("The interface of a library the package reaches; one for each"),
      opt[os.Path]("srcjar")
        .required()
        .action((x, c) => c.copy(srcjar = Some(x)))
        .text("Where to write the package's Scala sources"),
      opt[os.Path]("interface")
        .required()
        .action((x, c) => c.copy(interface = Some(x)))
        .text("Where to write the package's interface"),
      opt[Flavour]("flavour")
        .action((x, c) => c.copy(conversion = c.conversion.copy(flavour = x))),
      opt[Versions.Scala]("scala")
        .action((x, c) => c.copy(conversion = c.conversion.copy(versions = c.conversion.versions.copy(scala = x)))),
      opt[Versions.ScalaJs]("scalajs")
        .action((x, c) => c.copy(conversion = c.conversion.copy(versions = c.conversion.versions.copy(scalaJs = x)))),
      opt[Seq[String]]("ignoredLibs")
        .action((x, c) => c.copy(conversion = c.conversion.copy(ignored = x.toSet.sorted))),
      opt[Seq[String]]("stdlib")
        .action((x, c) => c.copy(conversion = c.conversion.copy(stdLibs = x.toSet.sorted))),
    )
  }

  def main(args: Array[String]): Unit =
    OParser.parse(Parser, args, Config(Main.DefaultOptions, None, None, Vector.empty, None, None)) match {
      case None => System.exit(2)
      case Some(c) =>
        val sources = os.temp.dir(prefix = "stc-package")
        val result = PackageConversion(
          conversion   = c.conversion,
          libName      = c.library.get,
          folder       = c.packageDir.get,
          dependencies = IArray.fromTraversable(c.dependencies),
          sources      = sources,
          interface    = c.interface.get,
          parallelism  = Runtime.getRuntime.availableProcessors,
          logger       = stdout.filter(LogLevel.warn),
        )
        result match {
          case Left(error) =>
            System.err.println(s"Couldn't convert ${c.library.get.value}: $error")
            System.exit(1)
          case Right(_) =>
            writeSrcjar(sources, c.srcjar.get)
        }
    }

  /** Every file under `sources`, in path order, or the placeholder when there are none. */
  private def writeSrcjar(sources: os.Path, srcjar: os.Path): Unit = {
    val written = os.walk(sources).filter(os.isFile).map(_.relativeTo(sources)).sortBy(_.toString)
    val entries: Seq[(String, Array[Byte])] =
      if (written.isEmpty) Seq("typings/NoDeclarations.scala" -> NoDeclarations.getBytes("UTF-8"))
      else written.map(path => path.toString -> os.read.bytes(sources / path))

    os.makeDir.all(srcjar / os.up)
    val out = new ZipOutputStream(os.write.over.outputStream(srcjar))
    try entries.foreach {
      case (name, bytes) =>
        val entry = new ZipEntry(name)
        entry.setLastModifiedTime(EntryTime)
        out.putNextEntry(entry)
        out.write(bytes)
        out.closeEntry()
    } finally out.close()
  }
}
