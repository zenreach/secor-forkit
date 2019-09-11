package com.zenreach.data.types

import java.nio.ByteBuffer

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.TypeMapper
import com.twitter.bijection.Bijection
import org.bson.types.ObjectId

trait ObjectIdImplicits {

  implicit def stringToOid(s: String): ObjectId = new ObjectId(s)

  implicit def bytesToOid(byteArr: Array[Byte]): ObjectId =
    new ObjectId(byteArr)

  implicit def oidToByteSeq(id: ObjectId): Array[Byte] =
    id.toByteArray

  implicit val oidBytesBij = Bijection.build[ObjectId, Array[Byte]](oidToByteSeq)(bytesToOid)

  implicit val oidByteStringBij =
    Bijection.build[ObjectId, ByteString](ObjectIdTypeMapper.toBase)(ObjectIdTypeMapper.toCustom)

  implicit object ObjectIdTypeMapper extends TypeMapper[ByteString, ObjectId] {

    override def toCustom(base: ByteString): ObjectId = {
      val buf = ByteBuffer.allocate(12)
      buf.clear()
      base.copyTo(buf)
      new ObjectId(buf.array())
    }

    override def toBase(custom: ObjectId): ByteString =
      ByteString.copyFrom(ByteBuffer.wrap(custom.toByteArray), 12)
  }
}

object ObjectIdImplicits extends ObjectIdImplicits
