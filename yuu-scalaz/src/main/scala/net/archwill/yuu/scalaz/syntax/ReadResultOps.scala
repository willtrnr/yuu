package net.archwill.yuu.scalaz.syntax

import net.archwill.yuu.ReadResult

final class ReadResultIdOps[A](private val self: A) extends AnyVal {

  def readSuccess: ReadResult[A] = ReadResult.success(self)

  def readError[B](implicit ev: A <:< String): ReadResult[B] = ReadResult.error(self)

}

trait ToReadResultIdOps {
  implicit def toReadResultIdOps[A](a: A): ReadResultIdOps[A] = new ReadResultIdOps(a)
}
