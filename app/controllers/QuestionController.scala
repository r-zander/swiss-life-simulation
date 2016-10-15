package controllers

// swagger imports
import com.wordnik.swagger.annotations._
import modules.{Rand, Db}

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models._
import javax.ws.rs.PathParam

import scala.annotation.tailrec
import scala.util.{Success, Failure, Random}

@Api(value = "/game/{gameid}/questions", description = "Questions micro-service")
class QuestionController extends Controller with ClientModel {
  val logger = Logger("application.QuestionController")

	val RND = new Random

	@ApiOperation(
		nickname = "getQuestions",
		value = "Get the next questions",
		notes = "Returns 3 questions",
		httpMethod = "GET")
	def getQuestions(@ApiParam(value = "gameId of the game to fetch") @PathParam("gameId") gameId: String) = Action {
		getQuestionsForGame(gameId) match {
			case Failure(ex) =>
        logger.error(s"GetQuestions($gameId) failed.", ex)
        NotFound(Json.toJson(ClientError(ex.toString)))
			case Success(questions) =>
        Ok(Json.toJson(questions))
		}
	}

  def filterAnswers(gameState: HiddenGameState, questions: Seq[HiddenQuestion]) = {
    val allRefs = gameState.answeredQuestions.flatMap(_.answer.flatRefs).toSet
    def filterQuestion(question: HiddenQuestion) = {
      val answers = question.answers.filter(_ hasRefs allRefs)
      question.copy(answers = answers)
    }
    questions map filterQuestion
  }

  def getQuestionsForGame(gameId: String) = Db.gameState(gameId) map { gameState =>
		val allQuestions = Db.config.questions.filter{
			_ hasAge gameState.age
		}
		val (topics, questions) = selectQuestions(Db.config.topics, Nil, allQuestions).unzip
    val filteredQuestions = filterAnswers(gameState, questions)
    val newGameState = gameState.copy(openQuestions = filteredQuestions)
    Db.updateGameState(newGameState)
    (topics zip filteredQuestions) map { case (t, q) => q.external(t) }
	}

	@tailrec
	private def selectQuestions(topics: List[String], selected: Seq[(String, HiddenQuestion)], available: Seq[HiddenQuestion]): Seq[(String, HiddenQuestion)] = {
    topics match {
      case Nil => selected
      case t :: ts =>
        val questions = available.filter(_.topics contains t)
        val q = Rand.selectRandom(questions, s"Question für Topic '$t'")
        selectQuestions(ts, selected :+ (t -> q), available.filterNot(_ == q))
    }
  }

}