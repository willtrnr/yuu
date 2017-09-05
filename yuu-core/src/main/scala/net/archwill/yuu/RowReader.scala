package net.archwill.yuu

import scala.collection.JavaConverters._

import org.apache.poi.ss.usermodel.Row

trait RowReader[A] { self =>

  def read(row: Row): ReadResult[A]

  def single: SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet.getRow(sheet.getFirstRowNum)) }

  def single(idx: Int): SheetReader[A] =
    SheetReader[A] { sheet => self.read(sheet.getRow(idx)) }

  def all: SheetReader[List[A]] =
    SheetReader[List[A]] { sheet =>
      sheet.rowIterator.asScala.foldLeft(ReadResult.success(List.empty[A])) { (la, a) =>
        la.flatMap(l => self.read(a).map(l :+ _))
      }
    }

  def map[B](f: A => B): RowReader[B] =
    RowReader[B] { row => self.read(row).map(f) }

  def flatMap[B](f: A => RowReader[B]): RowReader[B] =
    RowReader[B] { row => self.read(row).flatMap(a => f(a).read(row)) }

  def filter(error: => String)(p: A => Boolean): RowReader[A] =
    RowReader[A] { row => self.read(row).filter(p) orElse ReadResult.error(error) }

  def filter(p: A => Boolean): RowReader[A] =
    filter("Did not match filter")(p)

  def filterNot(error: => String)(p: A => Boolean): RowReader[A] =
    RowReader[A] { row => self.read(row).filterNot(p) orElse ReadResult.error(error) }

  def filterNot(p: A => Boolean): RowReader[A] =
    filterNot("Did not match filter")(p)

  def collect[B](error: => String)(pf: PartialFunction[A, B]): RowReader[B] = RowReader[B] { row =>
    self.read(row) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error(error)
    }
  }

  def collect[B](pf: PartialFunction[A, B]): RowReader[B] =
    collect("Did not match function")(pf)

  def orElse(other: => RowReader[A]): RowReader[A] = RowReader[A] { row =>
    self.read(row) orElse other.read(row)
  }

  def compose[B <: Row](fb: RowReader[B]): RowReader[A] =
    RowReader[A] { row => fb.read(row).flatMap(b => self.read(b)) }

  def andThen[B](fb: RowReader[B])(implicit ev: A <:< Row): RowReader[B] =
    fb.compose(this.map(ev))

}

object RowReader {

  def apply[A](f: Row => ReadResult[A]): RowReader[A] = new RowReader[A] {
    def read(row: Row): ReadResult[A] = f(row)
  }

  def pure[A](v: A): RowReader[A] = apply { _ => ReadResult.success(v) }

  @inline def of[A](implicit rr: RowReader[A]): RowReader[A] = rr

  def read[A](idx: Int)(implicit cr: CellReader[A]): RowReader[A] = cr.at(idx)
  def read[A](col: String)(implicit cr: CellReader[A]): RowReader[A] = cr.at(col)

  def bool(idx: Int): RowReader[Boolean] = read[Boolean](idx)
  def bool(col: String): RowReader[Boolean] = read[Boolean](col)

  def str(idx: Int): RowReader[String] = read[String](idx)
  def str(col: String): RowReader[String] = read[String](col)

  def double(idx: Int): RowReader[Double] = read[Double](idx)
  def double(col: String): RowReader[Double] = read[Double](col)

  def byte(idx: Int): RowReader[Byte] = read[Byte](idx)
  def byte(col: String): RowReader[Byte] = read[Byte](col)

  def short(idx: Int): RowReader[Short] = read[Short](idx)
  def short(col: String): RowReader[Short] = read[Short](col)

  def int(idx: Int): RowReader[Int] = read[Int](idx)
  def int(col: String): RowReader[Int] = read[Int](col)

  def long(idx: Int): RowReader[Long] = read[Long](idx)
  def long(col: String): RowReader[Long] = read[Long](col)

}
