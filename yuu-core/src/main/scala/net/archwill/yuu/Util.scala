package net.archwill.yuu

object Util {

  def columnToIndex(col: String): Option[Int] = {
    @annotation.tailrec def step(s: String, a: Int): Option[Int] = {
      if (s.isEmpty) Option(a) else s(0) match {
        case c if c >= 'A' && c <= 'Z' =>
          step(s.tail, a * 26 + c.toInt - ('A'.toInt) + 1)
        case _ =>
          None
      }
    }
    if (col.isEmpty) None else step(col, 0).map(_ - 1)
  }

  def indexToColumn(idx: Int): Option[String] = {
    @annotation.tailrec def step(i: Int, a: String): String = {
      if (i == 0) a else {
        val q = (i - 1) / 26
        val r = (i - 1) % 26
        step(q, ('A' + r).toChar + a)
      }
    }
    if (idx < 0) None else Option(step(idx + 1, ""))
  }

}
