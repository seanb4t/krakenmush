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

package net.muxserver.krakenmush.server

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import net.muxserver.krakenmush.server.actors.commands.CommandExecutor
import net.muxserver.krakenmush.server.actors.coreserver.CoreServer


/**
 * @since 9/5/15
 */
trait ClusterComms {
  self: Actor =>

  import akka.cluster.pubsub.DistributedPubSub

  val mediator = DistributedPubSub(context.system).mediator

  def subscribe(topic: String, actorRef: ActorRef) = mediator ! Subscribe(topic, actorRef)

  def publish(topic: String, message: Any) = {
    mediator ! Publish(topic, message)
  }
}

object CoreClusterTopics {
  final val SYSTEM_STATUS   = "/status/system"
  final val SERVER_STATUS   = "/status/server"
  final val CONNECTION_INFO = s"/status/mainTCPServer/connections"
}

object CoreClusterAddresses {
  final val CORE_SERVER      = s"/user/${CoreServer.name}"
  final val TCP_SERVER_MAIN  = s"$CORE_SERVER/mainTCPServer"
  final val COMMAND_EXECUTOR = s"$CORE_SERVER/${CommandExecutor.name}"
}
