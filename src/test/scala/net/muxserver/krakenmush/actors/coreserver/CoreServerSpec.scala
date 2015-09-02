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

package net.muxserver.krakenmush.actors.coreserver

import java.time.Instant

import akka.actor.{ActorContext, ActorRef}
import akka.pattern.ask
import akka.testkit._
import codes.reactive.scalatime.Scalatime._
import net.muxserver.krakenmush.actors.BaseActorSpec
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer._
import net.muxserver.krakenmush.server.actors.coreserver.CoreServerProtocol._
import net.muxserver.krakenmush.server.actors.netserver.{TCPServerProducer, TCPServerProtocol}
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => eql}
import org.scalatest.junit.JUnitRunner

import scala.util.Success

/**
 * @since 8/30/15
 */
//noinspection NameBooleanParameters
@RunWith(classOf[JUnitRunner])
class CoreServerSpec extends BaseActorSpec {

  trait TestingTCPServerProducer extends TCPServerProducer {
    var probe: TestProbe = _

    override def newTCPServer(listenAddress: String, listenPort: Int)(implicit context: ActorContext): ActorRef = {
      probe = TestProbe()
      probe.ref
    }
  }

  var coreServer                                                            = TestFSMRef(new
      CoreServer(config) with TestingTCPServerProducer)
  val correctTyping: TestActorRef[CoreServer with TestingTCPServerProducer] = coreServer


  "The CoreServer" must {
    "be stopped when created" in {
      coreServer.stateName must be(Stopped)
      coreServer.stateData must be(Uninitialized)
    }

    "start when sent the Start message" in {
      val future = coreServer ? Start
      val Success(result: Any) = future.value.get
      result must be(Starting)
      coreServer.stateName must be(Running)
      coreServer.stateData must not be Uninitialized
      coreServer.underlyingActor.probe.expectMsg(TCPServerProtocol.Start)
    }

    "stop when sent the Stop message when running" in {
      val clientConnectionProbe = TestProbe()
      val tcpServerProbe = TestProbe()
      val serverInfo = ServerInfo(Instant.now.minus(1L minute), None, List(clientConnectionProbe.ref), Some(tcpServerProbe.ref))
      coreServer.setState(Running, serverInfo)
      val future = coreServer ? Stop
      val Success(result: Any) = future.value.get
      result must be(Stopping)
      coreServer.stateName must be(Stopped)
      inside(coreServer.stateData) { case ServerInfo(startTime, stopTime, clientConnections, server) =>
        stopTime must not be None
        stopTime.foreach(startTime.isBefore(_) must be(true))
        clientConnections must contain(clientConnectionProbe.ref)
        server must contain(tcpServerProbe.ref)
      }
      tcpServerProbe.expectMsg(TCPServerProtocol.Stop)
    }

    "reply with an error when asked to Stop when already stopped" in {
      val future = coreServer ? Stop
      val Success(result: Error) = future.value.get
      coreServer.stateName must be(Stopped)
      result must matchPattern { case Error("Cannot stop already stopped server.", _) => }
    }

    "store a reference to a newly connected client" in {
      val testProbe = TestProbe("ClientHandlerProbe")
      coreServer.setState(Running, ServerInfo(Instant.now.minus(1L minute), None, List(), None))
      coreServer ! ClientConnected(testProbe.ref)
      coreServer.stateName must be(Running)
      inside(coreServer.stateData) { case s @ ServerInfo(_, _, currentConnections, _) =>
        currentConnections must contain(testProbe.ref)
      }
    }

    "starts a TCPServer when started" in {
      val future = coreServer ? Start
      val Success(result: Any) = future.value.get
      result must be(Starting)
      coreServer.stateName must be(Running)
      coreServer.stateData must not be Uninitialized
      coreServer.underlyingActor.probe.expectMsg(TCPServerProtocol.Start)
    }

    "logs unhandled events and stays() in current state" in {
      val future = coreServer ? Start
      val Success(result: Any) = future.value.get
      result must be(Starting)
      coreServer.stateName must be(Running)
      coreServer.stateData must not be Uninitialized
      EventFilter.warning(start = "Received unhandled request: ", occurrences = 1) intercept {
        coreServer ! "TESTEVENT"
      }
      coreServer.stateName must be(Running)
    }
  }

  override protected def beforeEach(): Unit = {
    coreServer = TestFSMRef(new CoreServer(config) with TestingTCPServerProducer)
  }
}
