/*
 * Copyright 2015 Sean Brandt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.muxserver.krakenmush.server.actors.netserver

import java.net.InetSocketAddress
import java.time.Instant

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put}
import akka.io._
import net.muxserver.krakenmush.server.actors.client.ClientHandlerProducer
import net.muxserver.krakenmush.server.{ClusterComms, CoreClusterTopics}


/**
 * @since 8/30/15
 */

object TCPServer {
  def props(listenAddress: String, listenPort: Int): Props = Props(new TCPServer(listenAddress, listenPort))
}

object TCPServerProtocol {

  sealed trait TCPServerMessage

  case object Start extends TCPServerMessage

  case class BindState(bound: Boolean = false, address: Option[InetSocketAddress] = None, errorMsg: Option[String] = None)
    extends TCPServerMessage

  case object Stop extends TCPServerMessage

  case class NewConnection(remoteAddress: InetSocketAddress, connectedAt: Instant)

}


class TCPServer(listenAddress: String, listenPort: Int)
  extends Actor with ClientHandlerProducer with IOSupport with ActorLogging with ClusterComms {

  import TCPServerProtocol._
  import Tcp._

  implicit val actorSystem = context.system

  // cannot restart closed connections.
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  var boundAddress: Option[InetSocketAddress] = None
  private var originActor: ActorRef = _

  override def preStart(): Unit = {
    log.info("Registering with cluster mediator: {}", self.path.toStringWithoutAddress)
    mediator ! Put(self)
  }

  def receive = {
    case Start                                  =>
      log.info("Starting TCP server: {}:{}", listenAddress, listenPort)
      originActor = sender()
      ioTCP ! Bind(self, new InetSocketAddress(listenAddress, listenPort))
    case Stop                                   =>
      log.info("Stopping TCP server: {}:{}", listenAddress, listenPort)
      ioTCP ! Unbind
    case Bound(address)                         =>
      log.info("TCP Server bound, address: {}", address)
      boundAddress = Some(address)
      mediator ! Publish(CoreClusterTopics.SERVER_STATUS, BindState(bound = true, boundAddress))
    case Unbound                                =>
      log.info("TCP Server bound, address: {}", boundAddress)
      boundAddress = None
      mediator ! Publish(CoreClusterTopics.SERVER_STATUS, BindState(bound = false))
    case Connected(localAddress, remoteAddress) =>
      log.info("Client Connected: local: {} remote: {}", localAddress, remoteAddress)
      val connection = sender()
      val clientHandler = newClientHandler(remoteAddress, connection)
      connection ! Register(clientHandler, keepOpenOnPeerClosed = true)
      mediator ! Publish(CoreClusterTopics.CONNECTION_INFO, NewConnection(remoteAddress, Instant.now()))
    case f @ CommandFailed(Bind(_, localAddress, _, _, _)) =>
      val msg = s"Cannot bind to requested address:port: ${ localAddress }"
      log.error(msg)
      boundAddress = None
      originActor ! BindState(bound = false, None, Some(msg))
      context stop self
  }

}
