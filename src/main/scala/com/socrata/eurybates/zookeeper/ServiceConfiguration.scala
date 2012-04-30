package com.socrata
package eurybates
package zookeeper

import com.socrata.util.error
import com.socrata.zookeeper._
import com.socrata.zookeeper.results._

import java.util.concurrent.Executor

class ServiceConfiguration(zkp: ZooKeeperProvider, executor: Executor, notifyOnChanges: Set[ServiceName] => Unit) {
  final def start(): Set[ServiceName] = synchronized {
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

  def createService(name: ServiceName) {
    while(true) {
      val zk = zkp.get()

      zk.create(path(name), persistent = true) match {
        case Create.OK | AlreadyExists =>
          return
        case NoPath =>
          zk.createPath(root)
        case ConnectionLost =>
          zk.waitUntilConnected()
        case SessionExpired =>
          // just reseat zk at the top of the loop and retry
      }
    }
  }

  def destroyService(name: ServiceName) {
    while(true) {
      val zk = zkp.get()

      zk.deleteAnyVersion(path(name)) match {
        case DeleteAnyVersion.OK | NotFound =>
          return
        case NotEmpty =>
          throw new IllegalStateException("Service node for " + name + " is not empty")
        case ConnectionLost =>
          zk.waitUntilConnected()
          // and retry
        case SessionExpired =>
          // and retry
      }
    }
  }

  private def scheduleUpdate() {
    executor.execute(new Runnable() {
      def run() { updateServiceConfiguration() }
    })
  }

  private def updateServiceConfiguration() {
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
