package net.archwill.yuu.scalaz

import _root_.scalaz._

import net.archwill.yuu.{ReadResult, ReadSuccess, ReadError}

trait ReadResultInstances {

  implicit val readResultInstances: Monad[ReadResult] = new Monad[ReadResult] {
    
    override def point[A](a: => A): ReadResult[A] =
      ReadSuccess(a)

    override def map[A, B](fa: ReadResult[A])(f: A => B): ReadResult[B] =
      fa map f

    override def ap[A, B](fa: => ReadResult[A])(f: => ReadResult[A => B]): ReadResult[B] = (fa, f) match {
      case (ReadSuccess(a), ReadSuccess(ff)) => ReadSuccess(ff(a))
      case (ReadError(e1), ReadError(e2)) => ReadError(e1 ++ e2)
      case (e: ReadError, _) => e
      case (_, e: ReadError) => e
    }

    override def bind[A, B](fa: ReadResult[A])(f: A => ReadResult[B]): ReadResult[B] =
      fa flatMap f

  }

}

object readResult extends ReadResultInstances {
  object readResultSyntax extends syntax.ToReadResultOps with syntax.ToReadResultIdOps
}
