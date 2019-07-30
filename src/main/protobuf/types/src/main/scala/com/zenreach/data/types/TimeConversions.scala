package com.zenreach.data.types

import java.text.SimpleDateFormat
import java.time.{Instant, Duration => JDuration}
import java.util.Calendar

import com.google.protobuf.timestamp.Timestamp
import com.trueaccord.scalapb.TypeMapper
import com.twitter.bijection.{Bijection, Injection, InversionFailure}
import com.twitter.util.{Duration, Time}

trait TimeConversions {

  implicit def longToTime(l: Long): Time =
    Time.fromMilliseconds(l)

  implicit def timeToLong: Time => Long = _.inMillis

  implicit def timestampToString(ts: Timestamp): String = {
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
    val instance = Calendar.getInstance()
    instance.setTimeInMillis(ts.seconds * 1000L)
    sdf.format(instance.getTime)
  }

  implicit val tsToStr: Injection[Timestamp, String] =
    Injection.build[Timestamp, String](timestampToString)(
      InversionFailure.failedAttempt
    )

  implicit val timeToInstant: Bijection[Time, Instant] =
    Bijection.build[Time, Instant](t => Instant.ofEpochMilli(t.inMillis))(
      i => Time.fromMilliseconds(i.toEpochMilli)
    )

  implicit def longTimeMapper =
    TypeMapper[Long, Time](longToTime)(timeToLong)

  implicit def timestampInstantMapper =
    TypeMapper[Timestamp, Instant](ts => Instant.ofEpochSecond(ts.seconds, ts.nanos))(
      i => Timestamp(i.getEpochSecond, i.getNano)
    )

  implicit def longInstantMapper =
    TypeMapper[Long, Instant](Instant.ofEpochMilli)(_.toEpochMilli)

  implicit val instantOrdering: Ordering[Instant] = new Ordering[Instant] {
    override def compare(x: Instant, y: Instant): Int = x.compareTo(y)
  }

  implicit def durationToJDuration(dur: Duration): JDuration = JDuration.ofMillis(dur.inMillis)

  implicit def jdurToDuration(jdur: JDuration): Duration = Duration.fromMilliseconds(jdur.toMillis)
}

object TimeConversions extends TimeConversions
