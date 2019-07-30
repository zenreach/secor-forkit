package com.zenreach.data.types

import java.util.UUID
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class UUIDImplicitsSpec extends WordSpec with MustMatchers {
  "an empty string" should {
    "convert to the default UUID" in {
      val typeMapper = ProtobufConversions.UUIDTypeStringMapper
      val base = ""
      val expectedUUID = new UUID(0, 0)

      typeMapper.toCustom(base) mustEqual expectedUUID
    }
  }
  "an invalid UUID" should {
    "throw an exception" in {
      val typeMapper = ProtobufConversions.UUIDTypeStringMapper
      val base = "X-X"

      intercept[java.lang.IllegalArgumentException] {
        typeMapper.toCustom(base)
      }
    }
  }
  "a valid UUID" should {
    "be parsed correctly" in {
      val typeMapper = ProtobufConversions.UUIDTypeStringMapper
      val base = "51d8e623-b052-4ef4-912f-3a7244365487"
      val expectedUUID = UUID.fromString(base)

      typeMapper.toCustom(base) mustEqual expectedUUID
    }
    "be convertable to a string" in {
      val typeMapper = ProtobufConversions.UUIDTypeStringMapper
      val uuid = UUID.fromString("51d8e623-b052-4ef4-912f-3a7244365487")
      val expectedValue = uuid.toString()

      typeMapper.toBase(uuid) mustEqual expectedValue
    }
  }
}
