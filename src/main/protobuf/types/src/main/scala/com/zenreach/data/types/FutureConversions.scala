package com.zenreach.data.types

import com.twitter.bijection.twitter_util.UtilBijections
import com.twitter.util.Future
import com.twitter.bijection.Conversion._

import scala.concurrent.ExecutionContext

trait FutureConversions extends UtilBijections {
  import scala.concurrent.{Future => ScalaFuture}
  implicit class ScalaFutureHelper[T](fut: ScalaFuture[T]) {
    def twitterFuture(implicit executionCtx: ExecutionContext): Future[T] =
      fut.as[Future[T]]
  }
}
