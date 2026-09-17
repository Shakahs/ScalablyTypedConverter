package org.scalablytyped.converter.cli

import java.io.{ByteArrayOutputStream, PrintStream}

import org.scalablytyped.converter.internal.InFolder
import org.scalablytyped.converter.internal.importer.LibTsSource
import org.scalablytyped.converter.internal.phases.PhaseListener._
import org.scalablytyped.converter.internal.ts.TsIdentLibrary
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._

class ProgressTest extends AnyFunSuite {
  def lib(name: String): LibTsSource =
    LibTsSource.FromFolder(InFolder(os.root / "tmp" / name), TsIdentLibrary(name))

  val a = lib("a")
  val b = lib("b")
  val c = lib("c")

  test("counts packages which are done and those on their way") {
    val p = new Progress("build")
    p.on("build", a, Started("build"))
    p.on("build", a, Blocked("build", Set(b, c)))
    p.on("build", b, Started("build"))
    p.on("build", c, Started("build"))
    p.on("build", b, Success("build"))
    p.on("typescript", c, Failure("typescript", Map.empty))

    assert(p.current.processed === 1)
    assert(p.current.pending === 2)
    assert(
      p.current.render === s"[${"#" * 10}${"-" * 20}] 1/3 packages processed, 2 pending | working: c | waiting: a",
    )

    p.on("build", c, Failure("build", Map(c -> Right("oops"))))
    p.on("build", a, Resumed("build"))
    p.on("build", a, Ignored())

    assert(p.current.render === s"[${"#" * 30}] 3/3 packages processed, 0 pending, 1 failed")
  }

  test("a package is pending while any of its computations runs") {
    val p = new Progress("build")
    p.on("build", a, Started("build"))
    p.on("build", a, Started("build")) // computed again as circular
    p.on("build", a, Ignored())
    assert((p.current.processed, p.current.pending) === ((0, 1)))
    p.on("build", a, Success("build"))
    assert((p.current.processed, p.current.pending) === ((1, 0)))
  }

  test("prints while running and once at the end") {
    val bytes = new ByteArrayOutputStream()
    val out   = new PrintStream(bytes, true)
    val ret = Progress.showing("build", out, interval = 20.millis) { p =>
      p.on("build", a, Started("build"))
      Thread.sleep(200)
      p.on("build", a, Success("build"))
      42
    }
    assert(ret === 42)
    assert(
      bytes.toString.linesIterator.toList === List(
        s"[${"-" * 30}] 0/1 packages processed, 1 pending | working: a",
        s"[${"#" * 30}] 1/1 packages processed, 0 pending",
      ),
    )
  }
}
