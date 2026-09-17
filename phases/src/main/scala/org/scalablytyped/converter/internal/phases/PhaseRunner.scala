package org.scalablytyped.converter.internal
package phases

import java.util.concurrent.Semaphore

import com.olvind.logging.{Formatter, Logger}
import ox.mapPar

import scala.collection.immutable.{SortedMap, SortedSet}
import scala.util.control.NonFatal

/**
  * Runs a computation given a sequence of input ids.
  */
object PhaseRunner {

  /**
    * Bounds how many phase computations do work at the same time.
    *
    * Requested dependencies are computed in parallel forks. A computation holds a permit only while it runs its own
    * phase, and gives it up while it waits for dependencies, so every permit belongs to a computation which can make
    * progress.
    */
  final class Workers private (val parallelism: Int) {
    private val permits = new Semaphore(parallelism)

    val isSerial: Boolean = parallelism == 1

    def traverse[A, B](as: Vector[A])(f: A => B): Vector[B] =
      if (isSerial || as.sizeIs < 2) as.map(f) else as.mapPar(as.size)(f)

    def working[T](f: => T): T =
      if (isSerial) f
      else {
        permits.acquire()
        try f
        finally permits.release()
      }

    def waiting[T](f: => T): T =
      if (isSerial) f
      else {
        permits.release()
        try f
        finally permits.acquireUninterruptibly()
      }
  }

  object Workers {
    val Serial: Workers = new Workers(1)

    def apply(parallelism: Int): Workers = {
      require(parallelism >= 1, s"parallelism must be at least 1, was $parallelism")
      if (parallelism == 1) Serial else new Workers(parallelism)
    }
  }

  def apply[Id: Formatter: Ordering, T](
      phase:     RecPhase[Id, T],
      getLogger: Id => Logger[Unit],
      listener:  PhaseListener[Id],
  )(initial:     phase._Id): PhaseRes[phase._Id, phase._T] =
    go(phase, initial, Nil, None, getLogger, listener, Workers.Serial)

  /**
    * Runs `phase` for all of `initial`, up to `parallelism` computations at a time.
    * With `parallelism` 1 this is the same as running each id in order.
    */
  def all[Id: Formatter: Ordering, T](
      phase:       RecPhase[Id, T],
      getLogger:   Id => Logger[Unit],
      listener:    PhaseListener[Id],
      parallelism: Int,
  )(initial:       Vector[Id]): Vector[(Id, PhaseRes[Id, T])] = {
    val workers = Workers(parallelism)
    workers.traverse(initial)(id => id -> go(phase, id, Nil, None, getLogger, listener, workers))
  }

  def go[Id: Formatter: Ordering, TT](
      phase:          RecPhase[Id, TT],
      id:             Id,
      circuitBreaker: List[Id],
      requester:      Option[(Id, IsCircular)],
      getLogger:      Id => Logger[Unit],
      listener:       PhaseListener[Id],
      workers:        Workers,
  ): PhaseRes[Id, TT] =
    phase match {
      case _:    RecPhase.Initial[Id] => PhaseRes.Ok[Id, TT](id)
      case next: RecPhase.Next[Id, t, TT] =>
        doNext[Id, t, TT](next, id, circuitBreaker, requester, getLogger, listener, workers)
    }

  def doNext[Id: Formatter: Ordering, T, TT](
      next:           RecPhase.Next[Id, T, TT],
      id:             Id,
      circuitBreaker: List[Id],
      requester:      Option[(Id, IsCircular)],
      getLogger:      Id => Logger[Unit],
      listener:       PhaseListener[Id],
      workers:        Workers,
  ): PhaseRes[Id, TT] = {

    def compute(isCircular: IsCircular): PhaseRes[Id, TT] = {
      val logger = getLogger(id)
        .withContext(id)
        .withContext("thread", Thread.currentThread().threadId())
        .withContext("phase", next.name)

      try {
        listener.on(next.name, id, PhaseListener.Started(next.name))

        val resLastPhase: PhaseRes[Id, T] =
          go(next.prev, id, Nil, None, getLogger, listener, workers)

        def calculateDeps(newRequestedIds: SortedSet[Id]): PhaseRes[Id, SortedMap[Id, TT]] = {
          listener.on(next.name, id, PhaseListener.Blocked(next.name, newRequestedIds))

          val ret: PhaseRes[Id, SortedMap[Id, TT]] =
            workers.waiting {
              PhaseRes.sequenceMap(
                SortedMap.empty[Id, PhaseRes[Id, TT]] ++
                  workers.traverse(newRequestedIds.toVector) { thisId =>
                    thisId -> go(
                      next,
                      thisId,
                      id :: circuitBreaker,
                      Some((id, isCircular)),
                      getLogger,
                      listener,
                      workers,
                    )
                  },
              )
            }

          listener.on(next.name, id, PhaseListener.Started(next.name))
          ret
        }

        val result: PhaseRes[Id, TT] =
          resLastPhase.flatMap(lastValue =>
            workers.working(
              PhaseRes.attempt(id, logger, next.trans(id, lastValue, calculateDeps, isCircular, logger)),
            ),
          )

        result match {
          case PhaseRes.Ok(_) =>
            listener.on(next.name, id, PhaseListener.Success(next.name))
          case PhaseRes.Failure(errors) =>
            listener.on(next.name, id, PhaseListener.Failure(next.name, errors))
          case PhaseRes.Ignore() =>
            listener.on(next.name, id, PhaseListener.Ignored())
        }
        result

      } catch {
        case NonFatal(e) =>
          val errors = Map[Id, Either[Throwable, String]](id -> Left(e))
          listener.on(next.name, id, PhaseListener.Failure(next.name, errors))
          logger.error(("Failure", e))
          PhaseRes.Failure(errors)
      }
    }

    val isCircular = circuitBreaker contains id

    next.cache.getOrCompute((id, isCircular), requester)(() => compute(isCircular)) match {
      case Some(result) => result
      /* `id` is being computed concurrently, and that computation (transitively) waits for `requester`.
       * A serial traversal would have found `id` in the circuit breaker, so treat it as circular */
      case None =>
        next.cache
          .getOrCompute((id, true), requester)(() => compute(isCircular = true))
          .getOrElse(compute(isCircular = true))
    }
  }
}
