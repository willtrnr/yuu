package net.archwill.yuu.akka

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import java.io.{File, FileInputStream, InputStream}
import javax.xml.namespace.QName
import javax.xml.stream.{XMLEventReader, XMLInputFactory}
import javax.xml.stream.events.{EndElement, StartElement}

import akka.NotUsed
import akka.stream.{Attributes, Outlet, SourceShape}
import akka.stream.scaladsl.Source
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}

import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.{Cell, CellStyle, CellType, Comment, Hyperlink, RichTextString, Row, Sheet}
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.util.{CellAddress, CellRangeAddress}
import org.apache.poi.xssf.eventusermodel.{ReadOnlySharedStringsTable, XSSFReader}
import org.apache.poi.xssf.model.StylesTable

// Barebone POI Cell implementation to allow using Yuu readers on the results
case class StreamingCell(address: CellAddress, valueType: String = null, style: CellStyle = null, value: String = null) extends Cell {

  private[this] var row: Row = _
  private[akka] def withRow(r: Row): StreamingCell = {
    row = r
    this
  }

  override def getAddress(): CellAddress = address
  override def getBooleanCellValue(): Boolean = value == "1"
  override def getCachedFormulaResultType(): Int = 1 // FIXME
  override def getCachedFormulaResultTypeEnum(): CellType = CellType.STRING // FIXME: It's like that in the POI impl though
  override def getCellStyle(): CellStyle = style
  override def getCellType(): Int =
    getCellTypeEnum match {
      case CellType.NUMERIC => 0
      case CellType.STRING => 1
      case CellType.FORMULA => 2
      case CellType.BLANK => 3
      case CellType.BOOLEAN => 4
      case CellType.ERROR => 5
      case _ => -1
    }
  override def getCellTypeEnum(): CellType =
    valueType match {
      case null => CellType.BLANK
      case "b" => CellType.BOOLEAN
      case "n" | "d" => CellType.NUMERIC
      case "e" => CellType.ERROR
      case "s" | "str" | "inlineStr" => CellType.STRING // "str" is actually a formula type column
      case _ => CellType.NUMERIC
    }
  override def getColumnIndex(): Int = address.getColumn
  override def getDateCellValue(): java.util.Date = DateUtil.getJavaDate(getNumericCellValue)
  override def getErrorCellValue(): Byte = value.toByte
  override def getNumericCellValue(): Double = value.toDouble
  override def getRow(): Row = row
  override def getRowIndex(): Int = row.getRowNum
  override def getStringCellValue(): String = value

  // Unsupported API, mostly writing and other rarely used features
  override def getArrayFormulaRange(): CellRangeAddress = throw new UnsupportedOperationException
  override def getCellComment(): Comment = throw new UnsupportedOperationException
  override def getCellFormula(): String = throw new UnsupportedOperationException
  override def getHyperlink(): Hyperlink = throw new UnsupportedOperationException
  override def getRichStringCellValue(): RichTextString = throw new UnsupportedOperationException
  override def getSheet(): Sheet = throw new UnsupportedOperationException
  override def isPartOfArrayFormulaGroup(): Boolean = throw new UnsupportedOperationException
  override def removeCellComment(): Unit = throw new UnsupportedOperationException
  override def removeHyperlink(): Unit = throw new UnsupportedOperationException
  override def setAsActiveCell(): Unit = throw new UnsupportedOperationException
  override def setCellComment(x$1: Comment): Unit = throw new UnsupportedOperationException
  override def setCellErrorValue(x$1: Byte): Unit = throw new UnsupportedOperationException
  override def setCellFormula(x$1: String): Unit = throw new UnsupportedOperationException
  override def setCellStyle(x$1: CellStyle): Unit = throw new UnsupportedOperationException
  override def setCellType(x$1: CellType): Unit = throw new UnsupportedOperationException
  override def setCellType(x$1: Int): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: Boolean): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: Double): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: java.util.Calendar): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: java.util.Date): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: RichTextString): Unit = throw new UnsupportedOperationException
  override def setCellValue(x$1: String): Unit = throw new UnsupportedOperationException
  override def setHyperlink(x$1: Hyperlink): Unit = throw new UnsupportedOperationException

}

// Barebone POI Row implementation to allow using Yuu readers on the results
case class StreamingRow(num: Int, cells: Map[Int, StreamingCell] = Map.empty, style: CellStyle = null, defaultStyle: CellStyle = null) extends Row {

  private[this] var blank = StreamingCell(address = null, valueType = null, style = defaultStyle, value = null).withRow(this)

  override def cellIterator(): java.util.Iterator[Cell] = cells.valuesIterator.asJava.asInstanceOf[java.util.Iterator[Cell]]
  override def getCell(cellnum: Int, policy: Row.MissingCellPolicy): Cell =
    policy match {
      case Row.MissingCellPolicy.RETURN_NULL_AND_BLANK =>
        cells.get(cellnum).map(_.withRow(this)).orNull
      case Row.MissingCellPolicy.RETURN_BLANK_AS_NULL =>
        cells.get(cellnum).filter(_.valueType ne null).map(_.withRow(this)).orNull
      case Row.MissingCellPolicy.CREATE_NULL_AS_BLANK =>
        cells.get(cellnum).map(_.withRow(this)).getOrElse(blank.copy(address = new CellAddress(num, cellnum)))
    }
  override def getCell(cellnum: Int): Cell = getCell(cellnum, Row.MissingCellPolicy.RETURN_NULL_AND_BLANK)
  override def getFirstCellNum(): Short = if (cells.isEmpty) -1 else cells.keysIterator.min.toShort
  override def getHeight(): Short = 255
  override def getHeightInPoints(): Float = getHeight.toFloat / 20.0f
  override def getLastCellNum(): Short = if (cells.isEmpty) -1 else (cells.keysIterator.max + 1).toShort
  override def getPhysicalNumberOfCells(): Int = cells.size
  override def getRowNum(): Int = num
  override def getRowStyle(): CellStyle = style
  override def getZeroHeight(): Boolean = false
  override def isFormatted(): Boolean = style ne null
  override def iterator(): java.util.Iterator[Cell] = cellIterator

  // Unsupported API, mostly writing
  override def createCell(x$1: Int, x$2: CellType): Cell = throw new UnsupportedOperationException
  override def createCell(x$1: Int, x$2: Int): Cell = throw new UnsupportedOperationException
  override def createCell(x$1: Int): Cell = throw new UnsupportedOperationException
  override def getOutlineLevel(): Int = throw new UnsupportedOperationException
  override def getSheet(): Sheet = throw new UnsupportedOperationException
  override def removeCell(x$1: Cell): Unit = throw new UnsupportedOperationException
  override def setHeight(x$1: Short): Unit = throw new UnsupportedOperationException
  override def setHeightInPoints(x$1: Float): Unit = throw new UnsupportedOperationException
  override def setRowNum(x$1: Int): Unit = throw new UnsupportedOperationException
  override def setRowStyle(x$1: CellStyle): Unit = throw new UnsupportedOperationException
  override def setZeroHeight(x$1: Boolean): Unit = throw new UnsupportedOperationException

}

class XlsxSource(is: () => InputStream, sheet: String) extends GraphStage[SourceShape[Row]] {

  val out: Outlet[Row] = Outlet("XlsxSource")
  override val shape: SourceShape[Row] = SourceShape(out)

  private[this] val xmlInput = XMLInputFactory.newFactory()

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private var stream: InputStream = _

    private var pkg: OPCPackage = _
    private var sst: ReadOnlySharedStringsTable = _
    private var reader: XSSFReader = _
    private var styles: StylesTable = _
    private var defaultStyle: CellStyle = _
    private var events: XMLEventReader = _

    override def preStart(): Unit = {
      try {
        stream = is()
        pkg = OPCPackage.open(stream)
        sst = new ReadOnlySharedStringsTable(pkg)
        reader = new XSSFReader(pkg)
        styles = reader.getStylesTable()
        defaultStyle = styles.getStyleAt(0)

        var found = false
        val sheets = reader.getSheetsData().asInstanceOf[XSSFReader.SheetIterator]
        while (sheets.hasNext && !found) {
          val ss = sheets.next()
          if (sheets.getSheetName == sheet) {
            events = xmlInput.createXMLEventReader(ss)
            found = true
          } else {
            ss.close()
          }
        }
      } catch {
        case NonFatal(e) =>
          postStop()
          throw e
      }
    }

    override def postStop(): Unit = {
      if (events ne null) events.close()
      if (pkg ne null) pkg.close()
      if (stream ne null) stream.close()
    }

    setHandler(out, new OutHandler {

      override def onPull(): Unit = {
        if (events eq null) {
          completeStage()
        } else {
          var row: StreamingRow = null
          var cell: StreamingCell = null

          var continue = true
          while (events.hasNext && continue) {
            events.nextEvent match {
              case e: StartElement if e.getName.getLocalPart == "row" =>
                row = StreamingRow(
                  num = e.getAttributeByName(new QName("r")).getValue.toInt,
                  style = Option(e.getAttributeByName(new QName("s"))).map(_.getValue.toInt).map(styles.getStyleAt).orNull
                )
              case e: StartElement if e.getName.getLocalPart == "c" && (row ne null) =>
                // Get what we can about the cell, see the next case for why we don't use `getElementText` to retrieve the value here
                cell = StreamingCell(
                  address = new CellAddress(e.getAttributeByName(new QName("r")).getValue),
                  valueType = Option(e.getAttributeByName(new QName("t"))).map(_.getValue).getOrElse(""),
                  style = Option(e.getAttributeByName(new QName("s"))).map(_.getValue.toInt).map(styles.getStyleAt).getOrElse(defaultStyle)
                )
              case e: StartElement if e.getName.getLocalPart == "v" && (cell ne null) =>
                // We use the <v> element to get the value since formula <c>'s will also include an <f>,
                // so `getElementText` wouldn't work properly on <c> directly as we'd also get the formula contents
                cell = cell.copy(value =
                  if (cell.valueType == "s")
                    sst.getEntryAt(events.getElementText.trim.toInt)
                  else
                    events.getElementText.trim
                )
              case e: EndElement if e.getName.getLocalPart == "c" && (cell ne null) =>
                // Both blank and numeric cells won't carry the "t" attribute,
                // if by the time we reach </c> we still haven't seen a <v> then the cell is blank
                if (cell.value eq null) cell = cell.copy(valueType = null)
                row = row.copy(cells = row.cells + (cell.address.getColumn -> cell))
                cell = null
              case e: EndElement if e.getName.getLocalPart == "row" && (row ne null) =>
                continue = false
              case _ =>
            }
          }

          if (row eq null) {
            // Couldn't find a new row, this means we're done
            completeStage()
          } else {
            push(out, row)
          }
        }
      }

    })

  }

}

object XlsxSource {

  def apply(is: () => InputStream, sheet: String): Source[Row, NotUsed] =
    Source.fromGraph(new XlsxSource(is, sheet))

  def apply(file: File, sheet: String): Source[Row, NotUsed] =
    apply(() => new FileInputStream(file), sheet)

  def apply(path: String, sheet: String): Source[Row, NotUsed] =
    apply(new File(path), sheet)

}
