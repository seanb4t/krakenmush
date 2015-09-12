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

package net.muxserver.krakenmush.server.support.guice

import akka.actor._
import com.google.inject.Injector


/**
 * @since 9/12/15
 */
class GuiceAkkaExtension(val system: ExtendedActorSystem) extends Extension {
  private var injector: Injector = _

  def initialize(injector: Injector) { this.injector = injector }

  def props(actorName: String) = Props(classOf[GuiceActorProducer], injector, actorName)
}

object GuiceAkkaExtension extends ExtensionId[GuiceAkkaExtension] with ExtensionIdProvider {

  override def createExtension(system: ExtendedActorSystem): GuiceAkkaExtension = new GuiceAkkaExtension(system)

  override def lookup(): ExtensionId[_ <: Extension] = GuiceAkkaExtension

  override def get(system: ActorSystem): GuiceAkkaExtension = super.get(system)
}

