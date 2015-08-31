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

package net.muxserver.krakenmush

import akka.actor.ActorSystem
import com.google.inject.Guice
import com.typesafe.scalalogging.StrictLogging
import net.muxserver.krakenmush.config.ConfigModule
import net.muxserver.krakenmush.server.AkkaModule
import net.muxserver.krakenmush.server.actors.CoreActorsModule
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer
import net.muxserver.krakenmush.server.support.GuiceAkkaExtension


/**
 * @since 8/29/2015
 */
object Main extends StrictLogging {


  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(
      new ConfigModule,
      new AkkaModule,
      new CoreActorsModule
    )

    import net.codingwell.scalaguice.InjectorExtensions._
    val actorSystem = injector.instance[ActorSystem]

    val coreServer = actorSystem.actorOf(GuiceAkkaExtension(actorSystem).props(CoreServer.name))

    coreServer ! CoreServer.Start

    //    Thread.sleep(90000)
    //
    //    coreServer ! CoreServer.Stop
    //
    //    val terminationFuture = actorSystem.terminate()
    //    Await.result(terminationFuture, 30 seconds)
  }
}
