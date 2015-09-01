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

import akka.actor._
import akka.io.Tcp.{Bind, Unbind}
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

    override def ioTCP()(implicit system: ActorSystem): ActorRef = {
      tcpProbe = TestProbe()
      tcpProbe.ref
    }
  }

  var tcpServer: TestActorRef[TCPServer with TestingIOSupport] = TestActorRef(Props(new TCPServer("0.0.0.0", 0) with TestingIOSupport))

  "A TCPServer" must {
    "be stopped when instantiated" in {
      tcpServer.underlyingActor.boundAddress must be(None)
    }

    "start when sent Start" in {
      EventFilter.info(start = "Starting TCP server:", occurrences = 1) intercept {
        tcpServer ! Start
        expectMsg(Started)
        tcpServer.underlyingActor.tcpProbe
          .expectMsgPF() { case Bind(_, localAddress, _, _, _) => localAddress.getAddress.getHostAddress == "0.0.0.0" }
      }
    }
    "stop when sent Stop" in {
      EventFilter.info(start = "Stopping TCP server:", occurrences = 1) intercept {
        tcpServer ! Stop
        expectMsg(Stopped)
        tcpServer.underlyingActor.tcpProbe.expectMsg(Unbind)
      }
    }
  }

}
