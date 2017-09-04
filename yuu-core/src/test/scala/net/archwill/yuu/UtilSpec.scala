package net.archwill.yuu

import org.scalatest.{OptionValues, Matchers, WordSpec}

class UtilSpec extends WordSpec with Matchers with OptionValues {

  "A column to index utility" should {

    "parse textual column references" in {
      Util.columnToIndex("A").value shouldBe 0
      Util.columnToIndex("Z").value shouldBe 25
      Util.columnToIndex("AA").value shouldBe 26
      Util.columnToIndex("AZ").value shouldBe 51
      Util.columnToIndex("ABC").value shouldBe 730
    }

    "fail for invalid references" in {
      Util.columnToIndex("") shouldBe 'empty
      Util.columnToIndex("a") shouldBe 'empty
      Util.columnToIndex("1") shouldBe 'empty
    }

  }

  "An index to column utility" should {

    "convert indices to textual column references" in {
      Util.indexToColumn(0).value shouldBe "A"
      Util.indexToColumn(25).value shouldBe "Z"
      Util.indexToColumn(26).value shouldBe "AA"
      Util.indexToColumn(51).value shouldBe "AZ"
      Util.indexToColumn(730).value shouldBe "ABC"
    }

    "fail for invalid indices" in {
      Util.indexToColumn(-1) shouldBe 'empty
    }

  }

}
