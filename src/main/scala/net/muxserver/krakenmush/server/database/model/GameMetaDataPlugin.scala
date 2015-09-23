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

import net.muxserver.krakenmush.server.database.Database
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import reactivemongo.extensions.dao.BsonDao

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success}


/**
 * @since 9/16/15
 */
class GameMetaDataPlugin extends DatabaseModelPlugin {
  type T = GameMetaData
  val collectionName                                  = GameMetaData.collectionName
  val modelName                                       = GameMetaData.modelName
  var collection: BSONCollection                      = _
  var DAO       : BsonDao[GameMetaData, BSONObjectID] = _

  override def init(db: DefaultDB): Future[Boolean] = {
    logger.info("Initializing Model")
    val result = Promise[Boolean]
    val collectionPromise = Promise[BSONCollection]()

    db.collectionNames onComplete {
      case Success(names) =>
        names.find(_.equals(collectionName)) match {
          case Some(n) =>
            collectionPromise.success(db.collection(n))
          case None =>
            Await.result(db[BSONCollection](collectionName).create(), Duration.Inf)
            collectionPromise.success(db.collection(collectionName))
        }
      case Failure(t) =>
        collectionPromise.failure(t)
        result.failure(t)
    }
    collectionPromise.future map { coll =>
      collection = coll
      DAO = GameMetaData.DAO(db)
      true
    }
  }
}

sealed case class GameMetaData(
  _id: BSONObjectID = BSONObjectID.generate,
  name: String = "KrakenMUSH",
  version: String = "1.0",
  createDate: Instant = Instant.now,
  lastStartDate: Instant = Instant.now
) extends DatabaseModel

object GameMetaData {
  val collectionName    = "meta"
  val modelName: String = "meta"

  import Database.executionContext

  implicit object GameMetaDataReader extends BSONDocumentReader[GameMetaData] with BSONDocumentWriter[GameMetaData] {
    override def read(bson: BSONDocument): GameMetaData = {
      for {
        id <- bson.getAs[BSONObjectID]("_id")
        name <- bson.getAs[String]("name")
        version <- bson.getAs[String]("version")
        createDate <- bson.getAs[BSONDateTime]("createDate")
        lastStartDate <- bson.getAs[BSONDateTime]("lastStartDate")
      } yield {
        val cDate = Instant.ofEpochMilli(createDate.value)
        val lmDate = Instant.ofEpochMilli(lastStartDate.value)
        GameMetaData(id, name, version, cDate, lmDate)
      }
    }.getOrElse(throw new DatabaseModelReadException("Cannot read GameMetaData"))

    override def write(t: GameMetaData): BSONDocument = ???
  }

  def DAO(db: DefaultDB) = BsonDao[GameMetaData,BSONObjectID](db,collectionName)
}

object GameMetaDataPlugin {
  val collectionName = GameMetaData.collectionName
  val modelName      = GameMetaData.modelName

}
