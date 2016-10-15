package modules

import scala.annotation.tailrec
import scala.util.Random

object Rand {

  val RND = new Random

  def selectRandom[T](available: Seq[T], name: String): T = {
    if (available.isEmpty) sys.error(s"Keine $name gefunden !")
    else available(RND.nextInt(available.size))
  }

  def selectRandom[T](available: Seq[T], weight: T => Double, name: String): T = {
    @tailrec
    def rec(sample: Double, remaining: List[T]): T = remaining match {
      case Nil => available.last
      case r :: rs =>
        val w = weight(r)
        if (sample <= w) r
        else rec(sample - w, rs)
    }
    if (available.isEmpty) sys.error(s"Keine $name gefunden !")
    else {
      val universe = available.map(weight).sum
      rec(RND.nextDouble() * universe, available.toList)
    }
  }
}
