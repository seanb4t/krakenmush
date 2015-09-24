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

import com.google.inject.Inject
import io.github.nremond.SecureHash
import kadai.config.Configuration
import org.neo4j.ogm.session.Session

import scala.collection.JavaConversions._
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

/**
 * @since 9/23/15
 */
class PlayerDatabaseModelPlugin @Inject() (override val config: Configuration) extends DatabaseModelPlugin(config){
  override type T = Player

  override def init(implicit session: Session): Future[Boolean] = {
    val initialized = Promise[Boolean]()
    Try(session.query(
      """
        |CREATE CONSTRAINT ON (player:Player) ASSERT player.name is UNIQUE
      """.stripMargin, Map[String,AnyRef]())) match {
      case Success(result) =>
        logger.debug("Created player unique name constraint, results: {}",result)
        initialized.success(true)
      case Failure(ex) => initialized.failure(ex)
    }

    initialized.future
  }

  override def packageNames: Set[String] = Set(classOf[Player].getPackage.getName)

  override def modelName: String = classOf[Player].getSimpleName
}

import net.muxserver.krakenmush.server.database.model.DatabaseModelAnnotations._

class Player(name: String,
  @Property(name = "hashedPassword")
  private var _hashedPassword: String,
  @Property
  var alias: String = null
) extends DatabaseModel(name) {

  def this() = this(null,null,null)

  def password:String = _hashedPassword
  def password_= (clearTextPassword:String):Unit = _hashedPassword = Player.hashPassword(clearTextPassword)

}

object Player {

  def apply(n: String, password: String): Player = new Player(n,hashPassword(password))

  def hashPassword(clearText: String) = SecureHash.createHash(
    password = clearText,
    iterations = 100000,
    dkLength = 32,
    cryptoAlgo = "HmacSHA512"
  )
  def validatePassword(clearText: String, hashedPass: String) = SecureHash.validatePassword(
    password = clearText,
    hashedPassword = hashedPass
  )
}
