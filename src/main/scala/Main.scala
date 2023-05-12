import Main.{FoldLeftWhile, ParserString}

import scala.language.implicitConversions
import scala.util.chaining.scalaUtilChainingOps

object Main {
  implicit class FoldLeftWhile[A](itOnce: IterableOnce[A]) {
    def foldLeftWhile[B](z: B)(op: (B, A) => Option[B]): B = {
      val iterator = itOnce.iterator
      var result = z
      var next = iterator.nextOption()
      while (next.isDefined) {
        op(result, next.get).fold {
          next = None
        } { b =>
          result = b
          next = iterator.nextOption()
        }
      }
      result
    }
  }

  implicit object ParserInt extends Parser[Int] {
    override def to(from: String): Int = from.toInt
  }

  implicit object ParserString extends Parser[String] {
    override def to(from: String): String = from
  }

  implicit def ParserTuple2[A, B, PA <: Parser[A], PB <: Parser[B]](implicit
      a: PA,
      b: PB
  ): Parser[(A, B)] =
    (from: String) =>
      Reader.one[A](a).product(Reader.one[B](b)).run(from.toList)._1

  implicit def ParserTuple3[
      A,
      B,
      C,
      PA <: Parser[A],
      PB <: Parser[B],
      PC <: Parser[C]
  ](implicit
      a: PA,
      b: PB,
      c: PC
  ): Parser[(A, B, C)] =
    (from: String) =>
      Reader
        .one[A](a)
        .product(Reader.one[B](b))
        .product(Reader.one[C](c))
        .map(n => (n._1._1, n._1._2, n._2))
        .run(from.toList)
        ._1

  // 競プロのコードはここのmain関数に書いて下さい
  def main(args: Array[String]): Unit = {
    val sampleInput =
      """|2 hello 3
         |a
         |b
         |c
         |d
         |hoge1 fuga1
         |hoge2 fuga2""".stripMargin

    (for {
      (num1, str1, num2) <- Reader.readOneLine[(Int, String, Int)]
      str2 <- Reader.one[String]
      strArray <- Reader.listOfN[String](num2)
      strTupleArray <- Reader.readMultipleLine[(String, String)](num1)
    } yield (num1, str1, num2, str2, strArray, strTupleArray))
      .run(sampleInput.toList)
      ._1
      .tap(println)
    // Output -> (2,hello,3,a,List(b, c, d),List((hoge1,fuga1), (hoge2,fuga2)))
  }
}

trait Parser[A] {
  def to(from: String): A
}

final case class Reader[+A](run: List[Char] => (A, List[Char])) {
  def map[B](f: A => B): Reader[B] = flatMap(n => Reader.unit(f(n)))

  def map2[B, C](that: Reader[B])(f: (A, B) => C): Reader[C] = for {
    one <- this
    two <- that
  } yield f(one, two)

  def flatMap[B](f: A => Reader[B]): Reader[B] = Reader { s =>
    val (a, ss) = run(s)
    f(a).run(ss)
  }

  def withFilter[B](f: A => Boolean): Reader[A] = this

  def product[B](that: Reader[B]): Reader[(A, B)] = map2(that)((_, _))
}

private object Reader {
  def unit[A](a: => A): Reader[A] = Reader((a, _))

  def extractor[A](
      cond: Char => Boolean
  )(implicit parser: Parser[A]): Reader[A] = Reader { s =>
    val (matched, remains) = s.foldLeftWhile(("", s))((b, a) =>
      Option.when(cond(a))((b._1 + a, b._2.tail))
    )
    (parser.to(matched), if (remains.nonEmpty) remains.tail else Nil)
  }

  def one[A](implicit parser: Parser[A]): Reader[A] =
    extractor[A](a => !a.isWhitespace && a != '\n')

  def readOneLine[A](implicit parser: Parser[A]): Reader[A] =
    extractor[A](_ != '\n')

  def readMultipleLine[A](n: Int)(implicit parser: Parser[A]): Reader[List[A]] =
    sequence(
      (for (_ <- 0 until n) yield readOneLine[A]).toList
    )

  def listOfN[A: Parser](n: Int): Reader[List[A]] =
    sequence((for (_ <- 0 until n) yield one[A]).toList)

  def sequence[A](list: List[Reader[A]]): Reader[List[A]] =
    list.foldLeft(unit(List.empty[A]))((b, a) => a.map2(b)(_ :: _))
}
