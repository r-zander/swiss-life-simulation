package models
package server

import play.api.libs.json._

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
    else if (age <= 80) ages contains age
    else ages.max >= 80
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

  override def toString = s"Answer(#$id,$refs,$text,$preconditions,$riskFactors)"

  def external = client.Answer(id, text)

  lazy val flatRefs = refs.toSeq.flatten
  lazy val flatPreConds = preconditions.toSeq.flatten

  def checkPrecondition(allRefs: Set[String]) = {
    if (flatPreConds.isEmpty) true
    else flatPreConds.map(_.previousAnswer) exists allRefs
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
  private lazy val maxYear = answeredQuestions.map(_.ageHigh).max

  lazy val money = {
    if (accumulatedYears == 0) 0
    else {
      // Money zu 50% für letzte Periode und zu 50% für vorhergehende
      val score = (answeredQuestions.partition(_.ageHigh == maxYear) match {
        case (last, prev) => (averageMoney(last), averageMoney(prev))
      }) match {
        case (Some(lastAverage), None) => lastAverage
        case (Some(lastAverage), Some(prevAverage)) => (lastAverage + prevAverage) / 2.0
        case _ => 0.0
      }
      ServerModel.normalizeScore(score, 0, 10)
    }
  }

  lazy val satisfaction = {
    if (accumulatedYears == 0) 3
    else {
      // Frontend hat Wertebereich 0-6
      // Satisfaction nur für letzte Periode
      val (lastPeriod, _) = answeredQuestions.partition(_.ageHigh == maxYear)
      val score = averageSatisfaction(lastPeriod).getOrElse(5.0) * 7.0 / 11.0
      ServerModel.normalizeScore(score, 0, 6)
    }
  }

  private def averageMoney(answers: Seq[AnsweredQuestion]) = if (answers.isEmpty) None else {
    Some(answers.map(_.moneyScore).sum / answers.map(_.years).sum.toDouble)
  }

  private def averageSatisfaction(answers: Seq[AnsweredQuestion]) = if (answers.isEmpty) None else {
    Some(answers.map(_.satisfactionScore).sum / answers.map(_.years).sum.toDouble)
  }

  def highScore = {
    answeredQuestions.map(_.satisfactionScore).sum
  }

  def determineName = answeredQuestions.find(_.question.preview.contains("Name")) match {
    case None => None
    case Some(aq) => Some(aq.answer.text)
  }

  def external = client.GameState(gameId, age, money, satisfaction, determineName, Nil, endOfGame.map(_.external))

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