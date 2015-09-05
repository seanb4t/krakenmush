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

package net.muxserver.krakenmush.server.actors.commands

import akka.actor._
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProtocol.{CommandExecutionResult, ExecuteParsedCommand,
ExecuteRawCommand}

object CommandExecutor {
  def apply() = Props(new CommandExecutor())
}

object CommandExecutorProtocol {

  sealed trait CommandExecutorOperation

  case class ExecuteRawCommand(rawCommandText: String)

  case class ExecuteParsedCommand(parsedCommand: ParsedCommand)

  case class CommandExecutionResult(data: Option[String], originalCommand: ParsedCommand)

}

trait CommandExecutorProducer {
  def newCommandExecutor()(implicit context: ActorContext): ActorRef = context.actorOf(CommandExecutor(), "CommandExecutor")
}

/**
 * @since 9/4/15
 */
class CommandExecutor extends Actor with ActorLogging {

  def receive = {
    case ExecuteParsedCommand(cmd) =>
      log.debug("Received command: {}", cmd)
      sender ! execute(cmd)
    case ExecuteRawCommand(unparsedCommand) =>
      log.debug("Received unparsed command: {}", unparsedCommand)
      val cmd = StandardCommandParser.parse(unparsedCommand)
      sender ! execute(cmd)
  }

  private def execute(command: ParsedCommand): CommandExecutionResult = {
    log.debug("Command execution request: {} for {}", command, sender)
    CommandExecutionResult(None, command)
  }
}

