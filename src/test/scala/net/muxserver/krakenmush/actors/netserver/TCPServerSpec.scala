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

package net.muxserver.krakenmush.actors.netserver

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Tcp._
import akka.testkit._
import net.muxserver.krakenmush.actors.BaseActorSpec
import net.muxserver.krakenmush.server.actors.netserver.TCPServerProtocol._
import net.muxserver.krakenmush.server.actors.netserver.{IOSupport, TCPServer}
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => eql}
import org.scalatest.junit.JUnitRunner

/**
 * @since 8/30/15
 */
//noinspection NameBooleanParameters
@RunWith(classOf[JUnitRunner])
class TCPServerSpec extends BaseActorSpec {

  trait TestingIOSupport extends IOSupport {
    var tcpProbe: TestProbe = _
    var useProbe            = false

    override def ioTCP()(implicit system: ActorSystem): ActorRef = {
      if (useProbe) {
        tcpProbe = TestProbe()
        tcpProbe.ref
      } else {
        super.ioTCP()
      }
    }
  }

  var tcpServer: TestActorRef[TCPServer with TestingIOSupport] =
    TestActorRef(Props(new TCPServer("0.0.0.0", 0, TestProbe("CommandExecutor").ref) with TestingIOSupport))

  var tcpServerAlreadyBoundPort: TestActorRef[TCPServer with TestingIOSupport] =
    TestActorRef(Props(new TCPServer("0.0.0.0", 22, TestProbe("CommandExecutor").ref) with TestingIOSupport))

  "A TCPServer" must {
    "be stopped when instantiated" in {
      tcpServer.underlyingActor.boundAddress must be(None)
    }

    "start when sent Start" in {
      tcpServer.underlyingActor.useProbe = true
      EventFilter.info(start = "Starting TCP server:", occurrences = 1) intercept {
        tcpServer ! Start
        tcpServer.underlyingActor.tcpProbe
          .expectMsgPF() { case Bind(_, localAddress, _, _, _) => localAddress.getAddress.getHostAddress == "0.0.0.0" }
        tcpServer.underlyingActor.tcpProbe.send(tcpServer, Bound(new InetSocketAddress("0.0.0.0", 0)))
        expectMsg(BindState(true, None))
        expectMsg(Started)
      }
    }

    "stop when sent Stop" in {
      tcpServer.underlyingActor.useProbe = true
      EventFilter.info(start = "Stopping TCP server:", occurrences = 1) intercept {
        tcpServer ! Start
        tcpServer.underlyingActor.tcpProbe
          .expectMsgPF() { case Bind(_, localAddress, _, _, _) => localAddress.getAddress.getHostAddress == "0.0.0.0" }
        tcpServer.underlyingActor.tcpProbe.send(tcpServer, Bound(new InetSocketAddress("0.0.0.0", 0)))
        expectMsg(BindState(true, None))
        expectMsg(Started)

        tcpServer ! Stop
        tcpServer.underlyingActor.tcpProbe.expectMsg(Unbind)
        tcpServer.underlyingActor.tcpProbe.send(tcpServer, Unbound)
        expectMsg(BindState(false, None))
        expectMsg(Stopped)
      }
    }

    "receives a Bound notification when started and bound to an unused port" in {
      tcpServer.underlyingActor.useProbe = false
      EventFilter.info(start = "TCP Server bound, address:", occurrences = 1) intercept {
        tcpServer ! Start
        expectMsg(BindState(true))
        expectMsg(Started)
      }
    }
    "receives an CommandFailed : Bind notification when started and bound to an in use port" in {
      tcpServerAlreadyBoundPort.underlyingActor.useProbe = false
      EventFilter.error(start = "Cannot bind to requested address:port:", occurrences = 1) intercept {
        tcpServerAlreadyBoundPort ! Start
        expectMsgPF() {
          case BindState(bound, errMsg) =>
            val correctMessage = errMsg match {
              case Some(msg) => msg.startsWith("Cannot bind to requested address:port:")
              case _         => false
            }
            correctMessage && !bound
        }
      }
    }

    "create a ClientHandler when connected" in {
      EventFilter.info(start = "Client Connected: local:", occurrences = 1) intercept {
        tcpServer ! Connected(new InetSocketAddress("127.1.0.1", 3333), new InetSocketAddress("127.0.0.1", 7177))
        expectMsgPF() { case Register(clientHandler, _, _) => clientHandler.isInstanceOf[ActorRef] }
      }
    }
  }

}
