package org.scalablytyped.converter.internal
package importer

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.serializers.ClosureSerializer
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy
import com.esotericsoftware.kryo.{Kryo, Serializer => KryoSerializer}
import org.objenesis.strategy.StdInstantiatorStrategy
import org.scalablytyped.converter.internal.scalajs.{Name, PackageTree, QualifiedName}
import org.scalablytyped.converter.internal.ts.{TsIdent, TsIdentLibrary, TsParsedFile}

import java.lang.reflect.Modifier
import scala.collection.immutable.SortedMap
import scala.collection.mutable

/**
  * What converting one library leaves for the libraries written against it: that library's own trees after each
  * phase, and the libraries it is written against by name only.
  *
  * A build converts each library in a step of its own, given the interfaces of every library it reaches, so a
  * library is converted once however many libraries depend on it. Its dependencies are named rather than carried,
  * so an interface holds one library's code and nothing else, and its bytes depend on that library alone.
  */
sealed trait PackageInterface extends Serializable {
  def libName: TsIdentLibrary
}

object PackageInterface {

  /** A package with no TypeScript declarations: nothing is written against it. */
  final case class Absent(libName: TsIdentLibrary) extends PackageInterface

  final case class Converted(
      libName:            TsIdentLibrary,
      isStdLib:           Boolean,
      tsVersion:          LibraryVersion,
      parsed:             TsParsedFile,
      tsDependencies:     IArray[TsIdentLibrary],
      scalaLibName:       String,
      scalaName:          Name,
      scalaVersion:       LibraryVersion,
      scalaTree:          PackageTree,
      scalaDependencies:  IArray[TsIdentLibrary],
      names:              Map[IArray[TsIdent], QualifiedName],
      flavourTree:        PackageTree,
      flavourDependencies: IArray[TsIdentLibrary],
  ) extends PackageInterface

  def write(path: os.Path, interface: PackageInterface): Unit = {
    os.makeDir.all(path / os.up)
    val output = new Output(os.write.over.outputStream(path), BufferSize)
    try new InterfaceKryo().writeClassAndObject(output, interface)
    finally output.close()
  }

  def read(path: os.Path): PackageInterface = {
    val input = new Input(os.read.inputStream(path), BufferSize)
    try
      new InterfaceKryo().readClassAndObject(input) match {
        case interface: PackageInterface => interface
        case other                       => sys.error(s"$path holds a ${other.getClass.getName}, not a package interface")
      }
    finally input.close()
  }

  private val BufferSize = 1 << 16

  /**
    * Kryo set up for the converter's trees, which Java serialization used to write.
    *
    * Kryo copies an object's fields where Java serialization honours what a class says about itself, so what that
    * covered is said here: a Scala `object` reads back as itself rather than as a second instance, since pattern
    * matches compare case objects by reference, and a lambda (an `Ordering.by` under a sorted collection) as one.
    * Shared subtrees stay shared. Classes are written by name, in the order the tree reaches them, so equal
    * interfaces are equal bytes.
    */
  private final class InterfaceKryo extends Kryo {
    setRegistrationRequired(false)
    setReferences(true)
    setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy))
    register(classOf[ClosureSerializer.Closure], new ClosureSerializer)

    override def getDefaultSerializer(tpe: Class[?]): KryoSerializer[?] =
      moduleOf(tpe) match {
        case Some(module) => new ModuleSerializer(module)
        case None         => super.getDefaultSerializer(tpe)
      }
  }

  /* The instance of a Scala `object`, which its class holds in a static `MODULE$` */
  private def moduleOf(tpe: Class[?]): Option[AnyRef] =
    tpe.getDeclaredFields.find(f => f.getName == "MODULE$" && Modifier.isStatic(f.getModifiers)).map { field =>
      field.setAccessible(true)
      field.get(null)
    }

  private final class ModuleSerializer(module: AnyRef) extends KryoSerializer[AnyRef] {
    override def write(kryo: Kryo, output: Output, obj: AnyRef): Unit = ()
    override def read(kryo: Kryo, input: Input, tpe: Class[? <: AnyRef]): AnyRef = module
  }

  /** Where a library is known to be by the libraries converted against its interface: nowhere they read. */
  def placeholderSource(libName: TsIdentLibrary): LibTsSource =
    if (libName === TsIdent.std)
      LibTsSource.StdLibSource(InFolder(placeholderFolder(libName)), Empty, libName)
    else LibTsSource.FromFolder(InFolder(placeholderFolder(libName)), libName)

  private def placeholderFolder(libName: TsIdentLibrary): os.Path =
    os.root / "scalablytyped-interface" / libName.`__value`

  /**
    * The converted libraries, each phase's result linked to its dependencies' as the full pipeline links them.
    *
    * @param sources the source each library is known by, the same instance wherever it is looked up
    */
  final class Linked(interfaces: IArray[Converted], val sources: Map[TsIdentLibrary, LibTsSource]) {
    private val byName: Map[TsIdentLibrary, Converted] =
      interfaces.map(i => i.libName -> i).toMap

    private val tsCache      = mutable.Map.empty[TsIdentLibrary, LibTs]
    private val scalaCache   = mutable.Map.empty[TsIdentLibrary, LibScalaJs]
    private val flavourCache = mutable.Map.empty[TsIdentLibrary, LibScalaJs]
    private val namingCache  = mutable.Map.empty[TsIdentLibrary, AdaptiveNamingImport]

    private def present(names: IArray[TsIdentLibrary]): IArray[TsIdentLibrary] =
      names.filter(byName.contains)

    /* Each library's own names, linked to the namings of the libraries it reaches. */
    private def naming(name: TsIdentLibrary): AdaptiveNamingImport =
      namingCache.getOrElseUpdate(
        name, {
          val i = byName(name)
          new AdaptiveNamingImport(
            name,
            i.names,
            AdaptiveNamingImport.reached(present(i.scalaDependencies).map(naming)),
          )
        },
      )

    def ts(name: TsIdentLibrary): LibTs =
      tsCache.getOrElseUpdate(
        name, {
          val i = byName(name)
          LibTs(sources(name))(
            version      = i.tsVersion,
            parsed       = i.parsed,
            dependencies = SortedMap.from(present(i.tsDependencies).map(d => sources(d) -> ts(d)).toList),
          )
        },
      )

    def scala(name: TsIdentLibrary): LibScalaJs =
      scalaCache.getOrElseUpdate(
        name, {
          val i = byName(name)
          LibScalaJs(sources(name))(
            libName      = i.scalaLibName,
            scalaName    = i.scalaName,
            libVersion   = i.scalaVersion,
            packageTree  = i.scalaTree,
            dependencies = present(i.scalaDependencies).map(d => sources(d) -> scala(d)).toList.toMap,
            isStdLib     = i.isStdLib,
            names        = naming(name),
          )
        },
      )

    def flavour(name: TsIdentLibrary): LibScalaJs =
      flavourCache.getOrElseUpdate(
        name, {
          val i = byName(name)
          LibScalaJs(sources(name))(
            libName      = i.scalaLibName,
            scalaName    = i.scalaName,
            libVersion   = i.scalaVersion,
            packageTree  = i.flavourTree,
            dependencies = present(i.flavourDependencies).map(d => sources(d) -> flavour(d)).toList.toMap,
            isStdLib     = i.isStdLib,
            names        = naming(name),
          )
        },
      )

    def contains(source: LibTsSource): Boolean =
      byName.contains(source.libName) && sources(source.libName) == source
  }
}
