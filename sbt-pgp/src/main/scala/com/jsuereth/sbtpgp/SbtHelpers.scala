package com.jsuereth.sbtpgp

/** Simple caching api. So simple it's probably horribly bad in some way. OH, right... synchronization could be bad here...
  */
trait Cache[K, V] {
  private val cache = new collection.mutable.HashMap[K, V]

  /** This method attempts to use a cached value, if one is found. If there is no cached value, the default is used and placed back into the cache.
    *
    * Upon any exception, the cache is cleared.
    *
    * TODO - Allow subclasses to handle specific exceptions.
    */
  @inline
  final def withValue[U](key: K, default: => V)(f: V => U): U =
    try
      f(synchronized {
        cache.getOrElseUpdate(key, default)
      })
    catch {
      case t: Exception =>
        // Clear the cache on any exception
        synchronized(cache remove key)
        throw t
    }
}

// TODO - Less ugly/dangerous hack here...
//  - Expire passwords after N minutes etc.
//  - Kill password only on password exceptions.
private[sbtpgp] object PasswordCache extends Cache[String, Array[Char]]
