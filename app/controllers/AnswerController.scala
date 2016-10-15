package controllers

// swagger imports
import javax.ws.rs.PathParam
import com.wordnik.swagger.annotations._

import models.{client, server}
import modules.{Rand, Db}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Random, Success}

@Api(value = "/answers", description = "Answers micro-services")
class AnswerController extends Controller with client.ClientModel {
	val logger = Logger("application.AnswerController")

	val RND = new Random

	@ApiOperation(
		nickname = "answer",
		value = "Submits an user answer and obtains the result",
		notes = "Simulates the next turn and returns the new game state. If ",
		httpMethod = "GET")
	def submitAnswer( @ApiParam(value = "gameId of the game being played") @PathParam("gameId") gameId: String,
										@ApiParam(value = "answerId of the answer given by the user") @PathParam("answerId") answerId: String
									) = Action {
		writeAnswer(gameId, answerId) match {
			case Failure(ex) =>
				logger.error(s"SubmitAnswer($gameId,$answerId) failed.", ex)
				NotFound(Json.toJson(client.ClientError(ex.toString)))
			case Success(questions) => Ok(Json.toJson(questions))
		}
	}

	def writeAnswer(gameId: String, answerId: String) = validateAnswer(gameId, answerId) map {
		case (gameState, answer, answeredQuestion, unansweredQuestions) =>
		  val pairedAnswers = (answeredQuestion, answer) +: autoAnswer(gameState, unansweredQuestions)
			val newGameState = updateGame(gameState, pairedAnswers)
			newGameState.external.copy(lastAnswers = pairedAnswers map summarize)
	}

	def summarize(pairedAnswers: (server.Question, server.Answer)): client.AnsweredQuestion = {
    client.AnsweredQuestion(pairedAnswers._1.text, pairedAnswers._2.text)
	}

	def validateAnswer(gameId: String, answerId: String) = Db.gameState(gameId) flatMap { gameState =>
		Db.answer(answerId) map { answer =>
			val (answeredQuestion, unansweredQuestions) = splitQuestions(gameState, answer)
			(gameState, answer, answeredQuestion, unansweredQuestions)
		}
	}

	def splitQuestions(gameState: server.GameState, answer: server.Answer) = {
		logger.info(s"Open Questions for Game ${gameState.gameId} : ${gameState.openQuestions}")
		gameState.openQuestions.partition(_.answers contains answer) match {
			case (answered, _) if answered.isEmpty => sys.error(s"Ungültige Antwort ${answer.id}")
			case (Seq(answered), unanswered) => answered -> unanswered
		}
	}

	def autoAnswer(gameState: server.GameState, questions: Seq[server.Question]): Seq[(server.Question, server.Answer)] = {
		questions map { autoAnswer(gameState, _) }
	}

	def autoAnswer(gameState: server.GameState, question: server.Question): (server.Question, server.Answer) = {
		def weight(answer: server.Answer) = answer.probability
		val answer = Rand.selectRandom(question.answers, weight, s"Answer")
		question -> answer
	}

	def updateGame(gameState: server.GameState, pairedAnswers: Seq[(server.Question, server.Answer)]) = {
		val nextAge = Db.nextAge(gameState.age)
		val answers = pairedAnswers.map { case (q,a) =>
      server.AnsweredQuestion(gameState.age, nextAge, q, a)
		}
		val nextGameState = checkEndOfGame(gameState.copy(
			age = nextAge,
			openQuestions = Nil,
			answeredQuestions = gameState.answeredQuestions ++ answers
		))
		Db.updateGameState(nextGameState)
		nextGameState
	}

	def checkEndOfGame(gameState: server.GameState): server.GameState = {
		def weight(riskFactor: server.RiskFactor) = riskFactor.value
		val riskFactors = gameState.allRiskFactors.toList
		val psum = riskFactors.map(weight).sum
		val notDead = server.RiskFactor("DUMMY", math.max(0.5, 1 - psum))
		Rand.selectRandom((notDead :: riskFactors).reverse, weight, s"RiskFactor") match {
			case rf if rf == notDead => gameState
			case server.RiskFactor(cause, prob) =>
				val endOfGame = server.EndOfGame(cause, prob, gameState.highScore)
				gameState.copy(endOfGame = Some(endOfGame))
		}
	}

}