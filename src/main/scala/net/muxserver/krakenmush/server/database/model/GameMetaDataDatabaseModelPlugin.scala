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

package net.muxserver.krakenmush.server.database.model

import java.time.Instant

import com.google.inject.Inject
import kadai.config.Configuration
import net.muxserver.krakenmush.server.support.neo4j.OGMConverters.InstantToLongConverter
import org.neo4j.ogm.session.Session

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * @since 9/16/15
 */
class GameMetaDataDatabaseModelPlugin @Inject() (override val config: Configuration) extends DatabaseModelPlugin(config) {
  type T = GameMetaData
  val packageNames = Set(classOf[GameMetaData].getPackage.getName)
  val modelName    = classOf[GameMetaData].getSimpleName

  override def init(implicit session: Session): Future[Boolean] = {
    logger.info("Initializing Model")
    val result = Promise[Boolean]

    if (session.countEntitiesOfType(classOf[GameMetaData]) != 1) {
      logger.info("Creating new GameMetaData - new system starting up!")
      val gmd = GameMetaData(
        name = config[String]("kraken.game.name"),
        version = config[String]("kraken.game.version"),
        gameOwner = Player(config[String]("kraken.game.owner-name"), config[String]("kraken.game.owner-password"))
      )
      Try(session.save(gmd)) match {
        case Success(_) => result.success(true)
        case Failure(ex) => result.failure(ex)
      }
    } else {
      logger.debug("System already has a GameMetaData!")
      Try(session.loadAll(classOf[GameMetaData])) match {
        case Success(results) =>
          val gmd = results.head
          gmd.lastStartDate = Instant.now()
          session.save(gmd)
          result.success(true)
        case Failure(ex) => result.failure(ex)
      }
    }

    result.future
  }
}

import net.muxserver.krakenmush.server.database.model.DatabaseModelAnnotations._

class GameMetaData extends DatabaseModel {
  var version: String = "1.0"
  @Relationship(`type`="OWNER")
  var gameOwner: Player = _
  @Property
  @Convert(classOf[InstantToLongConverter])
  var lastStartDate: Instant = Instant.now()
}

object GameMetaData {
  def apply(name: String, version: String, gameOwner: Player, lastStartDate: Instant = Instant.now()): GameMetaData = {
    val gmd = new GameMetaData()
    gmd.gameOwner = gameOwner
    gmd.name = name
    gmd.version = version
    gmd
  }
}
