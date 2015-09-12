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

package net.muxserver.krakenmush.server.database

import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import net.ceedubs.ficus.Ficus._
import reactivemongo.api.{MongoConnection, MongoDriver}

import scala.util.Try

/**
 * @since 9/12/15
 */
object Database {
  def apply(config: Config): Database = new Database(config)
}

class Database(config: Config) extends StrictLogging {
  private val driver = MongoDriver(config)
  val connection: Try[MongoConnection] = MongoConnection.parseURI(config.as[String]("uri")).map {driver.connection}
}
