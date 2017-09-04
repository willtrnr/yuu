package net.archwill.yuu

object YuuTest extends App {

  import RowReader._

  case class Dummy(id: Long, name: String, qty: Double)

  implicit val dummyReader: RowReader[Dummy] = for {
    id <- long("A")
    name <- str("B")
    qty <- double("C")
  } yield Dummy(id, name, qty)

  val wbkIO: WorkbookIO[ReadResult[Dummy]] = for {
    s <- WorkbookIO.getSheet("Sheet1")
    r <- WorkbookIO.pure(s.getRow(1))
    d <- WorkbookIO.pure(r.as[Dummy])
  } yield d

}
