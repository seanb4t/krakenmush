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

import com.typesafe.scalalogging.StrictLogging
import net.muxserver.krakenmush.server.database.Database
import reactivemongo.api.DefaultDB
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONObjectID
import reactivemongo.extensions.dao.BsonDao

import scala.concurrent.Future


/**
 * @since 9/16/15
 */
trait DatabaseModelPlugin extends StrictLogging {
  type T <: DatabaseModel
  implicit val executionContext = Database.executionContext

  def modelName: String

  def collectionName: String

  def init(db: DefaultDB): Future[Boolean]

  def collection: BSONCollection

  def DAO: BsonDao[T, BSONObjectID]
}

trait DatabaseModel

class DatabaseModelReadException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {

}
