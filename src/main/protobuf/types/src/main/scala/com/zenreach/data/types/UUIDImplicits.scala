package com.zenreach.data.types

import java.util.UUID
import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.TypeMapper
import com.twitter.bijection.Bijection

trait UUIDImplicits {

  implicit object UUIDTypeStringMapper extends TypeMapper[String, UUID] {
    override def toCustom(base: String): UUID =
      base match {
        case id if base.length > 0 => UUID.fromString(base)
        case _ => new UUID(0, 0)
      }

    override def toBase(custom: UUID): String =
      custom.toString()
  }

  implicit object UUIDTypeByteMapper
      extends TypeMapper[ByteString, UUID] with Bijection[ByteString, UUID] {
    override def toCustom(base: ByteString): UUID =
      base match {
        case id if base.size() > 0 => {
          val buffer = ByteBuffer.wrap(base.toByteArray)
          val first = buffer.getLong()
          val second = buffer.getLong()
          new UUID(first, second)
        }
        case _ => new UUID(0, 0)
      }

    override def toBase(custom: UUID): ByteString = {
      val buffer = ByteBuffer.allocate(16)

      buffer.putLong(custom.getMostSignificantBits)
      buffer.putLong(custom.getLeastSignificantBits)

      ByteString.copyFrom(buffer.array())
    }

    override def apply(a: ByteString): UUID = toCustom(a)
  }
}
