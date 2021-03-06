package controllers

import models.SearchModel
import play.api._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.mvc._

import scala.concurrent.Await
import scala.concurrent.duration._

object Application extends Controller {

  val emptyObj = __.json.put(Json.obj())
  val validateEvent: Reads[JsObject] = (
    (__ \ 'service ).json.pickBranch and
    ((__ \ 'source).json.pickBranch orElse emptyObj) and
    (__ \ 'etype).json.pickBranch and
    (__ \ 'content).json.pickBranch and
    (__ \ 'user).json.pickBranch and
    ((__ \ 'url).json.pickBranch orElse emptyObj) and
    (__ \ 'date_begun).json.pickBranch and
    ((__ \ 'date_ended).json.pickBranch orElse emptyObj)
  ).reduce

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def search(page: Int = 0, count: Int = 20) = Action { request =>
    val params = Seq("service", "source", "etype", "date_begun", "date_ended", "user")

    val filters = request.queryString filterKeys { key => params.contains(key) }

    val query = SearchModel.Query(
      page = page,
      count = count,
      filters = filters
    )

    val res = SearchModel.searchEvent(query)
    val response = Await.result(res, Duration(1, "seconds")).getResponseBody
    Ok(response)
  }

  def store = Action(parse.json) { request =>
    request.body.transform(validateEvent).map({ jsobj =>

      val s = SearchModel.indexEvent(jsobj)
      Logger.debug(Await.result(s, Duration(1, "second")).getResponseBody)
      Ok(jsobj)
    }).recoverTotal({
      e => BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toFlatJson(e)))
    })
  }
}