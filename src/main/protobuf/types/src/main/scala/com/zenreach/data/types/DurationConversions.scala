package com.zenreach.data.types

import com.trueaccord.scalapb.TypeMapper
import com.twitter.util.Duration

trait DurationConversions {

  implicit def longToDuration(l: Long): Duration =
    Duration.fromMilliseconds(l)

  implicit def durationToLong: Duration => Long = _.inMillis

  implicit def longDurationMapper =
    TypeMapper[Long, Duration](longToDuration)(durationToLong)

}

object DurationConversions extends DurationConversions
