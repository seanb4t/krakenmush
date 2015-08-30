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

import javax.inject.Singleton

import com.google.inject.Inject
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus._


/**
 * @since 8/29/15
 */
@Singleton
class DefaultServer @Inject()(val config: Config) extends Server with StrictLogging {
  override val listenAddress: String = config.as[String]("kraken.server.listenAddress")
  override val listenPort: Int = config.as[Int]("kraken.server.listenPort")

  override def stop(): Unit = {
    logger.info(s"Stopping KrakenMUSH server listening on: $listenAddress:$listenPort")
  }

  override def start(): Unit = {
    logger.info(s"Starting KrakenMUSH server listening on: $listenAddress:$listenPort")
  }

}
