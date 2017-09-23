package net.archwill.yuu

import scala.annotation.implicitNotFound

import java.sql.{Date => SDate, Time => STime, Timestamp => STimestamp}
import java.time.Instant
import java.util.Date

import org.apache.poi.ss.usermodel.{Cell, CellType, DateUtil}
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy
import org.apache.poi.ss.util.CellReference

@implicitNotFound("No implicit reader found for type ${A}, try implementing CellReader[${A}]")
trait CellReader[A] { self =>

  def read(cell: Cell): ReadResult[A]

  def at(idx: Int): RowReader[A] = RowReader[A] { row =>
    if (idx >= 0)
      self.read(row.getCell(idx, MissingCellPolicy.CREATE_NULL_AS_BLANK))
    else
      ReadResult.error("Invalid column reference")
  }

  def at(col: String): RowReader[A] =
    at(CellReference.convertColStringToIndex(col))

  def at(col: Int, row: Int): SheetReader[A] = SheetReader[A] { sheet =>
    if (col >= 0 && row >= 0)
      self.read(sheet.getRow(row).getCell(col, MissingCellPolicy.CREATE_NULL_AS_BLANK))
    else
      ReadResult.error("Invalid cell reference")
  }

  def at(col: String, row: Int): SheetReader[A] =
    at(CellReference.convertColStringToIndex(col), row)

  def opt: CellReader[Option[A]] =
    CellReader[Option[A]] { cell => ReadResult.success(self.read(cell).toOption) }

  def map[B](f: A => B): CellReader[B] =
    CellReader[B] { cell => self.read(cell).map(f) }

  def flatMap[B](f: A => CellReader[B]): CellReader[B] =
    CellReader[B] { cell => self.read(cell).flatMap(a => f(a).read(cell)) }

  def filter(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filter(p) orElse ReadResult.error }

  def filter(error: => String)(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filter(p) orElse ReadResult.error(error) }

  def filterNot(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filterNot(p) orElse ReadResult.error }

  def filterNot(error: => String)(p: A => Boolean): CellReader[A] =
    CellReader[A] { cell => self.read(cell).filterNot(p) orElse ReadResult.error(error) }

  def collect[B](pf: PartialFunction[A, B]): CellReader[B] = CellReader[B] { cell =>
    self.read(cell) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error
    }
  }

  def collect[B](error: => String)(pf: PartialFunction[A, B]): CellReader[B] = CellReader[B] { cell =>
    self.read(cell) flatMap {
      case a if pf.isDefinedAt(a) => ReadResult.success(pf(a))
      case _ => ReadResult.error(error)
    }
  }

  def orElse(other: => CellReader[A]): CellReader[A] =
    CellReader[A] { cell => self.read(cell) orElse other.read(cell) }

  def compose[B <: Cell](fb: CellReader[B]): CellReader[A] =
    CellReader[A] { cell => fb.read(cell).flatMap(b => self.read(b)) }

  def andThen[B](fb: CellReader[B])(implicit ev: A <:< Cell): CellReader[B] =
    fb.compose(this.map(ev))

}

object CellReader {

  import ReadResult._

  def apply[A](f: Cell => ReadResult[A]): CellReader[A] = new CellReader[A] {
    def read(cell: Cell): ReadResult[A] = f(cell)
  }

  def pure[A](v: A): CellReader[A] = apply { _ => success(v) }

  @inline def of[A](implicit cr: CellReader[A]): CellReader[A] = cr

  implicit val cellCellReader: CellReader[Cell] = apply(success)

  implicit val booleanCellReader: CellReader[Boolean] = apply { cell =>
    if (cell.valueType == CellType.BOOLEAN) {
      success(cell.getBooleanCellValue)
    } else {
      error("Expected boolean cell")
    }
  }

  implicit val stringCellReader: CellReader[String] = apply { cell =>
    if (cell.valueType == CellType.STRING) {
      success(cell.getStringCellValue)
    } else {
      error("Expected string cell")
    }
  }

  implicit val doubleCellReader: CellReader[Double] = apply { cell =>
    if (cell.valueType == CellType.NUMERIC) {
      success(cell.getNumericCellValue)
    } else {
      error("Expected numeric cell")
    }
  }

  implicit val byteCellReader: CellReader[Byte] =
    doubleCellReader.collect("Not a valid byte") { case d if d.isValidByte => d.toByte }

  implicit val shortCellReader: CellReader[Short] =
    doubleCellReader.collect("Not a valid short") { case d if d.isValidShort => d.toShort }

  implicit val intCellReader: CellReader[Int] =
    doubleCellReader.collect("Not a valid integer") { case d if d.isValidInt => d.toInt }

  implicit val longCellReader: CellReader[Long] =
    doubleCellReader.collect("Not a valid long") { case d if d.isWhole => d.toLong }

  implicit val floatCellReader: CellReader[Float] =
    doubleCellReader.map(_.toFloat)

  implicit val dateCellReader: CellReader[Date] = apply { cell =>
    if (cell.valueType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
      Option(DateUtil.getJavaDate(cell.getNumericCellValue)).fold(error[Date]("Not a valid date"))(success)
    } else {
      error("Expected numeric cell")
    }
  }

  implicit val sDateCellReader: CellReader[SDate] =
    dateCellReader.map(d => new SDate(d.getTime))

  implicit val sTimeCellReader: CellReader[STime] =
    dateCellReader.map(d => new STime(d.getTime))

  implicit val sTimestampCellReader: CellReader[STimestamp] =
    dateCellReader.map(d => new STimestamp(d.getTime))

  implicit val instantCellReader: CellReader[Instant] =
    dateCellReader.map(_.toInstant)

  implicit def optionCellReader[A](implicit cr: CellReader[A]): CellReader[Option[A]] = apply { cell =>
    if (cell.valueType == CellType.BLANK) {
      success(None)
    } else {
      cr.read(cell).map(Option(_))
    }
  }

}
