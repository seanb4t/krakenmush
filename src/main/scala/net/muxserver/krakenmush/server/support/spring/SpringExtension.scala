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

package net.muxserver.krakenmush.server.support.spring

import akka.actor._
import org.springframework.context.ApplicationContext

/**
 * @since 9/5/15
 */
class SpringExtension extends Extension {

  var applicationContext: ApplicationContext = _

  /**
   * Initializes the application context for the extension
   */
  def initialize(applicationContext: ApplicationContext): SpringExtension = {
    this.applicationContext = applicationContext
    this
  }

  /**
   * Create a Props for the specified actor bean name using the producer class
   */
  def props(beanName: String): Props = {
    Props(classOf[SpringActorProducer], applicationContext, beanName)
  }
}

object SpringExtension {
  def apply(system: ActorSystem)(implicit ctx: ApplicationContext): SpringExtension = SpringExt(system).initialize(ctx)
}

object SpringExt extends AbstractExtensionId[SpringExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem) = new SpringExtension()

  override def lookup() = SpringExt
}

class SpringActorProducer(ctx: ApplicationContext, actorBeanName: String) extends IndirectActorProducer {
  override def produce: Actor = ctx.getBean(actorBeanName, classOf[Actor])

  override def actorClass: Class[_ <: Actor] = ctx.getType(actorBeanName).asInstanceOf[Class[Actor]]
}
