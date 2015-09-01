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

import akka.actor._
import akka.io._
import net.muxserver.krakenmush.server.actors.client.ClientHandlerProducer


/**
 * @since 8/30/15
 */

object TCPServer {
  def props(listenAddress: String, listenPort: Int): Props = Props(new TCPServer(listenAddress, listenPort))
}

object TCPServerProtocol {

  case object Start

  case object Started

  case object Stop

  case object Stopped

}

trait IOSupport {
  def ioTCP()(implicit system: ActorSystem): ActorRef = IO(Tcp)

  def ioUDP()(implicit system: ActorSystem): ActorRef = IO(Udp)
}

class TCPServer(listenAddress: String, listenPort: Int) extends Actor with ClientHandlerProducer with IOSupport with ActorLogging {

  import TCPServerProtocol._
  import Tcp._

  implicit val actorSystem = context.system

  var boundAddress: Option[InetSocketAddress] = None

  def receive = {
    case Start =>
      log.info("Starting TCP server: {}:{}", listenAddress, listenPort)
      ioTCP ! Bind(self, new InetSocketAddress(listenAddress, listenPort))
      sender ! Started
    case Stop =>
      log.info("Stopping TCP server: {}:{}", listenAddress, listenPort)
      ioTCP ! Unbind
      sender ! Stopped
    case Bound(address) =>
      log.info("TCP Server bound, address: {}", address)
      boundAddress = Some(address)
    case Unbound =>
      log.info("TCP Server bound, address: {}", boundAddress)
      boundAddress = None
    case Connected(localAddress, remoteAddress) =>
      log.info("Client Connected: local: {} remote: {}", localAddress, remoteAddress)
      val connection = sender()
      val clientHandler = newClientHandler(remoteAddress, connection)
      connection ! Register(clientHandler)
    case f @ CommandFailed(_: Bind) => log.warning("Command Failed [Bind]!: {}", f)
  }

}
