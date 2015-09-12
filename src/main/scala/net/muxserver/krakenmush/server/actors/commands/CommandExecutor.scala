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
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import com.typesafe.config.Config
import net.muxserver.krakenmush.server.ClusterComms
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProtocol._
import net.muxserver.krakenmush.server.commands.{Command, CoreCommands, ParsedCommand, StandardCommandParser}

object CommandExecutor {
  def name = "CommandExecutor"

  def apply(config: Config) = Props(new CommandExecutor(config))
}

object CommandExecutorProtocol {

  sealed trait CommandExecutorOperation

  case class ExecuteRawCommand(rawCommandText: String) extends CommandExecutorOperation

  case class ExecuteParsedCommand(parsedCommand: ParsedCommand) extends CommandExecutorOperation

  sealed trait CommandExecutionResult

  case class CommandSuccess(data: Option[String], originalCommand: ParsedCommand) extends CommandExecutionResult

  case class CommandFailed(originalCommand: ParsedCommand) extends CommandExecutionResult

}

trait CommandExecutorProducer {
  def newCommandExecutor(config: Config)(implicit context: ActorContext): ActorRef = context
    .actorOf(CommandExecutor(config), CommandExecutor.name)
}

/**
 * @since 9/4/15
 */
class CommandExecutor(val config: Config) extends Actor with ActorLogging with ClusterComms {


  override def preStart() = {
    log.info("Registering with cluster mediator: {}", self.path.toStringWithoutAddress)
    mediator ! Put(self)
  }

  def receive = {
    case ExecuteParsedCommand(cmd) =>
      log.debug("Received command: {}", cmd)
      val result = execute(cmd)
      log.debug("Result: {}", result)
      sender ! result
    case ExecuteRawCommand(unparsedCommand) =>
      log.debug("Received unparsed command: {}", unparsedCommand)
      val cmd = StandardCommandParser.parse(unparsedCommand)
      val result = execute(cmd)
      log.debug("Result: {}", result)
      sender ! result
  }

  private def execute(command: ParsedCommand): CommandExecutionResult = {
    log.debug("Command execution request: {} for {}", command, sender)
    dispatch(command) match {
      case Some(c: Command) => CommandSuccess(Some(s"ECHO: ${command.raw}"), command)
      case None => CommandFailed(command)
    }
  }

  private def dispatch(cmd: ParsedCommand): Option[Command] = {
    log.debug("Dispatching: {}", cmd)
    log.debug("Core command list: {}", CoreCommands.commandList)
    CoreCommands.commandList.find(_.canHandle(cmd))
  }
}

