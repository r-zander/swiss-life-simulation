package controllers

// swagger imports
import javax.ws.rs.PathParam

import com.wordnik.swagger.annotations._
import models._
import modules.Db
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

@Api(value = "/game", description = "Game micro-service")
class GameController extends Controller with ClientModel {
	val logger = Logger("application.GameController")

	@ApiOperation(
		nickname = "startNewGame",
		value = "Starts a new game",
		notes = "Returns a GameState",
		httpMethod = "GET")
	def startNewGame() = Action {
		val gameState = Db.newGame
		Ok(Json.toJson(gameState.external))
	}

}