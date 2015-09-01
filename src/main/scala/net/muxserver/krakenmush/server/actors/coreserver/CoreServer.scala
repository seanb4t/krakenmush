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
import com.google.inject.Inject
import com.typesafe.config.Config
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import net.ceedubs.ficus.Ficus._
import net.muxserver.krakenmush.server.actors.coreserver.CoreServerProtocol.Error
import net.muxserver.krakenmush.server.actors.netserver._
import net.muxserver.krakenmush.server.support.NamedActor

@SuppressFBWarnings(Array("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"))
object CoreServer extends NamedActor {
  final val name = "CoreServer"

  sealed trait CoreServerState

  case object Running extends CoreServerState

  case object Stopped extends CoreServerState

  sealed trait CoreServerData

  case object Uninitialized extends CoreServerData

  case class ServerInfo(startTime: Instant, stopTime: Option[Instant], currentConnections: List[ActorRef], mainTCPServer: Option[ActorRef])
    extends CoreServerData

}

object CoreServerProtocol {

  case object Start

  case object Stop

  case object Starting

  case object Stopping

  case class Error(message: String, cause: Option[Any] = None)

  case class ClientConnected(client: ActorRef)

  case class ClientDisconnected(client: ActorRef)

}


/**
 * @since 8/30/15
 */
class CoreServer @Inject()(val config: Config) extends FSM[CoreServer.CoreServerState, CoreServer.CoreServerData]
                                                       with TCPServerProducer with ActorLogging {

  import CoreServer.{Running, ServerInfo, Stopped, Uninitialized}
  import CoreServerProtocol.{ClientConnected, Start, Starting, Stop, Stopping}

  val mainServerListenAddress = config.as[String]("kraken.server.listenAddress")
  val mainServerListenPort = config.as[Int]("kraken.server.listenPort")

  startWith(Stopped, Uninitialized)

  when(Stopped) {
                  case Event(Start, Uninitialized) =>
                    log.info("Starting KrakenMUSH from stopped state.")
                    val tcpServer = newTCPServer(mainServerListenAddress, mainServerListenPort)
                    tcpServer ! TCPServerProtocol.Start
                    goto(Running) using ServerInfo(Instant.now, None, List(), Some(tcpServer)) replying Starting
                  case Event(Stop, _) =>
                    stay() replying Error("Cannot stop already stopped server.")
                }

  onTransition {
                 case Stopped -> Running => log.info("Transitioning to running state.")
                 case Running -> Stopped => log.info("Transitioning to stopped state.")
               }

  when(Running) {
                  case Event(ClientConnected(client), serverInfo @ ServerInfo(startTime, stopTime, currentConnections, server)) =>
                    log.info("Connection accepted, current connection count: {}", currentConnections.size)
                    stay using serverInfo.copy(currentConnections = client :: currentConnections)
                  case Event(Stop, serverInfo @ ServerInfo(startTime, stopTime, currentConnections, server)) =>
                    log.info("Stopping server: {}", serverInfo)
                    server.foreach {
                                     _ ! TCPServerProtocol.Stop
                                   }
                    goto(Stopped) using serverInfo.copy(stopTime = Some(Instant.now)) replying Stopping
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
