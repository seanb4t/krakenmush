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

import net.muxserver.krakenmush.server.database.model.{Player, DatabaseModelPlugin, PlayerDatabaseModelPlugin}
import org.neo4j.ogm.session.result.ResultProcessingException
import scala.collection.JavaConversions._

/**
 * @since 9/23/15
 */
class PlayerDatabaseModelPluginSpec extends BaseDatabaseSpec {
  "The Player Model Plugin" must {
    val plugin = new PlayerDatabaseModelPlugin(config)
    val database = new Database(config,Set[DatabaseModelPlugin](plugin))

    "Save a new player" in {
      val p1 = Player("foo","bar")
      database.neo4jSession.save(p1)
      val p1FromDB = database.neo4jSession.queryForObject(classOf[Player],"MATCH (player:Player) WHERE player.name = 'foo' RETURN player",Map[String,AnyRef]())
      p1.name must equal(p1FromDB.name)
      p1.password must equal(p1FromDB.password)
      p1.id must not be(null)
      p1.id must equal (p1FromDB.id)
    }

    "Not save a player with the same name as one that already exists" in {
      val p1 = Player("foo","bar")
      val thrown: ResultProcessingException = the [ResultProcessingException] thrownBy database.neo4jSession.save(p1)
      val thrownDetail = extractExceptionDetail(thrown)
      thrownDetail.head.message must endWith("already exists with label Player and property \"name\"=[foo]")
    }
  }
}
