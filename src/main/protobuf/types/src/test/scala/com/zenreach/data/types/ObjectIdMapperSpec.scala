package com.zenreach.data.types

import org.bson.types.ObjectId
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{MustMatchers, WordSpec}

@RunWith(classOf[JUnitRunner])
class ObjectIdMapperSpec extends WordSpec with MustMatchers {
  "an object id" should {
    "convert to and from byte arrays" in {
      val oid = new ObjectId()
      new ObjectId(oid.toByteArray) mustEqual oid
    }
  }
}
