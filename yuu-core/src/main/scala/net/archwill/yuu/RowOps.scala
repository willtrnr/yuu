package net.archwill.yuu

import org.apache.poi.ss.usermodel.Row

final class RowOps(val self: Row) extends AnyVal {
  def as[A](implicit rr: RowReader[A]): ReadResult[A] = rr.read(self)
  def asOpt[A](implicit rr: RowReader[A]): Option[A] = rr.read(self).toOption
  def asEither[A](implicit rr: RowReader[A]): Either[Seq[String], A] = rr.read(self).toEither
}

trait ToRowOps {
  implicit def toRowOps(a: Row): RowOps = new RowOps(a)
}
