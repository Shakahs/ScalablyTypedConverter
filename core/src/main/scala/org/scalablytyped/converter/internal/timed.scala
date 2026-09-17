package org.scalablytyped.converter.internal

object timed {

  /** @return the result of `f` and the milliseconds it took */
  def apply[T](f: => T): (T, Long) = {
    val t0  = System.nanoTime()
    val ret = f
    (ret, (System.nanoTime() - t0) / 1000000)
  }
}
