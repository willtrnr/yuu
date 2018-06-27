package net.archwill.yuu

import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.util.CellAddress

final class SheetOps(val self: Sheet) extends AnyVal {
  def as[A](implicit sr: SheetReader[A]): ReadResult[A] = sr.read(self)
  def asOpt[A](implicit sr: SheetReader[A]): Option[A] = sr.read(self).toOption
  def asEither[A](implicit sr: SheetReader[A]): Either[Seq[(String, Option[CellAddress])], A] = sr.read(self).toEither
}

trait ToSheetOps {
  implicit def toSheetOps(a: Sheet): SheetOps = new SheetOps(a)
}
