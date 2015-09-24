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

package net.muxserver.krakenmush.server.support.neo4j

import java.time.Instant

import org.neo4j.ogm.typeconversion.AttributeConverter

/**
 * @since 9/22/15
 */
object OGMConverters {
  class InstantToLongConverter extends AttributeConverter[Instant,java.lang.Long] {
    override def toEntityAttribute(value: java.lang.Long): Instant = Instant.ofEpochMilli(value)

    override def toGraphProperty(value: Instant): java.lang.Long = value.toEpochMilli
  }

}
