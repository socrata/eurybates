package com.socrata
package eurybates
package zookeeper

import java.util.concurrent.Semaphore

class QuasiBlockingLock {
  // ok, so this is a little complicated.  It exists because if ZK
  // goes into a reconnect-loop (which we've only ever seen happen
  // once!) Eurybates will spam a bunch of threads to reread the
  // configuration.  But we only ever need at most two: one currently
  // trying to do so, and one waiting for the current reader to finish
  // (to catch two changes made in quick succession -- we only care
  // about the current state-of-the-world, so the waiting one will get
  // that once the not-waiting one finishes).

  // So we need a lock which is neither purely blocking nor
  // non-blocking.  It will act as a blocking lock if there are no
  // other threads waiting to acquire the lock, or a non-blocking one
  // if there is one.

  private var entered = 0 // count of threads that have successfully entered; will be 0, 1 or 2
  private var waiter: Semaphore = null // not-null only if entered == 2

  def tryWithLock[T]()(f: => T): Option[T] = {
    var s: Semaphore = null

    synchronized {
      if(entered == 2) {
        // There's already something waiting, fail
        return None
      } else if(entered == 1) {
        // No one else is waiting but the lock is held
        assert(waiter == null)
        s = new Semaphore(0)
        waiter = s
      } else {
        // I have the lock!
        assert(entered == 0)
        assert(waiter == null)
      }
      entered += 1
    }

    try {
      if(s != null) s.acquire() // wait for the lock-holder to release
      Some(f)
    } finally {
      synchronized {
        if(waiter != null) {
          assert(entered == 2)
          waiter.release()
          waiter = null
        }
        entered -= 1
      }
    }
  }

  def state() = synchronized {
    (entered, waiter != null)
  }
}
