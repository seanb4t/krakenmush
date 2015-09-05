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

package net.muxserver.krakenmush.server.support

import java.text.Normalizer

/**
 * @since 9/2/15
 */
trait StringProcessingSupport {

  def normalize(input: String): String = {
    Normalizer.normalize(input.trim, Normalizer.Form.NFD).replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
  }

  def stripNonPrintables(input: String): String = {
    input.replaceAll( """\p{Cc}""", "").replaceAll( """\r\n""", "\n").replaceAll( """\r""", "\n")

  }

  def normalizeAndStrip(input: String): String = {
    stripNonPrintables(normalize(input)).trim
  }

}
