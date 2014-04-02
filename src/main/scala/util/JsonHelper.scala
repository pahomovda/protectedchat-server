package util

import play.api.libs.json._

/**
 * Created by 7 on 02.04.2014.
 */
package object JsonExtensions {

  class JsonWrapper(val jsonval: JsValue, val sc: StringContext) {
    def compare(matcher: JsZipper, zipper: JsZipper): Option[Seq[JsValue]] = {
      def step(acc: Seq[Option[JsValue]], curMatcher: JsZipper, curZipper: JsZipper): Seq[Option[JsValue]] = {
        (curMatcher.value, curZipper.value) match {
          case (JsString("_?_:String"), a: JsString) => curZipper.right match {
            case JsZipper.Empty => acc :+ Some(a)
            case right => step(acc :+ Some(a), curMatcher.right, right)
          }

          case (JsString("_?_"), a) => curZipper.right match {
            case JsZipper.Empty => acc :+ Some(a)
            case right => step(acc :+ Some(a), curMatcher.right, right)
          }

          case (a, b) if a == b => curZipper.right match {
            case JsZipper.Empty => acc
            case right => step(acc, curMatcher.right, right)
          }

          case (a, b) => curZipper.right match {
            case JsZipper.Empty => if (curZipper.isLeaf) acc :+ None
            else step(acc, curMatcher.down.first, curZipper.down.first)
            case right => if (curZipper.isLeaf) step(acc :+ None, curMatcher.right, right)
            else step(acc, curMatcher.down.first, curZipper.down.first)
          }
        }
      }

      val matching = step(Seq(), matcher, zipper)

      if (matching.exists(_.isEmpty)) None
      else Some(matching.map(_.get))
    }

    val VAR_REPLACE = "\"_?_\""

    def unapplySeq(js: JsValue): Option[Seq[JsValue]] = {
      //val jsBase = Json.parse(sc.parts.mkString(VAR_REPLACE))

      compare(JsZipper(jsonval), JsZipper(js))
      /*val witness = JsZipper(jsBase).streamDeepLeaves
    val sample = JsZipper(js).streamDeepLeaves

    val zipped = sample.zip(witness)
    val matching = zipped.foldLeft(Seq[Option[JsValue]]()){ case( all, (zipsample, zipwitness)) =>
      (zipsample.value, zipwitness.value) match {
        case (a, JsNull)      => all :+ Some(a)
        case (a, b) if a == b => all
        case _                => all :+ None
      }
    }

    println("matching:"+matching)

    if(matching.exists( _.isEmpty )) None
    else Some(matching.map(_.get))*/

    }
  }

  implicit class JsonHelper(val sc: StringContext) extends {

    object json {
      def apply(args: Any*): JsonWrapper = {
        val strings = sc.parts.iterator
        val expressions = args.iterator
        var buf = new StringBuffer(strings.next)
        while (strings.hasNext) {
          buf append expressions.next
          buf append strings.next
        }
        val jsval = Json.parse(buf.toString)
        new JsonWrapper(jsval, sc)
      }
    }
  }
}