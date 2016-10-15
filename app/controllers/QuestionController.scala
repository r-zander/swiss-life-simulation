package controllers

// swagger imports
import com.wordnik.swagger.annotations._
import modules.{Rand, Db}

import play.api._
import play.api.mvc._
import play.api.Play.current
import play.api.mvc.Results._
import play.api.libs.json._

import models._
import javax.ws.rs.{QueryParam, PathParam}

import scala.annotation.tailrec
import scala.util.{Success, Failure, Random}

@Api(value = "/game/<gameid>/questions", description = "Questions micro-service")
class QuestionController extends Controller with ClientModel {

	val RND = new Random

	/*@ApiOperation(
		nickname = "getUserById",
		value = "Find a user by userid",
		notes = "Returns a user",
		httpMethod = "GET")
	@ApiResponses(Array(
		new ApiResponse(code = 200, message = "LGTM"),
	    new ApiResponse(code = 400, message = "Invalid ID supplied"),
	    new ApiResponse(code = 404, message = "User not found"),
	    new ApiResponse(code = 500, message = "Internal server error")
	))
	def getQuestions(
		@ApiParam(value = "id of the game to fetch") @PathParam("id") gameId: String) = Action {
		var retUserOpt = None: Option[User]

		if (retUserOpt.isDefined) {
			Ok(Json.toJson(retUserOpt.get))
		} else {
			InternalServerError
		}
	}*/
	@ApiOperation(
		nickname = "getQuestions",
		value = "Get the next questions",
		notes = "Returns 3 questions",
		httpMethod = "GET")
	def getQuestions(@ApiParam(value = "gameId of the game to fetch") @PathParam("gameId") gameId: String) = Action {
		getQuestionsForGame(gameId) match {
			case Failure(ex) => NotFound(Json.toJson(ClientError(ex.toString)))
			case Success(questions) => Ok(Json.toJson(questions))
		}
	}

	def getQuestionsForGame(gameId: String) = Db.gameState(gameId) map { gameState =>
		val allQuestions = Db.config.questions.filter{
			_ hasAge gameState.age
		}
		selectQuestions(Db.config.topics, Nil, allQuestions)
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