package models

import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.json.JsObject
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure,Success}
import wabisabi.Client

object SearchModel {

  val esURL = "http://localhost:9200"
  Logger.debug("Trying to connect to " + esURL)
  val esClient = new Client(esURL)

  val indexSettings = "{\"settings\": { \"index\": { \"number_of_shards\": 1 } } }"
  val indexName = "events"
  val ttype = "event"
  val mapping = """
  {
    "event": {
      "properties": {
        "service": {
          "type": "string",
          "index": "not_analyzed"
        },
        "source": {
          "type": "string",
          "index": "not_analyzed"
        },
        "etype": {
          "type": "string",
          "index": "not_analyzed"
        },
        "content": {
          "type": "string",
          "index": "not_analyzed"
        },
        "url": {
          "type": "string",
          "index": "not_analyzed"
        },
        "date_begun": {
          "type": "date",
          "format": "basic_date_time_no_millis"
        },
        "date_ended": {
          "type": "date",
          "format": "basic_date_time_no_millis"
        }
      }
    }
  }
  """

  def checkIndices = {

    Logger.debug("Creating index")
    esClient.verifyIndex(name = indexName) onComplete { maybeRes =>
      maybeRes match {
        case Success(res) => {
          if(res.getStatusCode == 404) {
            // If the verify fails, that means it doesn't exist. We don't care about
            // success because that means the index is there.
            esClient.createIndex(name = indexName, settings = Some(indexSettings)) map { f =>
              esClient.health(indices = Seq(indexName), waitForNodes = Some("1")) // XXX Number of shards should be configurable
            } map { f =>
              esClient.putMapping(indices = Seq(indexName), `type` = ttype, body = mapping)
            } recover {
              case x: Throwable => {
                Logger.error("Failed to create index")
                // Rethrow!
                throw x
              }
            }
          }
        }
        case Failure(res) => throw new RuntimeException("Problem creating index")
      }
    }

    // Verify we got all the indices created
    Await.result(esClient.health(indices = Seq(indexName), waitForNodes = Some("1")), Duration(5, "seconds"))
  }

  def indexEvent(event: JsObject, block: Boolean = false) = {
    esClient.index(index = indexName, `type` = ttype, data = event.toString, refresh = block)
  }

  def searchEvent(filters: Map[String,Seq[String]]) = {

    val actualFilters: Seq[JsObject] = filters.flatMap({ kv =>
      kv._2.map({ v =>
        Json.obj("term" -> Json.obj(kv._1 -> v))
      })
    }).toSeq

    val allQuery = Json.obj(
      "match_all" -> Json.obj()
    )

    val actualQuery = if(filters.isEmpty) {
      allQuery
    } else {
      Json.obj(
        "filtered" -> Json.obj(
          "query" -> allQuery,
          "filter" -> Json.obj("bool" -> Json.obj(
            "must" -> actualFilters
          ))
        )
      )
    }

    val finalSearch = Json.obj(
      "query" -> actualQuery,
      "sort" -> Json.arr(
        Json.obj("date_begun" -> Json.obj("order" -> "asc"))
      )
    )

    Logger.debug("Sending query to ES")
    Logger.debug(Json.prettyPrint(finalSearch))

    esClient.search(index = indexName, query = finalSearch.toString)
  }
}