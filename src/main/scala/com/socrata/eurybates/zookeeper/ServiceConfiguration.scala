package com.socrata
package eurybates
package zookeeper

import com.socrata.util.error
import com.socrata.zookeeper._
import com.socrata.zookeeper.results.Create.OK
import com.socrata.zookeeper.results._

import java.util.concurrent.Executor

import scala.annotation.tailrec
import scala.util.{Success, Try}

class ServiceConfiguration(zkp: ZooKeeperProvider, executor: Executor, notifyOnChanges: Set[ServiceName] => Unit) {
  final def start(): Traversable[ServiceName] = synchronized {
    while(true) {
      implicit val zk = zkp.get()
      zk.children(root, watcher) match {
        case Children.OK(cs, _) =>
          return cs
        case NotFound =>
          zk.createPath(root)
        case ConnectionLost =>
          zk.waitUntilConnected()
        case SessionExpired =>
      }
    }
    error("Can't get here")
  }

  private val watcher = new Watcher {
    def process(event: WatchedEvent) {
      scheduleUpdate()
    }
  }

  private val root = "/eurybates/services"

  private def path(service: ServiceName) = root + "/" + service

  private val DEFAULT_ZOOKEEPER_RETRIES = 5

  final def registerService(name: ServiceName): Unit = {
    this.registerService(name, DEFAULT_ZOOKEEPER_RETRIES)
  }

  @tailrec
  final def registerService(name: ServiceName, attempts: Int = DEFAULT_ZOOKEEPER_RETRIES): Unit = {
    attempts match {
      case i: Int if i < 0 => throw new IllegalStateException("Could not register service in zookeeper")
      case i: Int =>
        val zk = zkp.get()

        zk.createPath(root)
        zk.create(path(name), persistent = true) match {
          case ConnectionLost =>
            zk.waitUntilConnected()
            this.registerService(name, attempts - 1)
          case SessionExpired =>
            this.registerService(name, attempts - 1)
          case AlreadyExists | NoPath | Create.OK =>
        }
    }
  }

  final def destroyService(name: ServiceName): Unit = {
    this.destroyService(name, DEFAULT_ZOOKEEPER_RETRIES)
  }

  @tailrec
  final def destroyService(name: ServiceName, attempts: Int = DEFAULT_ZOOKEEPER_RETRIES): Unit = {
    attempts match {
      case i: Int if i < 0 => throw new IllegalStateException("Could not delete service from zookeeper")
      case i: Int =>
        val zk = zkp.get()

        zk.deleteAnyVersion(path(name)) match {
          case NotEmpty =>
            throw new IllegalStateException("Service node for " + name + " is not empty")
          case ConnectionLost =>
            zk.waitUntilConnected()
            this.destroyService(name, attempts - 1)
          case SessionExpired =>
            this.destroyService(name, attempts - 1)
          case NotFound | DeleteAnyVersion.OK =>
        }

    }
  }

  private def scheduleUpdate() : Unit = {
    executor.execute(new Runnable() {
      def run() { updateServiceConfiguration() }
    })
  }

  private def updateServiceConfiguration() = {
    synchronized {
      val zk = zkp.get()

      zk.children(root, watcher) match {
        case Children.OK(cs, _) =>
          notifyOnChanges(cs)
        case NotFound =>
          zk.exists(root, watcher) match {
            case Exists.OK(Some(_)) =>
              scheduleUpdate()
            case Exists.OK(None) =>
              notifyOnChanges(Set.empty)
            case ConnectionLost =>
              zk.waitUntilConnected()
              scheduleUpdate()
            case SessionExpired =>
              scheduleUpdate()
          }
        case ConnectionLost =>
          zk.waitUntilConnected()
          scheduleUpdate()
        case SessionExpired =>
          scheduleUpdate()
      }
    }
  }
}
