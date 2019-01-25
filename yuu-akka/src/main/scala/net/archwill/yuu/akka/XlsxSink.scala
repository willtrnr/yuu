package net.archwill.yuu.akka

import scala.util.control.NonFatal
import scala.collection.immutable

import java.io.{File, FileOutputStream, OutputStream}

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler}
import akka.stream.{Attributes, Inlet, SinkShape}

import org.apache.poi.ss.usermodel.{Cell, CellType, Row, Sheet}
import org.apache.poi.xssf.streaming.SXSSFWorkbook

class XlsxSink(os: () => OutputStream) extends GraphStage[SinkShape[XlsxSinkCommand]] {

  import XlsxSinkCommand._, CellValue._

  val in: Inlet[XlsxSinkCommand] = Inlet("XlsxSink")
  override val shape: SinkShape[XlsxSinkCommand] = SinkShape(in)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) with InHandler {

    private val wb: SXSSFWorkbook = new SXSSFWorkbook()

    private var sheet: Sheet = _
    private var row: Row = _
    private var cell: Cell = _

    override def preStart(): Unit = {
      pull(in)
    }

    override def onPush(): Unit = try {
      grab(in) match {
        case CreateSheet(name) =>
          sheet = wb.createSheet(name)
          row = null
          cell = null
        case CreateRow(num) =>
          if (sheet eq null) throw new IllegalStateException("no active sheet")
          row = sheet.createRow(num)
          cell = null
        case CreateCell(col) =>
          if (row eq null) throw new IllegalStateException("no active row")
          cell = row.createCell(col)
        case SetCellValue(value) =>
          if (cell eq null) throw new IllegalStateException("no active cell")
          value match {
            case BooleanCellValue(v) =>
              cell.setCellValue(v)
              cell.setCellType(CellType.BOOLEAN)
            case StringCellValue(v) =>
              cell.setCellValue(v)
              cell.setCellType(CellType.STRING)
            case NumericCellValue(v) =>
              cell.setCellValue(v)
              cell.setCellType(CellType.NUMERIC)
            case BlankCellValue =>
              cell.setCellType(CellType.BLANK)
          }
      }
      pull(in)
    } catch {
      case NonFatal(e) =>
        wb.dispose()
        failStage(e)
    }

    override def onUpstreamFinish(): Unit = {
      var stream: OutputStream = null
      try {
        stream = os()
        try {
          wb.write(stream)
        } finally {
          stream.close()
        }
        completeStage()
      } catch {
        case NonFatal(e) =>
          failStage(e)
      } finally {
        wb.dispose()
      }
    }

    setHandler(in, this)

  }

}

sealed trait XlsxSinkCommand

object XlsxSinkCommand {

  final case class CreateSheet(name: String) extends XlsxSinkCommand
  final case class CreateRow(num: Int) extends XlsxSinkCommand
  final case class CreateCell(col: Int) extends XlsxSinkCommand
  final case class SetCellValue(value: CellValue) extends XlsxSinkCommand

}

sealed trait CellValue

object CellValue {

  final case object BlankCellValue extends CellValue
  final case class BooleanCellValue(value: Boolean) extends CellValue
  final case class StringCellValue(value: String) extends CellValue
  final case class NumericCellValue(value: Double) extends CellValue

}

object XlsxSink {

  def apply(os: () => OutputStream): Sink[XlsxSinkCommand, NotUsed] =
    Sink.fromGraph(new XlsxSink(os))

  def apply(file: File): Sink[XlsxSinkCommand, NotUsed] =
    apply(() => new FileOutputStream(file))

  def apply(path: String): Sink[XlsxSinkCommand, NotUsed] =
    apply(new File(path))

  def asCommands(sheetName: String): Flow[immutable.Seq[CellValue], XlsxSinkCommand, NotUsed] = {
    import XlsxSinkCommand._, CellValue._
    Flow[immutable.Seq[CellValue]]
      .statefulMapConcat { () =>
        var i = -1
        row => {
          val cells = row.zipWithIndex flatMap {
            case (BlankCellValue, i) =>
              Nil
            case (v, i) =>
              List(CreateCell(i), SetCellValue(v))
          }
          i += 1
          CreateRow(i) +: cells
        }
      }
      .prepend(Source.single(CreateSheet(sheetName)))
  }

}
