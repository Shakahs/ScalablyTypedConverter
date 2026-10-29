package org.scalablytyped.converter.internal
package importer

import io.circe.Json
import org.scalablytyped.converter.internal.ts.TsIdentModule

/** Keep declaration module names stable while honoring the package's runtime export paths. */
object RuntimeModuleResolver {
  def apply(sources: Iterable[LibTsSource]): TsIdentModule => String = {
    val libraries = sources.map(source => source.libName -> source).toMap
    val resolved = scala.collection.mutable.Map.empty[TsIdentModule, String]
    module => resolved.getOrElseUpdate(module, libraries.get(module.inLibrary).fold(module.value)(resolveModule(_, module)))
  }

  private def resolveModule(source: LibTsSource, module: TsIdentModule): String = {
    val packageName = source.libName.value
    val subpath = if (module.value == packageName) "." else "./" + module.value.stripPrefix(packageName + "/")

    def selectRuntimeTarget(value: Json): Option[String] =
      value.asString.orElse(value.asObject.flatMap { conditions =>
        conditions("import").flatMap(selectRuntimeTarget).orElse(conditions("default").flatMap(selectRuntimeTarget))
      }).orElse(value.asArray.flatMap(_.iterator.flatMap(selectRuntimeTarget).take(1).toList.headOption))

    def resolveExport(exports: Json, exportedPath: String): Option[String] =
      exports.asObject match {
        case Some(entries) if entries.keys.exists(_.startsWith(".")) =>
          entries(exportedPath).flatMap(selectRuntimeTarget).orElse {
            entries.toList.filter(_._1.contains("*")).sortBy { case (pattern, _) =>
              (-pattern.takeWhile(_ != '*').length, -pattern.length)
            }.iterator.flatMap { case (pattern, target) =>
              val prefix = pattern.takeWhile(_ != '*')
              val suffix = pattern.drop(prefix.length + 1)
              if (exportedPath.startsWith(prefix) && exportedPath.endsWith(suffix) && exportedPath.length >= prefix.length + suffix.length)
                selectRuntimeTarget(target).map(_.replace("*", exportedPath.substring(prefix.length, exportedPath.length - suffix.length)))
              else None
            }.take(1).toList.headOption
          }
        case _ if exportedPath == "." => selectRuntimeTarget(exports)
        case _ => None
      }

    source.packageJsonOpt.flatMap(_.exports) match {
      case Some(exports) =>
        def hasRuntimeFile(exportedPath: String): Boolean =
          resolveExport(exports, exportedPath).exists { target =>
            target.startsWith("./") && os.isFile(source.folder.path / os.RelPath(target))
          }

        if (hasRuntimeFile(subpath)) module.value
        else {
          List(".js", ".mjs", ".cjs", "/index.js", "/index.mjs", "/index.cjs")
            .find(extension => hasRuntimeFile(subpath + extension))
            .fold(module.value)(extension => module.value + extension)
        }
      case None => module.value
    }
  }
}
