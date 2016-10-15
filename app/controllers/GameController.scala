package controllers

// swagger imports
import javax.ws.rs.PathParam

import com.wordnik.swagger.annotations._
import models._
import modules.Db
import play.api.libs.json._
import play.api.mvc._

@Api(value = "/game", description = "Game micro-service")
class GameController extends Controller with ClientModel {

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
		nickname = "startNewGame",
		value = "Starts a new game",
		notes = "Returns a GameState",
		httpMethod = "GET")
	def startNewGame() = Action {
		val gameState = Db.newGame
		Ok(Json.toJson(gameState.external))
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