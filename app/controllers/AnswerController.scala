package controllers

// swagger imports
import javax.ws.rs.PathParam
import com.wordnik.swagger.annotations._

import models._
import modules.{Rand, Db}
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.util.{Failure, Random, Success}

@Api(value = "/game/{gameId}/answers", description = "Answers micro-services")
class AnswerController extends Controller with ClientModel {
	val logger = Logger("application.AnswerController")

	val RND = new Random

	@ApiOperation(
		nickname = "submitAnswer",
		value = "Get the next questions",
		notes = "Returns 3 questions",
		httpMethod = "GET")
	def submitAnswer(
										@ApiParam(value = "gameId of the game to play") @PathParam("gameId") gameId: String,
										@ApiParam(value = "answerId of the user answer") @PathParam("answerId") answerId: String
									) = Action {
		writeAnswer(gameId, answerId) match {
			case Failure(ex) =>
				logger.error(s"SubmitAnswer($gameId,$answerId) failed.", ex)
				NotFound(Json.toJson(ClientError(ex.toString)))
			case Success(questions) => Ok(Json.toJson(questions))
		}
	}


	def writeAnswer(gameId: String, answerId: String) = validateAnswer(gameId, answerId) map {
		case (gameState, answer, answeredQuestion, unansweredQuestions) =>
		  val pairedAnswers = (answeredQuestion, answer) +: autoAnswer(gameState, unansweredQuestions)
			val newGameState = updateGame(gameState, pairedAnswers)
			newGameState.external.copy(lastAnswers = pairedAnswers map summarize)
	}

	def summarize(pairedAnswers: (HiddenQuestion, HiddenAnswer)): ClientAnsweredQuestion = {
		ClientAnsweredQuestion(pairedAnswers._1.text, pairedAnswers._2.text)
	}

	def validateAnswer(gameId: String, answerId: String) = Db.gameState(gameId) flatMap { gameState =>
		Db.answer(answerId) map { answer =>
			val (answeredQuestion, unansweredQuestions) = splitQuestions(gameState, answer)
			(gameState, answer, answeredQuestion, unansweredQuestions)
		}
	}

	def splitQuestions(gameState: HiddenGameState, answer: HiddenAnswer) = {
		logger.info(s"Open Questions for Game ${gameState.gameId} : ${gameState.openQuestions}")
		gameState.openQuestions.partition(_.answers contains answer) match {
			case (answered, _) if answered.isEmpty => sys.error(s"Ungültige Antwort ${answer.id}")
			case (Seq(answered), unanswered) => answered -> unanswered
		}
	}

	def autoAnswer(gameState: HiddenGameState, questions: Seq[HiddenQuestion]): Seq[(HiddenQuestion, HiddenAnswer)] = {
		questions map { autoAnswer(gameState, _) }
	}

	def autoAnswer(gameState: HiddenGameState, question: HiddenQuestion): (HiddenQuestion, HiddenAnswer) = {
		def weight(answer: HiddenAnswer) = answer.probability
		val answer = Rand.selectRandom(question.answers, weight, s"Answer")
		question -> answer
	}

	def updateGame(gameState: HiddenGameState, pairedAnswers: Seq[(HiddenQuestion, HiddenAnswer)]) = {
		val nextAge = Db.nextAge(gameState.age)
		val answers = pairedAnswers.map { case (q,a) =>
			AnsweredQuestion(gameState.age, nextAge, q, a)
		}
		val nextGameState = checkEndOfGame(gameState.copy(
			age = nextAge,
			openQuestions = Nil,
			answeredQuestions = gameState.answeredQuestions ++ answers
		))
		Db.updateGameState(nextGameState)
		nextGameState
	}

	def checkEndOfGame(gameState: HiddenGameState): HiddenGameState = {
		def weight(riskFactor: RiskFactor) = riskFactor.value
		val riskFactors = gameState.allRiskFactors.toList
		val psum = riskFactors.map(weight).sum
		val notDead = RiskFactor("DUMMY", math.max(0.5, 1 - psum))
		Rand.selectRandom((notDead :: riskFactors).reverse, weight, s"RiskFactor") match {
			case rf if rf == notDead => gameState
			case RiskFactor(cause, prob) =>
				val endOfGame = EndOfGame(cause, prob, gameState.highScore)
				gameState.copy(endOfGame = Some(endOfGame))
		}
	}

}