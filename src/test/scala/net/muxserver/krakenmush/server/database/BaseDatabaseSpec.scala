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
import net.muxserver.krakenmush.server.database.OGMSupport.ResultProcessingExceptionDetail
import net.muxserver.krakenmush.server.support.JsonSupport
import org.neo4j.ogm.session.result.ResultProcessingException
import org.scalatest.concurrent.IntegrationPatience
import org.json4s._
import org.json4s.jackson.JsonMethods._



/**
 * @since 9/23/15
 */
trait BaseDatabaseSpec extends BaseSpec with IntegrationPatience {
  val config = Configuration.load("application-test.conf")
  import OGMSupport._

  def extractExceptionDetail(ex:ResultProcessingException): List[ResultProcessingExceptionDetail] = {
    implicit val formats = JsonSupport.formats
    val msg = ex.getMessage
    val json = if ( msg.startsWith("{") ) parse(msg) else parse(s"{$msg") //XXX: STUPID HACK DUE TO OGM MSG HANDLING.
    val result: List[ResultProcessingExceptionDetail] = (json \ "errors").extract[List[ResultProcessingExceptionDetail]]
    result
  }

}

object OGMSupport {
  case class ResultProcessingExceptionDetail(code: String, message: String)
}
