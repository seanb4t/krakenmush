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

import net.muxserver.krakenmush.server.database.model._
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.neo4j.ogm.session.Session

import scala.collection.JavaConversions._
import scala.concurrent.Future

/**
 * @since 9/16/15
 */
class DatabaseSpec extends BaseDatabaseSpec {
  "The Database" must {
    "load the configuration" in {
      val databaseModel = mock[GameMetaDataDatabaseModelPlugin](name="DBModel1")
      when(databaseModel.packageNames).thenReturn(Set(classOf[GameMetaData].getPackage.getName))
      when(databaseModel.init(any[Session])).thenReturn(Future.successful(true))
      val db = new Database(config, Set[DatabaseModelPlugin](databaseModel))
      db.initialized must be(true)
      verify(databaseModel).init(any[Session])
    }

    "Bootstraps when there's no data" in {
      val gameMetaDataPlugin: GameMetaDataDatabaseModelPlugin = new GameMetaDataDatabaseModelPlugin(config)
      val db = new Database(config, Set[DatabaseModelPlugin](gameMetaDataPlugin))
      db.initialized must be(true)
      db.neo4jSession.countEntitiesOfType(classOf[GameMetaData]) must be(1)
    }
  }
}
