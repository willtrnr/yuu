package net.archwill.yuu

import org.apache.poi.ss.usermodel.Sheet

trait SheetReader[A] { self =>

  def read(sheet: Sheet): ReadResult[A]

  def map[B](f: A => B): SheetReader[B] =
    SheetReader[B] { sheet => self.read(sheet).map(f) }

  def flatMap[B](f: A => SheetReader[B]): SheetReader[B] =
    SheetReader[B] { sheet => self.read(sheet).flatMap(a => f(a).read(sheet)) }

  def filter(error: => String)(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filter(p) orElse ReadResult.error(error) }

  def filter(p: A => Boolean): SheetReader[A] =
    filter("Did not match filter")(p)

  def filterNot(error: => String)(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filterNot(p) orElse ReadResult.error(error) }

  def filterNot(p: A => Boolean): SheetReader[A] =
    filterNot("Did not match filter")(p)

  def collect[B](error: => String)(pf: PartialFunction[A, B]): SheetReader[B] = SheetReader[B] { sheet =>
    self.read(sheet) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error(error)
    }
  }

  def collect[B](pf: PartialFunction[A, B]): SheetReader[B] =
    collect("Did not match function")(pf)

  def orElse(other: => SheetReader[A]): SheetReader[A] = SheetReader[A] { sheet =>
    self.read(sheet) orElse other.read(sheet)
  }

  def compose[B <: Sheet](fb: SheetReader[B]): SheetReader[A] =
    SheetReader[A] { sheet => fb.read(sheet).flatMap(b => self.read(b)) }

  def andThen[B](fb: SheetReader[B])(implicit ev: A <:< Sheet): SheetReader[B] =
    fb.compose(this.map(ev))

}

object SheetReader {

  def apply[A](f: Sheet => ReadResult[A]): SheetReader[A] = new SheetReader[A] {
    def read(sheet: Sheet): ReadResult[A] = f(sheet)
  }

  def pure[A](v: A): SheetReader[A] = apply { _ => ReadResult.success(v) }

  @inline def of[A](implicit sr: SheetReader[A]): SheetReader[A] = sr

  implicit def singleSheetReader[A](implicit rr: RowReader[A]): SheetReader[A] = rr.single

  implicit def listSheetReader[A](implicit rr: RowReader[A]): SheetReader[List[A]] = rr.all

}
