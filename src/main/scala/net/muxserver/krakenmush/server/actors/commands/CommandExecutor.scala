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
import kadai.config.Configuration
import net.muxserver.krakenmush.server.ClusterComms
import net.muxserver.krakenmush.server.actors.commands.CommandExecutorProtocol._
import net.muxserver.krakenmush.server.commands._

object CommandExecutor {
  def name = "CommandExecutor"

  def apply(config: Configuration) = Props(new CommandExecutor(config))
}

object CommandExecutorProtocol {

  sealed trait CommandExecutorOperation {
    def context: CommandExecutionContext
  }

  case class ExecuteRawCommand(context: CommandExecutionContext, rawCommandText: String) extends CommandExecutorOperation

  case class ExecuteParsedCommand(context: CommandExecutionContext, parsedCommand: ParsedCommand) extends CommandExecutorOperation

  sealed trait CommandExecutionResult {
    def context: CommandExecutionContext

    def command: ParsedCommand
  }

  case class CommandSuccess(context: CommandExecutionContext, command: ParsedCommand, data: Option[String]) extends CommandExecutionResult

  case class CommandFailed(context: CommandExecutionContext, command: ParsedCommand, message: Option[String] = None)
    extends CommandExecutionResult

}

trait CommandExecutorProducer {
  def newCommandExecutor(config: Configuration)(implicit context: ActorContext): ActorRef = context
    .actorOf(CommandExecutor(config), CommandExecutor.name)
}

/**
 * @since 9/4/15
 */
class CommandExecutor(val config: Configuration) extends Actor with ActorLogging with ClusterComms {


  override def preStart() = {
    log.info("Registering with cluster mediator: {}", self.path.toStringWithoutAddress)
    mediator ! Put(self)
  }

  def receive = {
    case ExecuteParsedCommand(executionContext, cmd) =>
      log.debug("Received command: {}", cmd)
      val result = execute(executionContext, cmd)
      log.debug("Result: {}", result)
      sender ! result
    case ExecuteRawCommand(executionContext, unparsedCommand) =>
      log.debug("Received unparsed command: {}", unparsedCommand)
      val cmd = StandardCommandParser.parse(unparsedCommand)
      val result = execute(executionContext, cmd)
      log.debug("Result: {}", result)
      sender ! result
  }

  private def execute(context: CommandExecutionContext, command: ParsedCommand): CommandExecutionResult = {
    log.debug("Command execution request: {} for {}", command, sender)
    dispatch(context, command) match {
      case Some(c: Command) =>
        val validationResult = c.validate(context, command)
        if (validationResult.valid) {
          CommandSuccess(context, command, c.execute(context, command).data)
        } else {
          CommandFailed(context, command, validationResult.message)
        }
      case None => CommandFailed(context, command)
    }
  }

  private def dispatch(context: CommandExecutionContext, cmd: ParsedCommand): Option[Command] = {
    log.debug("Dispatching: {}", cmd)
    log.debug("Core command list: {}", CoreCommands.commandList)
    CoreCommands.commandList.find(_.canHandle(context, cmd))
  }
}

