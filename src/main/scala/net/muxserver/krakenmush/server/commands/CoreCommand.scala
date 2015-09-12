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

package net.muxserver.krakenmush.server.commands

import java.util.ServiceLoader

import com.typesafe.scalalogging.StrictLogging
import net.muxserver.krakenmush.server.commands.Command._

import scala.collection.JavaConverters._

/**
 * @since 9/11/15
 */
sealed trait CoreCommand extends Command

sealed trait CommandRequiringLoggedOutClient extends Command {
  abstract override def canHandle(context: CommandExecutionContext, command: ParsedCommand): Boolean = {
    (!context.client.isLoggedIn) && super.canHandle(context, command)
  }
}

sealed trait CommandRequiringLoggedInClient extends Command {
  abstract override def canHandle(context: CommandExecutionContext, command: ParsedCommand): Boolean = {
    context.client.isLoggedIn && super.canHandle(context, command)
  }
}

object CoreCommands extends StrictLogging {
  private val coreCommandLoader: ServiceLoader[CoreCommand] = ServiceLoader.load(classOf[CoreCommand])
  val commandList: Seq[Command] = coreCommandLoader.asScala.toSeq
  commandList.foreach { c =>
    logger.debug("CoreCommandLoaded: {}", c)
  }
  val commandMap: Map[String, Command] = Map(commandList.map(c => c.name -> c): _*)
  logger.debug("CommandMap: {}", commandMap)
}

class LoginCommand extends CoreCommand with CommandRequiringLoggedOutClient {
  val name: String = "login"
  override val aliases: Array[String] = Array("connect", "con")


  override def validate(context: CommandExecutionContext, command: ParsedCommand): CommandValidationResult = {
    command.args match {
      case Some(s: String) if s.split(' ').length == 2 => true
      case _ => (false, Option("Invalid input."))
    }
  }

  override def execute(context: CommandExecutionContext, command: ParsedCommand): CommandOutput = {
    CommandOutput(Option(s"ECHO: $command"))
  }
}

class LogoutCommand extends CoreCommand with CommandRequiringLoggedInClient {
  val name = "logout"
  override val aliases = Array("q", "quit", "bye")

  override def execute(context: CommandExecutionContext, command: ParsedCommand): CommandOutput = {
    CommandOutput(Option(s"ECHO: $command"))
  }

}


