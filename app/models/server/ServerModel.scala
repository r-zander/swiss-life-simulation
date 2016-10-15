package models
package server

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class SimulatorConfig(topics: List[String],
                           ages: Seq[Int],
                           questions: Seq[Question])

case class Question(topics: Seq[String],
                    preview: Option[String],
                    text: String,
                    ages: Seq[Int],
                    answers: Seq[Answer]) {

  def external(topic: String) = client.Question(topic, preview, text, ages, answers.map(_.external))

  def hasAge(age: Int) = {
    if (ages.isEmpty) age >= 25
    else ages contains age
  }
}

case class Answer(refs: Option[Seq[String]],
                  text: String,
                  satisfaction: Int,
                  money: Int,
                  probability: Double,
                  preconditions: Option[Seq[PreCond]],
                  riskFactors: Option[Seq[RiskFactor]]) {
  val id = ServerModel.generateId

  def external = client.Answer(id, text)

  lazy val flatRefs = refs.toSeq.flatten

  def hasRefs(allRefs: Set[String]) = {
    if (flatRefs.isEmpty) true
    else flatRefs exists allRefs
  }
}

case class AnsweredQuestion(ageLow: Int,
                            ageHigh: Int,
                            question: Question,
                            answer: Answer) {
  def years = ageHigh - ageLow

  def moneyScore = answer.money * years

  def satisfactionScore = answer.satisfaction * years
}

case class EndOfGame(causeOfDeath: String,
                     probability: Double,
                     highScore: Int) {
  def external = client.EndOfGame(causeOfDeath, f"${probability * 100}%.1f", highScore)
}

case class GameState(gameId: String,
                     age: Int,
                     openQuestions: Seq[Question],
                     answeredQuestions: Seq[AnsweredQuestion],
                     endOfGame: Option[EndOfGame]) {

  private lazy val accumulatedYears = answeredQuestions.map(_.years).sum

  lazy val money = {
    if (accumulatedYears == 0) 0
    else {
      val score = answeredQuestions.map(_.moneyScore).sum / accumulatedYears.toDouble
      ServerModel.normalizeScore(score, 0, 10)
    }
  }

  lazy val satisfaction = {
    if (accumulatedYears == 0) 5
    else {
      val score = answeredQuestions.map(_.satisfactionScore).sum / accumulatedYears.toDouble
      ServerModel.normalizeScore(score, 0, 10)
    }
  }

  def highScore = {
    answeredQuestions.map(_.satisfactionScore).sum
  }

  def external = client.GameState(gameId, age, money, satisfaction, Nil, endOfGame.map(_.external))

  def allRiskFactors = for (aq <- answeredQuestions; rfs <- aq.answer.riskFactors.toSeq; rf <- rfs) yield rf
}

case class PreCond(previousAnswer: String)

case class RiskFactor(cause: String,
                      value: Double)

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
  implicit val EndOfGameFormat = Json.format[EndOfGame]
  implicit val RiskFactorFormat = Json.format[RiskFactor]
  implicit val HiddenAnswerFormat = Json.format[Answer]
  implicit val HiddenQuestionFormat = Json.format[Question]
  implicit val AnsweredQuestionFormat = Json.format[AnsweredQuestion]
  implicit val HiddenGameStateFormat = Json.format[GameState]
  implicit val SimulatorConfigFormat = Json.format[SimulatorConfig]
}