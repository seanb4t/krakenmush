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

import java.util.{Set => JSet}

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import kadai.config.Configuration
import net.muxserver.krakenmush.server.database.model.{DatabaseModelPlugin, GameMetaDataPlugin}
import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future, duration}
import scala.language.existentials
import scala.util.{Failure, Success, Try}

/**
 * @since 9/12/15
 */
object Database {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val executionContext = global
}

class Database @Inject()(config: Configuration, modelPlugins: JSet[DatabaseModelPlugin]) extends StrictLogging {

  import Database.executionContext

  private final val dbConfig                                         = config[Configuration]("kraken.database")
  private final val dbDriver                                         = new MongoDriver(Option(dbConfig.toConfig))
  final         val database      : Try[DefaultDB]                   = for {
    uri <- MongoConnection.parseURI(dbConfig[String]("uri"))
    conn <- Try(dbDriver.connection(uri))
    dbName <- Try(uri.db.get)
    db <- Try(conn.db(dbName))
  } yield {
      db
    }
  private final val modelPluginMap: Map[String, DatabaseModelPlugin] = modelPlugins.map { m => (m.modelName, m) }.toMap
  final         val initialized                                      = Await.result(bootstrap(), duration.Duration.Inf)

  def bootstrap(): Future[Boolean] = {
    logger.warn("Bootstrapping Database!")
    database match {
      case Success(db) =>
        val results: Map[String, Future[Boolean]] = modelPlugins.map { plugin => (plugin.modelName, plugin.init(db)) }.toMap
        Future.reduce(results.values)(_ && _)
      case Failure(ex) =>
        logger.error("Unable to bootstrap the DB.", ex)
        Future.failed(ex)
    }
  }

  def meta = {
    modelPluginMap(GameMetaDataPlugin.modelName).DAO
  }

  def rooms = {
    ???
  }

  def characters = {
    ???
  }

}
