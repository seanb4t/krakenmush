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
import java.util.Objects

import com.google.inject.Inject
import com.typesafe.scalalogging.StrictLogging
import kadai.config.Configuration
import net.muxserver.krakenmush.server.database.Database
import net.muxserver.krakenmush.server.support.neo4j.OGMConverters.InstantToLongConverter
import org.neo4j.ogm.annotation.{GraphId, NodeEntity}
import org.neo4j.ogm.session.Session

import scala.annotation.meta._
import scala.concurrent.Future


/**
 * @since 9/16/15
 */
abstract class DatabaseModelPlugin @Inject() (val config: Configuration) extends StrictLogging {
  type T <: DatabaseModel
  implicit val executionContext = Database.executionContext

  def modelName: String
  def packageNames: Set[String]

  def init(implicit session: Session): Future[Boolean]

}

import DatabaseModelAnnotations._
@NodeEntity
abstract class DatabaseModel(@Property var name: String) {
  import java.lang.Long

  @Id
  var id: Long = _
  @Property
  @Convert(classOf[InstantToLongConverter])
  var createdAt: Instant = Instant.now()
  @Property
  @Convert(classOf[InstantToLongConverter])
  var lastModifiedAt: Instant = Instant.now()

  def this() = this(null)

  override def hashCode(): Int = Objects.hash(id)

  override def equals(obj: scala.Any): Boolean = {
    obj match {
      case that: DatabaseModel => Objects.equals(id,that.id)
      case _ => false
    }
  }
}

class DatabaseModelReadException(msg: String, cause: Throwable = null) extends RuntimeException(msg, cause) {

}

object DatabaseModelAnnotations {
  type Id = GraphId @field
  type Convert = org.neo4j.ogm.annotation.typeconversion.Convert @field
  type Property = org.neo4j.ogm.annotation.Property @field
  type Relationship = org.neo4j.ogm.annotation.Relationship @field
}
