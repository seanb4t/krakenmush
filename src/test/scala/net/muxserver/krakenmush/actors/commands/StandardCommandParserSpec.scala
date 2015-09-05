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

package net.muxserver.krakenmush.actors.commands

import net.muxserver.krakenmush.BaseSpec
import net.muxserver.krakenmush.server.actors.commands.StandardCommandParser

/**
 * @since 9/4/15
 */
class StandardCommandParserSpec extends BaseSpec {

  "The StandardCommandParser" must {
    "parse raw commands into ParsedCommands" in {
      var cmd = StandardCommandParser.parse("connect foo bar")
      cmd.command must be("connect")
      cmd.prefix must be(None)
      cmd.switch must be(None)
      cmd.args must be(Some("foo bar"))

      cmd = StandardCommandParser.parse("look/here")
      cmd.command must be("look")
      cmd.prefix must be(None)
      cmd.switch must be(Some("here"))
    }
  }
}
