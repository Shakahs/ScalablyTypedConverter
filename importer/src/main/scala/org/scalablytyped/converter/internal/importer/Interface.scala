package org.scalablytyped.converter.internal
package importer

import org.scalablytyped.converter.internal.phases.PhaseListener
import org.scalablytyped.converter.internal.ts.TsIdentLibrary
import fansi.Color

import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable
import scala.util.Try

object Interface {
  def apply[T](debugMode: Boolean)(f: PhaseListener[LibTsSource] => T) = {
    val i = new Interface(debugMode)
    i.start()
    try {
      f(i)
    } finally {
      i.finish()
    }
  }

  private class Interface(debugMode: Boolean) extends Thread with PhaseListener[LibTsSource] {

    import PhaseListener._
    private val t0     = System.currentTimeMillis
    private val status = mutable.Map.empty[TsIdentLibrary, Event[LibTsSource]]
    private var failed = List.empty[(TsIdentLibrary, Failure[LibTsSource])]

    private val hasExited = new AtomicBoolean(false)

    /* `status` and `failed` are written by the phases, so read them under the same lock */
    private def snapshot(): (Map[TsIdentLibrary, Event[LibTsSource]], List[(TsIdentLibrary, Failure[LibTsSource])]) =
      synchronized((status.toMap, failed))

    def finish(): Unit = {
      val (status, _) = snapshot()
      require(!status.values.exists { case _: Blocked[LibTsSource] => true; case _ => false })
      require(!status.values.exists { case _: Started[LibTsSource] => true; case _ => false })
      hasExited.set(true)
    }

    override def on(phaseName: String, id: LibTsSource, event: PhaseListener.Event[LibTsSource]): Unit =
      synchronized {
        event match {
          case fail @ Failure(_, _) =>
            failed = (id.libName -> fail) :: failed.filterNot(_._1 === id.libName)
          case _ => ()
        }

        status(id.libName) = event
        ()
      }

    override def run(): Unit =
      if (!debugMode) {
        while (!hasExited.get) {
          render()
          Try(Thread.sleep(1000))
        }
      }

    def render(): Unit = {
      val (status, failed) = snapshot()
      val ignored          = status.collect { case (lib, _: Ignored[LibTsSource]) => lib }
      val active           = status.collect { case (lib, x: Started[LibTsSource]) => (lib, x) }
      val blocked          = status.collect { case (lib, x: Blocked[LibTsSource]) => (lib, x) }
      val succeeded        = status.collect { case (lib, x: Success[LibTsSource]) => lib -> x }

      val sb = new StringBuffer()
      def println(s: String) = sb.append(s).append("\n")

      val numSucceded        = succeeded.size
      val numFailed          = failed.size
      val numIgnored         = ignored.size
      val td                 = (System.currentTimeMillis() - t0) / 1000.0
      val numProcessed       = numFailed + numSucceded
      val processedPerSecond = td / numProcessed
      //reset terminal
      println("\u001b[2J")
      //move cursor to start of screen
      println("\u001b[H")

      def row[N: Numeric](title: String, num: N) =
        println(fansi.Str.join(List(title, ": ", fansi.Bold.On(num.toString))).render)

      row("Successes", numSucceded)
      row("Failed", numFailed)
      row("Ignored", numIgnored)
      row("Seconds elapsed", td)
      row("Seconds per library", processedPerSecond)

      println("Active:")
      active.toVector.map(x => (Color.Green(x._1.value).render, x._2.phase)).sorted.foreach(x => println(x.toString))

      println("Blocked:")
      blocked.toVector
        .map {
          case (name, Blocked(phase, on)) =>
            s"${Color.LightGray(name.value).render} ($phase) blocked on ${(on.map(_.libName) -- succeeded.keys).map(_.value)}"
        }
        .sorted
        .foreach(println)

      val n = math.min(10, failed.size)
      println(s"Last $n errors: ")

      failed.take(n).foreach {
        case (lib, failure) =>
          val errorStrings = failure.errors
            .map {
              case (lib: LibTsSource, Left(th)) =>
                s"dependency ${lib.libName.value} failed ${th.getClass.getSimpleName}: ${th.getMessage}"
              case (lib: LibTsSource, Right(str)) =>
                s"dependency ${lib.libName.value} failed: $str"
            }

          val errorMsg = errorStrings
            .map(_.takeWhile(_ =/= '\n'))
            .toVector
            .distinct
            .mkString(", ")

          println(s"${Color.Red(lib.value)}: $errorMsg")
      }

      System.out.println(sb)
    }
  }
}
