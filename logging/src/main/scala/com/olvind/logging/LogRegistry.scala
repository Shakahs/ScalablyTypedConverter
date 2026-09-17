package com.olvind.logging

import java.util.concurrent.ConcurrentHashMap

import scala.jdk.CollectionConverters._

class LogRegistry[K, KK, U](outer: Logger[Unit], grouper: K => KK, subLogger: KK => Logger[U]) {
  private val loggers = new ConcurrentHashMap[KK, Logger[U]]()

  def logs: Map[KK, Logger[U]] =
    loggers.asScala.toMap

  def get(key: K): Logger[Unit] =
    outer.zipWith(loggers.computeIfAbsent(grouper(key), kk => subLogger(kk))).void
}
