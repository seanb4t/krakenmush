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
import com.typesafe.config.{Config, ConfigFactory}
import kamon.sigar.SigarProvisioner
import net.ceedubs.ficus.Ficus._
import net.muxserver.krakenmush.server.support.spring.SpringExt
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.{Bean, ComponentScan, Configuration}

/**
 * @since 9/5/15
 */
@Configuration
@ComponentScan(Array("net.muxserver.krakenmush"))
class ServerApplicationConfiguration {

  @Autowired
  implicit var ctx: ApplicationContext = _

  /**
   * Application configuration
   * @return
   */
  @Bean
  def config(): Config = {
    ConfigFactory.load()
  }

  /**
   * Main ActorSystem
   * @return
   */
  @Bean
  def actorSystem() = {
    SigarProvisioner.provision(new File(config().as[String]("kraken.server.tmpDir")))
    val system = ActorSystem("main-actor-system", config())
    SpringExt(system)
    system
  }

}
