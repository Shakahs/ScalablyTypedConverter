package org.scalablytyped.converter.cli

import java.io.PrintStream
import java.util.concurrent.atomic.AtomicReference

import org.scalablytyped.converter.internal.importer.LibTsSource
import org.scalablytyped.converter.internal.phases.PhaseListener
import org.scalablytyped.converter.internal.ts.TsIdentLibrary
import ox.{forever, fork, sleep, supervised}

import scala.concurrent.duration._

/**
  * Counts the packages which have passed through `trackedPhase` (the last one) and those still on their way.
  *
  * Packages are discovered as they are requested, so the total grows while the conversion runs.
  */
final class Progress(trackedPhase: String) extends PhaseListener[LibTsSource] {
  import Progress.State

  private val state       = new AtomicReference(State.Empty)
  private val lastPrinted = new AtomicReference("")

  override def on(phaseName: String, id: LibTsSource, event: PhaseListener.Event[LibTsSource]): Unit =
    if (phaseName == trackedPhase) {
      val _ = state.updateAndGet(_.on(id.libName, event))
    }

  def current: State = state.get

  /* prints the progress unless it is what was printed last */
  def printIfChanged(out: PrintStream): Unit = {
    val line = current.render
    if (lastPrinted.getAndSet(line) != line) out.println(line)
  }
}

object Progress {
  val BarWidth = 30

  /**
    * @param open how many computations of each package are running or waiting for dependencies
    * @param blocked packages whose computation waits for dependencies
    * @param finished packages with at least one finished computation
    */
  final case class State(
      open:     Map[TsIdentLibrary, Int],
      blocked:  Set[TsIdentLibrary],
      finished: Set[TsIdentLibrary],
      failed:   Set[TsIdentLibrary],
  ) {
    def on(lib: TsIdentLibrary, event: PhaseListener.Event[LibTsSource]): State =
      event match {
        case PhaseListener.Started(_) =>
          copy(open = open.updated(lib, open.getOrElse(lib, 0) + 1))
        case PhaseListener.Success(_) =>
          done(lib)
        case PhaseListener.Ignored() =>
          done(lib)
        case PhaseListener.Failure(_, _) =>
          done(lib).copy(failed = failed + lib)
        case PhaseListener.Blocked(_, _) =>
          copy(blocked = blocked + lib)
        case PhaseListener.Resumed(_) =>
          copy(blocked = blocked - lib)
      }

    private def done(lib: TsIdentLibrary): State =
      open.getOrElse(lib, 0) match {
        case n if n <= 1 => State(open - lib, blocked - lib, finished + lib, failed)
        case n           => State(open.updated(lib, n - 1), blocked - lib, finished + lib, failed)
      }

    def pending:   Int = open.size
    def processed: Int = (finished -- open.keySet).size
    def total:     Int = pending + processed

    /* packages whose own phase is running right now */
    def working: List[String] = (open.keySet -- blocked).map(_.value).toList.sorted
    /* packages waiting for their dependencies */
    def waiting: List[String] = blocked.filter(open.contains).map(_.value).toList.sorted

    def render: String = {
      val filled = if (total == 0) 0 else processed * BarWidth / total
      val bar    = "#" * filled + "-" * (BarWidth - filled)
      val failures =
        if (failed.isEmpty) "" else s", ${failed.size} failed"
      val current = List("working" -> working, "waiting" -> waiting).collect {
        case (label, libs) if libs.nonEmpty => s" | $label: ${libs.mkString(", ")}"
      }
      s"[$bar] $processed/$total packages processed, $pending pending$failures${current.mkString}"
    }
  }

  object State {
    val Empty: State = State(Map.empty, Set.empty, Set.empty, Set.empty)
  }

  /**
    * Runs `f` with a `Progress` listener, and prints the progress every `interval` while it changes
    */
  def showing[T](trackedPhase: String, out: PrintStream, interval: FiniteDuration = 1.second)(
      f:                       Progress => T,
  ): T = {
    val progress = new Progress(trackedPhase)
    supervised {
      val _ = fork {
        forever {
          sleep(interval)
          progress.printIfChanged(out)
        }
      }
      val ret = f(progress)
      progress.printIfChanged(out)
      ret
    }
  }
}
