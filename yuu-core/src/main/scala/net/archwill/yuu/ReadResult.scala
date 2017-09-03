package net.archwill.yuu

sealed trait ReadResult[+A] {
  
  def get: A
  
  def isSuccess: Boolean
  def isFailure: Boolean = !isSuccess

  def fold[X](error: Seq[String] => X, success: A => X): X

  @inline def getOrElse[AA >: A](orElse: => AA): AA =
    if (isSuccess) this.get else orElse

  @inline def orElse[AA >: A](other: ReadResult[AA]): ReadResult[AA] =
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

  @inline def toOption: Option[A] =
    if (isSuccess) Option(this.get) else None

  def toEither: Either[Seq[String], A] = this match {
    case ReadSuccess(a) => Right(a)
    case ReadError(e) => Left(e)
  }

}

case class ReadSuccess[A](value: A) extends ReadResult[A] {
  override def get: A = value
  override def isSuccess: Boolean = true
  override def fold[X](error: Seq[String] => X, success: A => X): X = success(value)
}

case class ReadError(errors: Seq[String] = Seq.empty) extends ReadResult[Nothing] {
  override def get: Nothing = throw new NoSuchElementException("ReadError.get")
  override def isSuccess: Boolean = false
  override def fold[X](error: Seq[String] => X, success: Nothing => X): X = error(errors)
}

object ReadResult {

  def success[A](a: A): ReadResult[A] = ReadSuccess(a)

  def error[A]: ReadResult[A] = ReadError()
  def error[A](error: String): ReadResult[A] = ReadError(Seq(error))
  def error[A](errors: Seq[String]): ReadResult[A] = ReadError(errors)

}
