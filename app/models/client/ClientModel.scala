package models.client

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class ClientError(error: String)

case class Question(topic: String,
                    preview: Option[String],
                    text: String,
                    ages: Seq[Int],
                    answers: Seq[Answer])

case class Answer(id: String,
                  text: String)

case class AnsweredQuestion(question: String,
                            answer: String)

case class EndOfGame(causeOfDeath: String,
                     probability: String,
                     highScore: Int)

case class GameState(gameId: String,
                     age: Int,
                     money: Int,
                     satisfaction: Int,
                     name: Option[String],
                     lastAnswers: Seq[AnsweredQuestion],
                     endOfGame: Option[EndOfGame])

trait ClientModel {
  implicit val ClientErrorFormat = Json.format[ClientError]
  implicit val ClientEndOfGameFormat = Json.format[EndOfGame]
  implicit val AnswerFormat = Json.format[Answer]
  implicit val QuestionFormat = Json.format[Question]
  implicit val ClientAnsweredQuestionFormat = Json.format[AnsweredQuestion]
  implicit val GameStateFormat = Json.format[GameState]
}