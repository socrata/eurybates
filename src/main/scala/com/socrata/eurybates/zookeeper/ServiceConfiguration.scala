package com.socrata
package eurybates
package zookeeper

import com.socrata.zookeeper._
import com.socrata.zookeeper.results._

import java.util.concurrent.Executor

import scala.annotation.tailrec

// TODO: Remove ZooKeeper dependency
class ServiceConfiguration(zkp: ZooKeeperProvider, executor: Executor, notifyOnChanges: Set[ServiceName] => Unit) {

  final def start(): Traversable[ServiceName] = synchronized {
    startHelper()
  }

  // This helper is needed because synchronized methods cannot be recursive
  // Must be synchronized before use
  @tailrec private def startHelper(): Traversable[ServiceName] = {
    implicit val zk = zkp.get()
    zk.children(root, watcher) match {
      case Children.OK(cs, _) =>
        cs
      case NotFound =>
        zk.createPath(root)
        startHelper()
      case ConnectionLost =>
        zk.waitUntilConnected()
        startHelper()
      case SessionExpired =>
        startHelper()
    }
  }

  private val watcher = new Watcher {
    def process(event: WatchedEvent): Unit = {
      scheduleUpdate()
    }
  }

  private val root = "/eurybates/services"

  private def path(service: ServiceName) = root + "/" + service

  private val DefaultZookeeperRetries = 5

  final def registerService(name: ServiceName): Unit = {
    this.registerService(name, DefaultZookeeperRetries)
  }

  @tailrec
  final def registerService(name: ServiceName, attempts: Int = DefaultZookeeperRetries): Unit = {
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
    this.destroyService(name, DefaultZookeeperRetries)
  }

  @tailrec
  final def destroyService(name: ServiceName, attempts: Int = DefaultZookeeperRetries): Unit = {
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

  private def scheduleUpdate(): Unit = {
    executor.execute(new Runnable() {
      def run(): Unit = {
        updateServiceConfiguration()
      }
    })
  }

  private def updateServiceConfiguration(): Unit = {
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
