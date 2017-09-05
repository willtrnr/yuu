package net.archwill.yuu

object YuuTest extends App {

  import RowReader._

  case class Dummy(id: Long, name: String, qty: Double)

  implicit val dummyReader: RowReader[Dummy] = for {
    id <- long("A")
    name <- str("B")
    qty <- double("C")
  } yield Dummy(id, name, qty)

  val wbkIO: WorkbookIO[ReadResult[List[Dummy]]] = for {
    l <- WorkbookIO.withSheet("Sheet 1")(SheetIO.read[List[Dummy]])
  } yield l

}
