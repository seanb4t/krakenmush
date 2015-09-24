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
import net.muxserver.krakenmush.server.database.model.DatabaseModelPlugin
import org.neo4j.ogm.session.{Session, SessionFactory}

import scala.collection.JavaConversions._
import scala.concurrent.{Await, Future, duration}
import scala.language.existentials
import scala.util.{Failure, Success, Try}

/**
 * @since 9/12/15
 */
class Database @Inject()(config: Configuration, modelPlugins: JSet[DatabaseModelPlugin]) extends StrictLogging {

  implicit val executionContext =  Database.executionContext

  private final implicit val neo4jSessionFactory: Try[SessionFactory] = Try(new SessionFactory(modelPlugins.map(_.packageNames).flatten.toArray:_*))

  implicit def neo4jSession: Session = neo4jSessionFactory.get.openSession(
    s"http://${config[String]("kraken.server.database.host")}:${config[String]("kraken.server.database.port")}",
    config[String]("kraken.server.database.username"),
    config[String]("kraken.server.database.password")
  )

  private final val modelPluginMap: Map[String, DatabaseModelPlugin] = modelPlugins.map { m => (m.modelName, m) }.toMap
  final         val initialized                                      = Await.result(bootstrap(), duration.Duration.Inf)

  def bootstrap(): Future[Boolean] = {
    logger.warn("Bootstrapping Database!")

    neo4jSessionFactory match {
      case Success(db) =>
        val results: Map[String, Future[Boolean]] = modelPlugins.map { plugin => (plugin.modelName, plugin.init) }.toMap
        Future.reduce(results.values)(_ && _)
      case Failure(ex) =>
        logger.error("Unable to bootstrap the DB.", ex)
        Future.failed(ex)
    }
  }

  def meta = {
    ???
  }

  def rooms = {
    ???
  }

  def characters = {
    ???
  }

}

object Database {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val executionContext = global
}

