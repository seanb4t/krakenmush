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

package net.muxserver.krakenmush.server.actors.coreserver

import akka.actor.{Actor, ActorLogging, Address, Props}
import akka.cluster.ClusterEvent.{CurrentClusterState, MemberEvent, MemberRemoved, MemberUp}
import akka.cluster.{Cluster, MemberStatus}
import net.muxserver.krakenmush.server.ClusterComms

object ClusterMemberListener {
  def props() = Props(new ClusterMemberListener)
}

/**
 * @since 9/6/15
 */
class ClusterMemberListener extends Actor with ActorLogging with ClusterComms {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberEvent])

  override def postStop(): Unit = cluster unsubscribe self

  var nodes = Set.empty[Address]

  def receive = {
    case state: CurrentClusterState =>
      nodes = state.members.collect {
        case m if m.status == MemberStatus.up => m.address
      }
    case MemberUp(member) =>
      nodes += member.address
      log.info("Member is Up: {}, {} nodes in cluster.", member.address, nodes.size)
    case MemberRemoved(member, _) =>
      nodes -= member.address
      log.info("Member is Removed: {}, {} nodes in cluster", member.address, nodes.size)
    case _: MemberEvent => // ignore
  }
}
