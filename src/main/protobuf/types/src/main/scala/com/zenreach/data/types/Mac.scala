package com.zenreach.data.types

import java.nio.ByteBuffer
import java.util

import com.google.protobuf.ByteString
import com.trueaccord.scalapb.TypeMapper
import com.twitter.bijection.Bijection
import scodec.bits.{ByteOrdering, ByteVector}

case class HexString(s: String) {
  def hex: Array[Byte] =
    HexString
      .takeOnlyHex(s)
      .grouped(2) map { Integer.parseInt(_, 16).toByte } toArray
}

trait HexImplicits {
  implicit def stringToHexString(s: String): HexString = HexString(s)
}

object HexString extends HexImplicits {
  def takeOnlyHex(s: String): String = s.replaceAll("[^a-fA-F0-9]", "")
}

case class Mac(bytes: Array[Byte]) {
  def hexStr(delim: String = ":") = Mac.bytesToByteMethods(bytes).hexStr(delim)
  def str: String = hexStr(":")
  def strUpperWithColon: String = hexStr(":").toUpperCase
  def strUpperNoColon: String = hexStr("").toUpperCase
  def routerFormat: String = strUpperNoColon

  // Locally administered MACs are not real and should be ignored (except for testing purposes)
  def isLocal: Boolean = (bytes(0) & (1.toByte << 1)) != 0
  def isGlobal: Boolean = !isLocal
  // Any Client MAC starting with 123 is reserved for internal testing and should be allowed through filters
  // Related: platform/common/netutil/mac.go
  def hasTestPrefix: Boolean = (bytes(0) == 0x12.toByte) && ((bytes(1) & (0xf0.toByte)) == 0x30.toByte)

  override def toString: String = s"Mac($str)"

  override def equals(o: Any): Boolean = o match {
    case m: Mac =>
      util.Arrays.equals(bytes, m.bytes)
    case b: Array[Byte] => util.Arrays.equals(bytes, b)
    case s: String => util.Arrays.equals(bytes, Mac(s).bytes)
    case other => other.equals(bytes)
  }
}

trait MacImplicits {
  implicit def stringToMacString(s: String): Mac = Mac(HexString(s).hex)
  implicit def macByteStringtoString(bs: ByteString): Array[Byte] = bs.toByteArray
  implicit def macStringToString(m: Mac): String = m.str
  implicit def macToBytes(m: Mac): Array[Byte] = m.bytes
  implicit val macBytesBij = Bijection.build[Mac, Array[Byte]](macToBytes)(Mac.apply)

  implicit object MacTypeMapper extends TypeMapper[ByteString, Mac] {

    override def toCustom(base: ByteString): Mac =
      if (base.length == 6)
        new Mac(base.toByteArray)
      else Mac(new String(base.toByteArray))

    override def toBase(custom: Mac): ByteString =
      ByteString.copyFrom(ByteBuffer.wrap(custom.bytes))
  }

  implicit object MacTypeMapperLong extends TypeMapper[Long, Mac] {
    override def toCustom(base: Long): Mac =
      Mac(ByteVector.fromLong(base, 6, ByteOrdering.BigEndian).toArray)

    override def toBase(custom: Mac): Long =
      ByteVector(custom.bytes).toLong(signed = true, ByteOrdering.BigEndian)
  }
}

object Mac extends MacImplicits {
  def formatMacAddr(input: String) = input.str
  def apply(s: String): Mac = s

  implicit def bytesToByteMethods(bytes: Array[Byte]): ByteMethods =
    ByteMethods(bytes)

  implicit class ByteMethods(bytes: Array[Byte]) {
    def hexStr = bytes map { "%02x" format _ } mkString
    def hexStr(sep: String = "") =
      bytes map { "%02x" format _ } mkString sep
  }
}
