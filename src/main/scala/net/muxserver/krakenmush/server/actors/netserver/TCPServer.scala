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
import akka.io.{IO, Tcp}
import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import net.muxserver.krakenmush.server.actors.client.ClientHandler
import net.muxserver.krakenmush.server.support.NamedActor


/**
 * @since 8/30/15
 */

object TCPServer extends NamedActor {
  final val name = "TCPServer"

  def props(config: Config): Props = Props(new TCPServer(config))

}

object TCPServerProtocol {

  case object Start

  case object Starting

  case object Stop

  case object Stopping

}

class TCPServer(val config: Config) extends Actor with ActorLogging {
  val listenAddress: String = config.as[String]("kraken.server.listenAddress")
  val listenPort: Int = config.as[Int]("kraken.server.listenPort")

  import TCPServerProtocol._
  import Tcp._
  import context.system

  var boundAddress: Option[InetSocketAddress] = _

  def receive = {
    case Start =>
      log.info("Starting TCP server: {}:{}", listenAddress, listenPort)
      IO(Tcp) ! Bind(self, new InetSocketAddress(listenAddress, listenPort))
      sender ! Starting
    case Stop =>
      log.info("Stopping TCP server: {}:{}", listenAddress, listenPort)
      IO(Tcp) ! Unbind
    case b@Bound(address) =>
      log.info("TCP Server bound, address: {}", address)
      boundAddress = Some(address)
    case u@Unbound =>
      log.info("TCP Server bound, address: {}", boundAddress)
      boundAddress = null
    case c@Connected(localAddress, remoteAddress) =>
      log.info("Client Connected: local: {} remote: {}", localAddress, remoteAddress)
      val connection = sender()
      val clientHandler = context.actorOf(ClientHandler.props(config, remoteAddress, connection))
      connection ! Register(clientHandler)
    case f@CommandFailed(_: Bind) => log.warning("Command Failed!: {}", f)
  }

}
