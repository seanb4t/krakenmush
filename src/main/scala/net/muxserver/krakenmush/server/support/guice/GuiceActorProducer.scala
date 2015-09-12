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

import akka.actor.{Actor, IndirectActorProducer}
import com.google.inject.name.Names
import com.google.inject.{Injector, Key}

/**
 * @since 9/12/15
 */
class GuiceActorProducer(val injector: Injector, val actorName: String) extends IndirectActorProducer {
  override def produce(): Actor = injector.getBinding(Key.get(classOf[Actor], Names.named(actorName))).getProvider.get()

  override def actorClass: Class[_ <: Actor] = classOf[Actor]
}
