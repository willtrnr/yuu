package net.archwill.yuu

sealed trait CellValue {
  def as[A](implicit cr: CellReader[A]): ReadResult[A] = cr.read(this)
  def asOpt[A](implicit cr: CellReader[A]): Option[A] = cr.read(this).toOption
  def asEither[A](implicit cr: CellReader[A]): Either[Seq[String], A] = cr.read(this).toEither
}

case class CellString(value: String) extends CellValue
case class CellNumeric(value: Double) extends CellValue
case class CellBoolean(value: Boolean) extends CellValue
case class CellError(value: Byte) extends CellValue
case object CellBlank extends CellValue
