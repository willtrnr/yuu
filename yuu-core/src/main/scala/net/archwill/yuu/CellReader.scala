package net.archwill.yuu

trait CellReader[A] { self =>

  def read(cell: CellValue): ReadResult[A]

  def map[B](f: A => B): CellReader[B] =
    CellReader[B] { cell => self.read(cell).map(f) }

  def flatMap[B](f: A => CellReader[B]): CellReader[B] =
    CellReader[B] { cell => self.read(cell).flatMap(a => f(a).read(cell)) }

  def filter(error: => String)(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filter(p) orElse ReadError(Seq(error)) }

  def filter(p: A => Boolean): CellReader[A] =
    filter("Did not match filter")(p)

  def filterNot(error: => String)(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filterNot(p) orElse ReadError(Seq(error)) }

  def filterNot(p: A => Boolean): CellReader[A] =
    filterNot("Did not match filter")(p)

  def collect[B](error: => String)(pf: PartialFunction[A, B]): CellReader[B] = CellReader[B] { cell =>
    self.read(cell) flatMap {
      case a if pf.isDefinedAt(a) => ReadSuccess(pf(a))
      case _ => ReadError(Seq(error))
    }
  }

  def collect[B](pf: PartialFunction[A, B]): CellReader[B] =
    collect("Did not match function")(pf)

  def orElse(other: => CellReader[A]): CellReader[A] = CellReader[A] { cell =>
    self.read(cell) orElse other.read(cell)
  }

  def compose[B <: CellValue](fb: CellReader[B]): CellReader[A] =
    CellReader[A] { cell => fb.read(cell).flatMap(b => self.read(b)) }

  def andThen[B](fb: CellReader[B])(implicit ev: A <:< CellValue): CellReader[B] =
    fb.compose(this.map(ev))

}

object CellReader {

  import ReadResult._

  def apply[A](f: CellValue => ReadResult[A]): CellReader[A] = new CellReader[A] {
    def read(cell: CellValue): ReadResult[A] = f(cell)
  }

  def pure[A](v: A): CellReader[A] = apply { _ => success(v) }

  @inline def of[A](implicit c: CellReader[A]): CellReader[A] = c
  
  implicit val byteCellReader: CellReader[Byte] = apply {
    case CellNumeric(d) if d.isValidByte => success(d.toByte)
    case _: CellNumeric => error("Not a valid byte")
    case _ => error("Expected numeric type")
  }

  implicit val shortCellReader: CellReader[Short] = apply {
    case CellNumeric(d) if d.isValidShort => success(d.toShort)
    case _: CellNumeric => error("Not a valid short")
    case _ => error("Expected numeric type")
  }

  implicit val intCellReader: CellReader[Int] = apply {
    case CellNumeric(d) if d.isValidInt => success(d.toInt)
    case _: CellNumeric => error("Not a valid integer")
    case _ => error("Expected numeric type")
  }

  implicit val longCellReader: CellReader[Long] = apply {
    case CellNumeric(d) if d.isWhole => success(d.toLong)
    case _: CellNumeric => error("Not a valid long")
    case _ => error("Expected numeric type")
  }

  implicit val floatCellReader: CellReader[Float] = apply {
    case CellNumeric(d) => success(d.toFloat)
    case _ => error("Expected numeric type")
  }

  implicit val doubleCellReader: CellReader[Double] = apply {
    case CellNumeric(d) => success(d)
    case _ => error("Expected numeric type")
  }

  implicit val booleanCellReader: CellReader[Boolean] = apply {
    case CellBoolean(b) => success(b)
    case _ => error("Expected boolean type")
  }

  implicit val stringCellReader: CellReader[String] = apply {
    case CellString(s) => success(s)
    case _ => error("Expected string type")
  }

  implicit def optionCellReader[A](implicit cr: CellReader[A]): CellReader[Option[A]] = apply {
    case CellBlank => success(None)
    case c => cr.read(c).map(Option(_))
  }

}
