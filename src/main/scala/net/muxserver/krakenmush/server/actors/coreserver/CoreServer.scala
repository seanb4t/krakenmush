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

package net.muxserver.krakenmush.server.actors.coreserver

import java.time.Instant

import akka.actor._
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Put, Send}
import com.typesafe.config.Config
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.ceedubs.ficus.Ficus._
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProducer
import net.muxserver.krakenmush.server.actors.coreserver.CoreServerProtocol.{Error, Started}
import net.muxserver.krakenmush.server.actors.netserver.TCPServerProtocol.BindState
import net.muxserver.krakenmush.server.actors.netserver._
import net.muxserver.krakenmush.server.{ClusterComms, CoreClusterAddresses, CoreClusterTopics}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation
import org.springframework.stereotype.Component

@SuppressFBWarnings(Array("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"))
object CoreServer {
  val name = "CoreServer"

  sealed trait CoreServerState

  case object Running extends CoreServerState

  case object Stopped extends CoreServerState

  sealed trait CoreServerData

  case object Uninitialized extends CoreServerData

  case class ServerInfo(startTime: Instant, stopTime: Option[Instant], currentConnections: List[ActorRef])
    extends CoreServerData

}

object CoreServerProtocol {

  case object Start

  case object Stop

  case object Starting

  case object Started

  case object Stopping

  case class Error(message: String, cause: Option[Any] = None)

  case class ClientConnected(client: ActorRef)

  case class ClientDisconnected(client: ActorRef)

}


/**
 * @since 8/30/15
 */
@Component("CoreServer")
@annotation.Scope("prototype")
class CoreServer @Autowired()(val config: Config) extends FSM[CoreServer.CoreServerState, CoreServer.CoreServerData] with ClusterComms
with TCPServerProducer
with CommandExecutorProducer
with ActorLogging {

  import CoreServer.{Running, ServerInfo, Stopped, Uninitialized}
  import CoreServerProtocol.{ClientConnected, Start, Stop, Stopping}

  val mainTcpServer: ActorRef = newTCPServer(config.as[String]("kraken.server.listenAddress"), config.as[Int]("kraken.server.listenPort")
    , Some("mainTCPServer"))
  context.watch(mainTcpServer)
  val commandExecutor: ActorRef = newCommandExecutor(config)
  context.watch(commandExecutor)

  startWith(Stopped, Uninitialized)

  when(Stopped) {
    case Event(Start, Uninitialized) =>
      log.info("Starting KrakenMUSH from stopped state.")
      mediator ! Put(self)
      mediator ! Send(CoreClusterAddresses.TCP_SERVER_MAIN, TCPServerProtocol.Start, localAffinity = false)
      mediator ! Publish(CoreClusterTopics.SERVER_STATUS, Started)
      goto(Running) using ServerInfo(Instant.now, None, List())
    case Event(Stop, _)              =>
      stay() replying Error("Cannot stop already stopped server.")
  }

  onTransition {
    case Stopped -> Running => log.info("Transitioning to running state.")
    case Running -> Stopped => log.info("Transitioning to stopped state.")
  }

  when(Running) {
    case Event(ClientConnected(client), serverInfo@ServerInfo(startTime, stopTime, currentConnections)) =>
      val updatedConnections = client :: currentConnections
      log.info("Connection accepted, current connection count: {}", updatedConnections.size)
      stay using serverInfo.copy(currentConnections = updatedConnections)
    case Event(Stop, serverInfo@ServerInfo(startTime, stopTime, currentConnections)) =>
      log.info("Stopping server: {}", serverInfo)
      mediator ! Send(CoreClusterAddresses.TCP_SERVER_MAIN, TCPServerProtocol.Stop, localAffinity = false) //TODO wait!?
      goto(Stopped) using serverInfo.copy(stopTime = Some(Instant.now)) replying Stopping
    case Event(BindState(bound, address, msg), ServerInfo(_, _, _)) =>
      if (bound) {
        log.info("mainTCPServer is bound at address: {}", address)
        stay()
      } else {
        log.warning("Unable to bind mainTCPServer.", msg.get)
        stay() //TODO: ?
      }
  }


  whenUnhandled {
    case Event(e, s) =>
      log.warning("Received unhandled request: {} in state {}/{}", e, stateName, s)
      stay()
  }

  onTermination {
    case StopEvent(reason, state, data) =>
      log.warning("Terminating due to {} event from state: {} with info: {}", reason, state, data)
  }

}
