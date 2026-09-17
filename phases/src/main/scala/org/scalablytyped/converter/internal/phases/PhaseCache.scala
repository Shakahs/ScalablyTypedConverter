package org.scalablytyped.converter.internal.phases

import java.util
import java.util.concurrent.{CompletableFuture, ExecutionException}

import org.scalablytyped.converter.internal.StableHash
import org.scalablytyped.converter.internal.phases.PhaseCache.Ref

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._

/**
  * Computes each key at most once, also when several threads ask for it at the same time.
  *
  * A thread asking for a key which is being computed elsewhere waits for that computation. Every such request is
  * recorded as an edge from the requesting key to the requested key, so a wait which would close a cycle
  * (the requested key is itself, transitively, waiting for the requester) is refused instead of deadlocking.
  */
final class PhaseCache[Id, U](initialCapacity: Int = 1000) {
  private type Key = (Id, IsCircular)

  // all mutable state below is guarded by `lock`
  private val lock = new Object
  private val completed: util.Map[Ref[Key], Ref[PhaseRes[Id, U]]] =
    new util.HashMap(initialCapacity)
  private val inFlight: util.Map[Key, CompletableFuture[PhaseRes[Id, U]]] =
    new util.HashMap()
  private val requestedBy: util.Map[Key, util.Set[Key]] =
    new util.HashMap()

  /**
    * @param requester the key whose computation asks for `key`, if any
    * @return `None` if `key` is being computed and waiting for it would deadlock
    */
  def getOrCompute(key: Key, requester: Option[Key])(compute: () => PhaseRes[Id, U]): Option[PhaseRes[Id, U]] = {
    val keyRef = new Ref(key)
    val _      = keyRef.hashCode // compute the hash outside the lock

    val action: PhaseCache.Action[PhaseRes[Id, U]] =
      lock.synchronized {
        Option(completed.get(keyRef)).flatMap(ref => Option(ref.get)) match {
          case Some(found) => PhaseCache.Found(found)
          case None =>
            inFlight.get(key) match {
              case null =>
                val promise = new CompletableFuture[PhaseRes[Id, U]]
                inFlight.put(key, promise)
                requester.foreach(addEdge(_, key))
                PhaseCache.Compute(promise)
              case _ if requester.exists(r => reaches(List(key), r, Set.empty)) =>
                PhaseCache.WouldDeadlock()
              case promise =>
                requester.foreach(addEdge(_, key))
                PhaseCache.Await(promise)
            }
        }
      }

    action match {
      case PhaseCache.Found(found)    => Some(found)
      case PhaseCache.WouldDeadlock() => None
      case PhaseCache.Await(promise) =>
        try Some(PhaseCache.await(promise))
        finally lock.synchronized(requester.foreach(removeEdge(_, key)))
      case PhaseCache.Compute(promise) =>
        val result =
          try compute()
          catch {
            case th: Throwable =>
              lock.synchronized {
                inFlight.remove(key)
                requester.foreach(removeEdge(_, key))
              }
              promise.completeExceptionally(th)
              throw th
          }
        lock.synchronized {
          completed.put(keyRef, new Ref(result))
          inFlight.remove(key)
          requester.foreach(removeEdge(_, key))
        }
        promise.complete(result)
        Some(result)
    }
  }

  private def addEdge(from: Key, to: Key): Unit = {
    requestedBy.computeIfAbsent(from, _ => new util.HashSet[Key]()).add(to)
    ()
  }

  private def removeEdge(from: Key, to: Key): Unit =
    requestedBy.get(from) match {
      case null => ()
      case tos =>
        tos.remove(to)
        if (tos.isEmpty) {
          val _ = requestedBy.remove(from)
        }
        ()
    }

  /* whether `target` can be reached from any key in `todo` by following requests */
  @tailrec
  private def reaches(todo: List[Key], target: Key, seen: Set[Key]): Boolean =
    todo match {
      case Nil                                 => false
      case head :: _ if head == target         => true
      case head :: tail if seen.contains(head) => reaches(tail, target, seen)
      case head :: tail =>
        val next = requestedBy.get(head) match {
          case null => tail
          case tos  => tos.asScala.foldLeft(tail)((acc, to) => to :: acc)
        }
        reaches(next, target, seen + head)
    }
}

object PhaseCache {
  private sealed trait Action[T]
  private final case class Found[T](value:     T) extends Action[T]
  private final case class Compute[T](promise: CompletableFuture[T]) extends Action[T]
  private final case class Await[T](promise:   CompletableFuture[T]) extends Action[T]
  private final case class WouldDeadlock[T]() extends Action[T]

  private def await[T](promise: CompletableFuture[T]): T =
    try promise.get()
    catch {
      case e: ExecutionException if e.getCause != null => throw e.getCause
    }

  private final class Ref[T](t: T) extends java.lang.ref.SoftReference[T](t) {
    override def equals(obj: Any): Boolean =
      obj match {
        case that: Ref[_] => that.get == get
        case _ => false
      }

    override lazy val hashCode: Int = {
      val value = get
      if (value == null) 0 else StableHash(value)
    }
  }
}
