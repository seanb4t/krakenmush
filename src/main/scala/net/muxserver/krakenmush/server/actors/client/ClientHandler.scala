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
import akka.cluster.pubsub.DistributedPubSubMediator.Send
import akka.io.Tcp
import akka.util.ByteString
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProtocol
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProtocol.ExecuteRawCommand
import net.muxserver.krakenmush.server.commands.CommandExecutionContext
import net.muxserver.krakenmush.server.support.StringProcessingSupport
import net.muxserver.krakenmush.server.{ClusterComms, CoreClusterAddresses}

object ClientHandler {
  def props(remote: InetSocketAddress, connection: ActorRef) = Props(new
      ClientHandler(remote, connection))

  def renderOutputForClient(data: Option[String]) = {
    var output = data match {
      case Some(s: String) => s
      case None => ""
    }
    output = output match {
      case s if s.matches( """\r\n$""") => s
      case "" => "\r\n"
      case s: String => s"$s\r\n"
    }
    ByteString(output)
  }
}



/**
 * @since 8/30/15
 */
class ClientHandler(remote: InetSocketAddress, connection: ActorRef)
  extends Actor with ActorLogging with ClusterComms with StringProcessingSupport {

  import ClientHandler.renderOutputForClient
  import Tcp._

  context.watch(connection)
  var alive : Boolean = false
  val client: Client  = Client(self, remote)

  /**
   * Main receive handler.
   * TODO: Assumes data comes in 'lines' from the client, will likely need to revisit.
   */
  def receive = {
    case Received(data)           =>
      val text = normalizeAndStrip(data.utf8String)
      log.debug("Raw data from client: [{}] >: {} :<", remote, data.toArray.map(_.toInt).mkString(","))
      log.debug("Normalized data received for client: [{}] >: {} :<", remote, text)
      client.update(data.length)
      text match {
        case "" => log.debug("Client sent no data or all whitespace: no-op or keep alive.")
        case s: String =>
          mediator ! Send(CoreClusterAddresses
            .COMMAND_EXECUTOR, ExecuteRawCommand(CommandExecutionContext(client), text), localAffinity = false)
      }
    case CommandExecutorProtocol.CommandSuccess(executionContext, cmd, someData) =>
      log.debug("Sending result for command: {}", cmd)
      connection ! Write(renderOutputForClient(someData))
    case CommandExecutorProtocol.CommandFailed(executionContext, originalCommand, message) =>
      log.debug("Command failed: {}", originalCommand)
      connection ! Write(renderOutputForClient(message.orElse(Option("Huh? I don't understand what you're trying to tell me."))))
    case e @ PeerClosed           =>
      log.info("Client closed, shutting down: [{}]/{}", remote, e)
      context.stop(self)
    case Terminated(`connection`) =>
      log.info("Stopping due to terminated connection.")
      context.stop(self)
  }

}


