package net.archwill.yuu

import org.apache.poi.ss.util.CellAddress

sealed trait ReadResult[+A] {

  def get: A

  def isSuccess: Boolean
  def isError: Boolean = !isSuccess

  def fold[X](error: Seq[(String, Option[CellAddress])] => X, success: A => X): X

  def getOrElse[AA >: A](orElse: => AA): AA =
    if (isSuccess) this.get else orElse

  def orElse[AA >: A](other: => ReadResult[AA]): ReadResult[AA] =
    if (isSuccess) this else other

  def map[B](f: A => B): ReadResult[B] = this match {
    case ReadSuccess(a) => ReadSuccess(f(a))
    case e: ReadError => e
  }

  def flatMap[B](f: A => ReadResult[B]): ReadResult[B] = this match {
    case ReadSuccess(a) => f(a)
    case e: ReadError => e
  }

  def filter(p: A => Boolean): ReadResult[A] =
    flatMap { a => if (p(a)) ReadSuccess(a) else ReadError() }

  def filterNot(p: A => Boolean): ReadResult[A] =
    flatMap { a => if (!p(a)) ReadSuccess(a) else ReadError() }

  def toOption: Option[A] =
    if (isSuccess) Option(this.get) else None

  def toEither: Either[Seq[(String, Option[CellAddress])], A] = this match {
    case ReadSuccess(a) => Right(a)
    case ReadError(e) => Left(e)
  }

}

case class ReadSuccess[A](value: A) extends ReadResult[A] {
  override def get: A = value
  override def isSuccess: Boolean = true
  override def fold[X](error: Seq[(String, Option[CellAddress])] => X, success: A => X): X = success(value)
}

case class ReadError(errors: Seq[(String, Option[CellAddress])] = Seq.empty) extends ReadResult[Nothing] {
  override def get: Nothing = throw new NoSuchElementException("ReadError.get")
  override def isSuccess: Boolean = false
  override def fold[X](error: Seq[(String, Option[CellAddress])] => X, success: Nothing => X): X = error(errors)
}

object ReadResult {

  @inline def success[A](a: A): ReadResult[A] = ReadSuccess(a)

  @inline def error[A]: ReadResult[A] = ReadError()
  @inline def error[A](error: String): ReadResult[A] = ReadError(Seq((error, None)))
  @inline def error[A](error: String, address: CellAddress): ReadResult[A] = ReadError(Seq((error, Option(address))))
  @inline def error[A](first: String, others: String*): ReadResult[A] = ReadError((first, None) +: others.map((_, None)))
  @inline def error[A](first: (String, Option[CellAddress]), others: (String, Option[CellAddress])*): ReadResult[A] = ReadError(first +: others.toSeq)
  @inline def error[A](errors: Seq[(String, Option[CellAddress])]): ReadResult[A] = ReadError(errors)

}
