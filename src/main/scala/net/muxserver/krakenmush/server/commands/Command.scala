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

import net.muxserver.krakenmush.server.actors.client.Client
import net.muxserver.krakenmush.server.support.JsonToString

import scala.language.implicitConversions
import scala.util.matching.Regex

/**
 * @since 9/2/15
 */
object Command {
  implicit def booleanToValidationResult(bool: Boolean): CommandValidationResult = CommandValidationResult(bool, None)

  implicit def tuple2ToValidationResult(tup: (Boolean, Option[String])): CommandValidationResult = CommandValidationResult(tup._1, tup._2)
}

trait Command extends {

  import Command._

  val name: String

  val aliases: Array[String] = Array()

  val commandParser: CommandParser = StandardCommandParser

  def commandPattern: Regex = s"^((?i)${(name +: aliases).map(Regex.quote).mkString("|")})".r

  def canHandle(context: CommandExecutionContext, command: ParsedCommand): Boolean = {
    commandPattern.findFirstIn(command.command).isDefined
  }

  def validate(context: CommandExecutionContext, command: ParsedCommand): CommandValidationResult = { true }

  def execute(context: CommandExecutionContext, command: ParsedCommand): CommandOutput

  override def toString: String = {
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods._
    val output = ("name" -> name) ~ ("aliases" -> aliases.toSeq) ~ ("commandParser" -> commandParser.getClass.toString)
    compact(render(output))
  }

}

sealed case class CommandExecutionContext(client: Client)

sealed case class CommandOutput(data: Option[String])

sealed case class CommandValidationResult(valid: Boolean, message: Option[String])

case class ParsedCommand(prefix: Option[String], command: String, switch: Option[String], args: Option[String], raw: String)
  extends JsonToString

trait CommandParser {
  def parse(rawCommand: String): ParsedCommand
}

object StandardCommandParser extends CommandParser {

  /**
   * Standard pattern that captures _prefix__command__/_switch_ _args_
   *
   * look/here
   * jump
   * connect foo bar
   * \@tel
   */
  val standardParsePattern = new Regex(
    """^([\\\/+=@&]?)([^\s\d\/]+)(\/[^\s]+)?(.+)*$""",
    "prefix", "cmd", "switch", "args")

  def parse(rawCommand: String): ParsedCommand = {
    var standardParsePattern(prefix, cmd, switch, args) = rawCommand
    if (args != null) args = args.trim
    if (switch != null) switch = switch.substring(1)
    ParsedCommand(prefix, cmd, switch, args, rawCommand)
  }

  implicit def stringToOption(string: String): Option[String] = {
    string match {
      case "" => None
      case s: String => Option(s)
      case _ => None
    }
  }
}


