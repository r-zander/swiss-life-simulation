package controllers

// swagger imports
import com.wordnik.swagger.annotations._
import modules.{Rand, Db}

import play.api._
import play.api.mvc._
import play.api.libs.json._

import models.{server, client}
import javax.ws.rs.PathParam

import scala.annotation.tailrec
import scala.util.{Success, Failure}

@Api(value = "/questions", description = " Service to retrieve questions")
class QuestionController extends Controller with client.ClientModel {
  val logger = Logger("application.QuestionController")

	@ApiOperation(
		nickname = "questions",
		value = "Get the next questions in the game",
		notes = "Returns 3 questions, one for each topic (career, leisure, family)",
		httpMethod = "GET")
	def getQuestions(@ApiParam(value = "gameId of the game being played") @PathParam("gameId") gameId: String) = Action {
		getQuestionsForGame(gameId) match {
			case Failure(ex) =>
        logger.error(s"GetQuestions($gameId) failed.", ex)
        NotFound(Json.toJson(client.ClientError(ex.toString)))
			case Success(questions) =>
        Ok(Json.toJson(questions))
		}
	}

  def filterAnswers(gameState: server.GameState, questions: Seq[server.Question]) = {
    val allRefs = gameState.answeredQuestions.flatMap(_.answer.flatRefs).toSet
    def filterQuestion(question: server.Question) = {
      val answers = question.answers.filter(_ checkPrecondition allRefs)
      if (answers.isEmpty) {
        sys.error(s"Keine Antworten für ${question} gefunden !")
      }
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
	private def selectQuestions(topics: List[String],
                              selected: Seq[(String, server.Question)],
                              available: Seq[server.Question]): Seq[(String, server.Question)] = {
    topics match {
      case Nil => selected
      case t :: ts =>
        val questions = available.filter(_.topics contains t)
        val q = Rand.selectRandom(questions, s"Question für Topic '$t'")
        selectQuestions(ts, selected :+ (t -> q), available.filterNot(_ == q))
    }
  }

}