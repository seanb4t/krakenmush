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

package net.muxserver.krakenmush.server

import java.io.File

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Inject, Injector, Provider}
import com.typesafe.config.Config
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kamon.sigar.SigarProvisioner
import net.ceedubs.ficus.Ficus._
import net.codingwell.scalaguice.ScalaModule
import net.muxserver.krakenmush.server.AkkaModule.ActorSystemProvider
import net.muxserver.krakenmush.server.support.GuiceAkkaExtension



/**
 * @since 8/29/15
 */
object AkkaModule {

  class ActorSystemProvider @Inject()(val config: Config, val injector: Injector) extends Provider[ActorSystem] {
    @SuppressFBWarnings(Array("PATH_TRAVERSAL_IN"))
    def get() = {
      SigarProvisioner.provision(new File(config.as[String]("kraken.server.tmpDir")))
      val system = ActorSystem("main-actor-system", config)
      GuiceAkkaExtension(system).initialize(injector)
      system
    }
  }

}

/**
 * A module providing an Akka ActorSystem
 */
class AkkaModule extends AbstractModule with ScalaModule {
  def configure(): Unit = {
    bind[ActorSystem].toProvider[ActorSystemProvider].asEagerSingleton()
  }
}
