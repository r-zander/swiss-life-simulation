package modules

import java.nio.charset.StandardCharsets
import java.nio.file.{Paths, Files}
import scala.collection.JavaConversions._

import models._
import play.api.libs.json.Json

import scala.util._

object Db extends ServerModel {


  private val _initialGameState = HiddenGameState(null, 0, Nil, Nil)
  private val _gameState = collection.concurrent.TrieMap.empty[String, HiddenGameState]
  private val _config = readConfig

  def config = _config

  def newGame = {
    val gameId = ServerModel.generateId
    val gameState = _initialGameState.copy(gameId = gameId)
    _gameState(gameId) = gameState
    gameState
  }

  def gameState(gameid: String) = Try(_gameState(gameid))

  def answer(answerId: String) = Try {
    config.questions flatMap (_.answers) find (_.id == answerId) getOrElse {
      throw new RuntimeException(s"Answer with id '$answerId' not found")
    }
  }

  def nextAge(age: Int) = config.ages.dropWhile(_ <= age).headOption.getOrElse(age + 10)

  def updateGameState(newGameState: HiddenGameState) = {
    _gameState(newGameState.gameId) = newGameState
  }

  private def readConfig: SimulatorConfig = {
    val url = classOf[Db].getClassLoader.getResource("questions.json")
    val path = Paths.get(url.toURI)
    val lines: Seq[String] = Files.readAllLines(path, StandardCharsets.UTF_8)
    val json = Json parse lines.mkString
    json.as[SimulatorConfig]
  }
}

class Db
