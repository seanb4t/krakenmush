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

package net.muxserver.krakenmush.actors

import java.io.File

import akka.actor.ActorSystem
import akka.testkit._
import com.typesafe.config.{Config, ConfigFactory}
import kamon.sigar.SigarProvisioner
import net.muxserver.krakenmush.BaseRequiredSpecs
import org.mockito.Matchers.{eq => eql}

object BaseActorSpec {
  SigarProvisioner.provision(new File("./build/.native"))
  val testConfig =
    """
      |akka {
      | loglevel = "DEBUG"
      | loggers = [akka.testkit.TestEventListener]
      |}
      |kraken {
      |  server {
      |    listenAddress = 0.0.0.0
      |    listenPort = 0
      |    tmpDir = ./build/.native
      |  }
      |  game {
      |    name = KrakenMUSH
      |  }
      |}
      |
    """.stripMargin
  val config     = ConfigFactory.parseString(testConfig)
}

/**
 * @since 8/30/15
 */
abstract class BaseActorSpec extends TestKit(ActorSystem("testsystem", BaseActorSpec.config))
with BaseRequiredSpecs
with StopSystemAfterAll
with DefaultTimeout
with ImplicitSender {

  var config: Config = BaseActorSpec.config

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }
}
