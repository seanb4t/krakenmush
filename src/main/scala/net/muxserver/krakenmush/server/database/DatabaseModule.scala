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

import com.google.inject.AbstractModule
import net.codingwell.scalaguice.{ScalaModule, ScalaMultibinder}
import net.muxserver.krakenmush.server.database.model.{RoomDatabaseModelPlugin, PlayerDatabaseModelPlugin, DatabaseModelPlugin,
GameMetaDataDatabaseModelPlugin}

/**
 * @since 9/16/15
 */
class DatabaseModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    val sBinder = ScalaMultibinder.newSetBinder[DatabaseModelPlugin](binder)
    sBinder.addBinding.to[GameMetaDataDatabaseModelPlugin]
    sBinder.addBinding.to[PlayerDatabaseModelPlugin]
    sBinder.addBinding.to[RoomDatabaseModelPlugin]
    bind[Database].to[Database].asEagerSingleton()
  }
}
