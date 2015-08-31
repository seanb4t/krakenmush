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

import akka.pattern.ask
import akka.testkit.{TestActorRef, TestFSMRef}
import codes.reactive.scalatime.Scalatime._
import net.muxserver.krakenmush.actors.BaseActorSpec
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer._
import net.muxserver.krakenmush.server.actors.coreserver.CoreServerProtocol._
import org.junit.runner.RunWith
import org.mockito.Matchers.{eq => eql}
import org.scalatest.junit.JUnitRunner

import scala.util.Success

/**
 * @since 8/30/15
 */
//noinspection NameBooleanParameters
@RunWith(classOf[JUnitRunner])
class CoreServerTest extends BaseActorSpec {


  var coreServer = TestFSMRef(new CoreServer(config))
  val correctTyping: TestActorRef[CoreServer] = coreServer


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
    }

    "stop when sent the Stop message when running" in {
      coreServer.setState(Running, ServerInfo(Instant.now.minus(1L minute), None, List(), None))
      val future = coreServer ? Stop
      val Success(result: Any) = future.value.get
      result must be(Stopping)
      coreServer.stateName must be(Stopped)
      inside(coreServer.stateData) { case ServerInfo(startTime, stopTime, _, _) =>
        stopTime must not be None
        stopTime.foreach(startTime.isBefore(_) must be(true))
      }
    }

    "reply with an error when asked to Stop when already stopped" in {
      val future = coreServer ? Stop
      val Success(result: Error) = future.value.get
      coreServer.stateName must be(Stopped)
      result must matchPattern { case Error("Cannot stop already stopped server.", _) => }
    }

  }

  override protected def beforeEach(): Unit = {
    coreServer = TestFSMRef(new CoreServer(config))
  }
}
