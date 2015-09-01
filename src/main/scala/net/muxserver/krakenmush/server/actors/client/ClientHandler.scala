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

package net.muxserver.krakenmush.server.actors.client

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp

object ClientHandler {
  def props(remote: InetSocketAddress, connection: ActorRef) = Props(new ClientHandler(remote, connection))
}

/**
 * @since 8/30/15
 */
class ClientHandler(remote: InetSocketAddress, connection: ActorRef) extends Actor with ActorLogging {

  import Tcp._

  context.watch(connection)

  def receive = {
    case Received(data) =>
      val text = data.utf8String.trim
      log.debug("data received: {}", text)
      text match {
        case _ => sender ! Write(data)
      }
    case e@PeerClosed =>
      log.debug("peer closed, shutting down: {}", e)
      context.stop(self)
    case Terminated(`connection`) =>
      log.debug("Stopping due to terminated connection.")
      context.stop(self)
  }
}
