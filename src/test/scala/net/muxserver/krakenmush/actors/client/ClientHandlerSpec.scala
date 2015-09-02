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

package net.muxserver.krakenmush.actors.client

import java.net.InetSocketAddress

import akka.io.Tcp.PeerClosed
import akka.testkit._
import net.muxserver.krakenmush.actors.BaseActorSpec
import net.muxserver.krakenmush.server.actors.client.ClientHandler

/**
 * @since 9/1/15
 */
class ClientHandlerSpec extends BaseActorSpec {

  var clientConnectionProbe: TestProbe                   = _
  var clientHandler        : TestActorRef[ClientHandler] = _


  "A ClientHandler " must {
    "stop when connection terminated" in {
      EventFilter.info(message = "Stopping due to terminated connection.", occurrences = 1) intercept {
        system.stop(clientConnectionProbe.ref)
      }
    }

    "stop when client closes the connection" in {
      EventFilter.info(start = "Client closed, shutting down:", occurrences = 1) intercept {
        clientHandler ! PeerClosed
      }
    }

    "restricts data input to printable characters (UTF-8)" in {

    }
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    clientConnectionProbe = TestProbe()
    clientHandler = TestActorRef(ClientHandler
      .props(new InetSocketAddress("127.1.1.1", 63333), clientConnectionProbe.ref))

  }
}
