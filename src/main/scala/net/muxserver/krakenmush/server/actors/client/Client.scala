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

package net.muxserver.krakenmush.server.actors.client

import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID

import akka.actor.ActorRef

/**
 * @since 9/2/15
 */
object Client {
  def apply(clientHandler: ActorRef, remoteAddress: InetSocketAddress) = new Client(clientHandler, remoteAddress)
}

class Client(val clientHandler: ActorRef, val remoteAddress: InetSocketAddress) {

  val id            = UUID.randomUUID().toString
  val connectedAt   = Instant.now()
  var lastActiveAt  = Instant.now()
  var bytesReceived = 0L


  def remoteIP = remoteAddress.getHostName

  def remotePort = remoteAddress.getPort

  def update(byteCount: Int = 0): Unit = {
    if (byteCount > 0) {
      lastActiveAt = Instant.now()
      bytesReceived += byteCount
    }
  }
}
