package controllers

// swagger imports
import javax.ws.rs.PathParam

import com.wordnik.swagger.annotations._
import models._
import modules.{Rand, Db}
import play.api.libs.json._
import play.api.mvc._

import scala.annotation.tailrec
import scala.util.{Failure, Random, Success}

@Api(value = "/game/<gameId>/answer/<answerId>", description = "Answers micro-services")
class AnswerController extends Controller with ClientModel {

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
			case Failure(ex) => NotFound(Json.toJson(ClientError(ex.toString)))
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
		val answer = Rand.selectRandom(question.answers, weight)
		question -> answer
	}

	def updateGame(gameState: HiddenGameState, pairedAnswers: Seq[(HiddenQuestion, HiddenAnswer)]) = {
		val nextAge = Db.nextAge(gameState.age)
		val answers = pairedAnswers.map { case (q,a) =>
			AnsweredQuestion(gameState.age, nextAge, q, a)
		}
		val nextGameState = gameState.copy(
			age = nextAge,
			openQuestions = Nil,
			answeredQuestions = gameState.answeredQuestions ++ answers
		)
		Db.updateGameState(nextGameState)
		nextGameState
	}

	@tailrec
	private def selectQuestions(topics: List[String], selected: Seq[Question], available: Seq[HiddenQuestion]): Seq[Question] = topics match {
		case Nil => selected
		case t :: ts =>
			val questions = available.filter(_.topics contains t)
			val qInt = Rand.selectRandom(questions)
			val qExt = qInt.external(t)
			selectQuestions(ts, selected :+ qExt, available.filterNot(_ == qInt))
	}


	/*
	@ApiOperation(
		nickname = "initUser",
		value = "Init a user",
		notes = "Init a user and return id",
		httpMethod = "PUT")
	@ApiResponses(Array(
		new ApiResponse(code = 200, message = "LGTM"),
		new ApiResponse(code = 400, message = "Invalid params"),
		new ApiResponse(code = 405, message = "User exists"),
		new ApiResponse(code = 500, message = "Internal server error")
	))
	@ApiImplicitParams(Array(
		new ApiImplicitParam (
			name = "email", value = "email of the user to init", 
			required = true, dataType = "String", paramType = "body"),
		new ApiImplicitParam (
			name = "sex", value = "sex of the user to init", 
			required = true, dataType = "String", paramType = "body"),
		new ApiImplicitParam (
			name = "passwd", value = "password the user input", 
			required = true, dataType = "String", paramType = "body")
	))
	def initUser = Action { request =>
		var retUserOpt = None: Option[User]

		if (retUserOpt.isDefined) {
			Ok(Json.toJson(retUserOpt.get))
		} else {
			InternalServerError
		}
	}
	*/
}