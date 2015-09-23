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

import kadai.config.Configuration
import net.muxserver.krakenmush.BaseSpec
import net.muxserver.krakenmush.server.database.model._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.IntegrationPatience
import reactivemongo.api.DefaultDB

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * @since 9/16/15
 */
class DatabaseSpec extends BaseSpec with IntegrationPatience {
  val config = Configuration.from(
    s"""
       |kraken {
       | database {
       |   uri = "mongodb://localhost:27017/krakenTestDB?authMode=scram-sha1&rm.tcpNoDelay=true&rm
       |   .keepAlive=true&writeConcernW=1&readPreference=primaryPreferred"
       |   mongo-async-driver {
       |   }
       | }
       |}
    """.stripMargin)
  "The Database" must {
    "load the configuration" in {
      val databaseModel = mock[GameMetaDataPlugin](name="DBModel1")
      when(databaseModel.init(any[DefaultDB])).thenReturn(Future.successful(true))
      val db = new Database(config, Set[DatabaseModelPlugin](databaseModel))
      db.database.isSuccess must be(true)
      verify(databaseModel).init(db.database.get)
    }

    "Bootstraps when there's no data" in {
      val gameMetaDataPlugin: GameMetaDataPlugin = new GameMetaDataPlugin()
      val db = new Database(config, Set[DatabaseModelPlugin](gameMetaDataPlugin))
      db.database.isSuccess must be(true)
      val futureNames = db.database.get.collectionNames
      whenReady(futureNames) { result =>
        result must contain(model.GameMetaDataPlugin.collectionName)
        gameMetaDataPlugin.collection must not be null
      }
    }
  }
}
