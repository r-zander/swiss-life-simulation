package controllers

import javax.inject._
import play.api._
import play.api.mvc._


class Application extends Controller {

	def swagger = Action { request =>
		Ok(views.html.swagger())
	}


	def pushToMailStream = Action {
		//publisher.publish("ms.backend.stream.mail", "this is for mail stream.")
		Ok
	}

	def pushToLogStream = Action {
		//publisher.publish("ms.backend.stream.log", "this is for log stream.")
		Ok
	}
}
