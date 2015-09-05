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

import net.muxserver.krakenmush.server.support.JsonToString

import scala.util.matching.Regex

/**
 * @since 9/2/15
 */
trait Command extends JsonToString {
  val name: String

  def valid: Boolean

  def canHandle(command: ParsedCommand): Boolean
}


case class ParsedCommand(prefix: Option[String], command: String, switch: Option[String], args: Option[String])

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
    ParsedCommand(prefix, cmd, switch, args)
  }

  implicit def stringToOption(string: String): Option[String] = {
    string match {
      case "" => None
      case s: String => Option(s)
      case _ => None
    }
  }
}

sealed trait CoreCommand extends Command

object CoreCommands {

}

object LoginCommand {

}

class LoginCommand extends CoreCommand {
  val name         : String        = "login"
  val aliases      : Array[String] = Array("connect", "con", "log")
  val commandParser: CommandParser = StandardCommandParser
  var parsed       : String        = _
  var raw          : String        = _
  var valid        : Boolean       = false

  def canHandle(command: ParsedCommand): Boolean = { false }

}
