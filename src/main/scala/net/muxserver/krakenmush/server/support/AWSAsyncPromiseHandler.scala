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

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.concurrent.{Future, Promise}

/**
 * @since 9/21/15
 */
class AWSAsyncPromiseHandler[R <: AmazonWebServiceRequest, T](promise: Promise[T]) extends AsyncHandler[R, T] {
  override def onError(exception: Exception): Unit = promise failure exception

  override def onSuccess(request: R, result: T): Unit = promise success result
}

object AWSAsyncPromiseHandler {
  def awsToScala[R <: AmazonWebServiceRequest, T](fn: (R, AsyncHandler[R, T]) => java.util.concurrent.Future[T]): (R) => Future[T] = {
    req =>
      val p = Promise[T]
      fn(req, new AWSAsyncPromiseHandler(p))
      p.future
  }
}
