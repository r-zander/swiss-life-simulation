package models

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SimulatorConfig(
                          topics: List[String],
                          ages: Seq[Int],
                          questions: Seq[HiddenQuestion]
                          )

case class HiddenQuestion(
                           topics: Seq[String],
                           text: String,
                           ages: Seq[Int],
                           answers: Seq[HiddenAnswer]
                         ) {
  def external(topic: String) = Question(topic, text, ages, answers.map(_.external))

  def hasAge(age: Int) = {
    if (ages.isEmpty) age >= 25
    else ages contains age
  }
}

case class HiddenAnswer(
                         ref: Option[String],
                         text: String,
                         satisfaction: Int,
                         money: Int,
                         probability: Double,
                         preconditions: Option[Seq[PreCond]]
                       ) {
  val id = ServerModel.generateId
  def external = Answer(id, text)
}

case class AnsweredQuestion(
                             ageLow: Int,
                             ageHigh: Int,
                             question: HiddenQuestion,
                             answer: HiddenAnswer
                           ) {
  def years = ageHigh - ageLow
  def moneyScore = answer.money * years
  def satisfactionScore = answer.satisfaction * years
}

case class HiddenGameState(
                      gameId: String,
                      age: Int,
                      openQuestions: Seq[HiddenQuestion],
                      answeredQuestions: Seq[AnsweredQuestion]
                    ) {

  lazy val money = if (age == 0) 0 else {
    val score = answeredQuestions.map(_.moneyScore).sum / age.toDouble
    ServerModel.normalizeScore(score, 0, 10)
  }

  lazy val satisfaction = if (age == 0) 5 else {
    val score = answeredQuestions.map(_.satisfactionScore).sum / age.toDouble
    ServerModel.normalizeScore(score, 0, 10)
  }

  def external = GameState(gameId, age, money, satisfaction, Nil)
}

case class PreCond(
                    prevAnswer: Seq[String]
                  )

case class RiskFactor(
                       text: String,
                       riskValue: Double
                     )

object ServerModel {
  private val counter = new java.util.concurrent.atomic.AtomicInteger
  def generateId: String = counter.incrementAndGet.toString

  def normalizeScore(score: Double, low: Int, high: Int) = score match {
    case s if s <= low.toDouble => low
    case s if s >= high.toDouble => high
    case s => (s + 0.5).toInt
  }
}

trait ServerModel {

  implicit val PreCondFormat = Json.format[PreCond]
  implicit val RiskFactorFormat = Json.format[RiskFactor]
  implicit val HiddenAnswerFormat = Json.format[HiddenAnswer]
  implicit val HiddenQuestionFormat = Json.format[HiddenQuestion]
  implicit val AnsweredQuestionFormat = Json.format[AnsweredQuestion]
  implicit val HiddenGameStateFormat = Json.format[HiddenGameState]
  implicit val SimulatorConfigFormat = Json.format[SimulatorConfig]
}