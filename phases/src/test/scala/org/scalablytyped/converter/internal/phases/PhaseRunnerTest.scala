package org.scalablytyped.converter.internal
package phases

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ConcurrentHashMap, CyclicBarrier, TimeUnit}

import com.olvind.logging.Logger
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.immutable.SortedSet
import scala.concurrent.duration._

class PhaseRunnerTest extends AnyFunSuite {

  /* A phase where every id depends on the ids in `graph`. The result shows which dependencies were resolved */
  final class Fixture(graph: Map[String, List[String]], beforeDeps: String => Unit = _ => ()) {
    val computations    = new ConcurrentHashMap[String, AtomicInteger]()
    private val running = new AtomicInteger(0)
    val maxRunning      = new AtomicInteger(0)

    val phase: RecPhase[String, String] =
      RecPhase[String].next(
        (id: String, _: String, getDeps: GetDeps[String, String], isCircular: IsCircular, _: Logger[Unit]) =>
          if (isCircular) PhaseRes.Ignore()
          else {
            computations.computeIfAbsent(id, _ => new AtomicInteger(0)).incrementAndGet()
            val now = running.incrementAndGet()
            maxRunning.accumulateAndGet(now, math.max)
            Thread.sleep(5)
            running.decrementAndGet()
            beforeDeps(id)
            getDeps(SortedSet.empty[String] ++ graph.getOrElse(id, Nil)).flatMap { deps =>
              if (id.startsWith("fail")) PhaseRes.Failure(Map(id -> Right("failed")))
              else PhaseRes.Ok(s"$id(${deps.values.mkString(",")})")
            }
          },
        "test",
      )

    def run(parallelism: Int, roots: String*): Map[String, PhaseRes[String, String]] =
      ox.timeout(20.seconds) {
        PhaseRunner
          .all(phase, (_: String) => Logger.DevNull, PhaseListener.NoListener[String], parallelism)(roots.toVector)
          .toMap
      }
  }

  def barrier(parties: Int, ids: Set[String]): String => Unit = {
    val b = new CyclicBarrier(parties)
    id =>
      if (ids(id)) {
        b.await(10, TimeUnit.SECONDS)
        ()
      }
  }

  val dag = Map(
    "app1" -> List("lib", "std"),
    "app2" -> List("lib", "util"),
    "app3" -> List("util"),
    "lib" -> List("util", "std"),
    "util" -> List("std"),
  )
  val dagRoots = List("app1", "app2", "app3", "lib", "util", "std")

  test("parallel run gives the same results as a serial run") {
    val serial   = new Fixture(dag).run(1, dagRoots *)
    val parallel = new Fixture(dag).run(4, dagRoots *)
    assert(parallel === serial)
    assert(serial("app2") === PhaseRes.Ok("app2(lib(std(),util(std())),util(std()))"))
  }

  test("serial run matches running each root with PhaseRunner.apply") {
    val fixture = new Fixture(dag)
    val one     = new Fixture(dag)
    val applied = dagRoots.map(id =>
      id -> PhaseRunner(one.phase, (_: String) => Logger.DevNull, PhaseListener.NoListener[String])(id),
    )
    assert(fixture.run(1, dagRoots *) === applied.toMap)
  }

  test("shared dependencies are computed once") {
    val fixture = new Fixture(dag)
    fixture.run(4, dagRoots *)
    dagRoots.foreach(id => assert(fixture.computations.get(id).get === 1, id))
  }

  test("no more than `parallelism` phases work at the same time") {
    val wide    = (1 to 30).map(i => s"root$i" -> List("leaf1", "leaf2", s"leaf$i")).toMap
    val fixture = new Fixture(wide)
    val results = fixture.run(3, wide.keys.toSeq *)
    assert(results.values.forall { case PhaseRes.Ok(_) => true; case _ => false })
    assert(fixture.maxRunning.get <= 3)
    assert(fixture.maxRunning.get > 1)
  }

  test("two roots which depend on each other do not deadlock") {
    val graph   = Map("a" -> List("b"), "b" -> List("a"))
    val fixture = new Fixture(graph, barrier(2, Set("a", "b")))
    val results = fixture.run(2, "a", "b")

    // whichever library asked last sees the other one as circular, like a serial run would
    val expected = Set[Map[String, PhaseRes[String, String]]](
      Map("a" -> PhaseRes.Ok("a(b())"), "b" -> PhaseRes.Ok("b()")),
      Map("a" -> PhaseRes.Ok("a()"), "b" -> PhaseRes.Ok("b(a())")),
    )
    assert(expected.contains(results), results)
  }

  test("a longer cycle entered from every member at once does not deadlock") {
    val graph   = Map("a" -> List("b"), "b" -> List("c"), "c" -> List("a"))
    val fixture = new Fixture(graph, barrier(3, Set("a", "b", "c")))
    val results = fixture.run(3, "a", "b", "c")
    assert(results.values.forall { case PhaseRes.Ok(_) => true; case _ => false }, results)
    // the cycle is broken somewhere
    assert(results.exists { case (id, res) => res === PhaseRes.Ok(s"$id()") }, results)
  }

  test("a cycle gives serial results when parallelism is 1") {
    val graph = Map("a" -> List("b"), "b" -> List("a"))
    assert(
      new Fixture(graph).run(1, "a", "b") === Map("a" -> PhaseRes.Ok("a(b())"), "b" -> PhaseRes.Ok("b()")),
    )
  }

  test("failures propagate to dependents") {
    val graph   = Map("app" -> List("fail-lib"), "other" -> List("std"))
    val results = new Fixture(graph).run(4, "app", "other")
    assert(results("app") === PhaseRes.Failure(Map("fail-lib" -> Right("failed"))))
    assert(results("other") === PhaseRes.Ok("other(std())"))
  }
}
