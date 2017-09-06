package net.archwill.yuu

import org.apache.poi.ss.usermodel.Sheet

trait SheetReader[A] { self =>

  def read(sheet: Sheet): ReadResult[A]

  def map[B](f: A => B): SheetReader[B] =
    SheetReader[B] { sheet => self.read(sheet).map(f) }

  def flatMap[B](f: A => SheetReader[B]): SheetReader[B] =
    SheetReader[B] { sheet => self.read(sheet).flatMap(a => f(a).read(sheet)) }

  def filter(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filter(p) orElse ReadResult.error }

  def filter(error: => String)(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filter(p) orElse ReadResult.error(error) }

  def filterNot(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filterNot(p) orElse ReadResult.error }

  def filterNot(error: => String)(p: A => Boolean): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet).filterNot(p) orElse ReadResult.error(error) }

  def collect[B](pf: PartialFunction[A, B]): SheetReader[B] = SheetReader[B] { sheet =>
    self.read(sheet) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error
    }
  }

  def collect[B](error: => String)(pf: PartialFunction[A, B]): SheetReader[B] = SheetReader[B] { sheet =>
    self.read(sheet) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error(error)
    }
  }

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

  def read[A](col: Int, row: Int)(implicit cr: CellReader[A]): SheetReader[A] = cr.at(col, row)
  def read[A](col: String, row: Int)(implicit cr: CellReader[A]): SheetReader[A] = cr.at(col, row)

  def bool(col: Int, row: Int): SheetReader[Boolean] = read[Boolean](col, row)
  def bool(col: String, row: Int): SheetReader[Boolean] = read[Boolean](col, row)

  def str(col: Int, row: Int): SheetReader[String] = read[String](col, row)
  def str(col: String, row: Int): SheetReader[String] = read[String](col, row)

  def double(col: Int, row: Int): SheetReader[Double] = read[Double](col, row)
  def double(col: String, row: Int): SheetReader[Double] = read[Double](col, row)

  def byte(col: Int, row: Int): SheetReader[Byte] = read[Byte](col, row)
  def byte(col: String, row: Int): SheetReader[Byte] = read[Byte](col, row)

  def short(col: Int, row: Int): SheetReader[Short] = read[Short](col, row)
  def short(col: String, row: Int): SheetReader[Short] = read[Short](col, row)

  def int(col: Int, row: Int): SheetReader[Int] = read[Int](col, row)
  def int(col: String, row: Int): SheetReader[Int] = read[Int](col, row)

  def long(col: Int, row: Int): SheetReader[Long] = read[Long](col, row)
  def long(col: String, row: Int): SheetReader[Long] = read[Long](col, row)

  implicit def singleSheetReader[A](implicit rr: RowReader[A]): SheetReader[A] = rr.single

  implicit def listSheetReader[A](implicit rr: RowReader[A]): SheetReader[List[A]] = rr.all

}
