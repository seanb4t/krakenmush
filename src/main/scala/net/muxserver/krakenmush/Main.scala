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

package net.muxserver.krakenmush

import akka.actor.{ActorRef, ActorSystem, Inbox}
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.util.Timeout
import com.typesafe.scalalogging.StrictLogging
import net.muxserver.krakenmush.server.actors.coreserver.CoreServerProtocol.{Start, Started}
import net.muxserver.krakenmush.server.actors.coreserver.{ClusterMemberListener, CoreServer}
import net.muxserver.krakenmush.server.support.spring.SpringExtension
import net.muxserver.krakenmush.server.{CoreClusterTopics, ServerApplicationConfiguration}
import org.springframework.context.annotation.AnnotationConfigApplicationContext

import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * @since 8/29/2015
 */
object Main extends StrictLogging {

  def subscribeToTopic(inbox: Inbox, mediator: ActorRef, topic: String)(implicit timeout: Timeout): Unit = {
    val subRequest = Subscribe(topic, inbox.getRef())
    inbox.send(mediator, subRequest)

    inbox.receive(timeout.duration) match {
      case SubscribeAck(s) => logger.info("Subscribed to topic: {}", s.topic)
      case x: AnyRef => logger.warn("Received unknown message: {}", x)
    }

  }

  def main(args: Array[String]): Unit = {
    implicit val ctx = new AnnotationConfigApplicationContext(classOf[ServerApplicationConfiguration])

    val actorSystem = ctx.getBean(classOf[ActorSystem])
    val clusterMemberListener = actorSystem.actorOf(ClusterMemberListener.props())

    val inbox = Inbox.create(actorSystem)
    val coreServer = actorSystem.actorOf(SpringExtension(actorSystem).props(CoreServer.name), CoreServer.name)
    logger.info("Bootstrapping CoreServer at path: {}", coreServer.path.toStringWithoutAddress)
    import akka.cluster.pubsub.DistributedPubSub
    val mediator = DistributedPubSub(actorSystem).mediator

    implicit val timeout = Timeout(30 seconds)

    Seq(
      CoreClusterTopics.SERVER_STATUS,
      CoreClusterTopics.SYSTEM_STATUS,
      CoreClusterTopics.CONNECTION_INFO
    ).sorted.foreach(subscribeToTopic(inbox, mediator, _))

    inbox.send(coreServer, Start)

    inbox.receive(timeout.duration) match {

      case Started => logger.info("Core Server is starting up, everything's in it's hands now.")

      case x: AnyRef =>
        logger.warn("Unknown response to start request: {}", x)
        Await.result(actorSystem.terminate(), 30 seconds)
    }

  }
}
