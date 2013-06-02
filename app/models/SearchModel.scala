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

  case class Query(
    page: Int = 0,
    count: Int = 20,
    filters: Map[String,Seq[String]] = Map.empty
  )

  val esURL = "http://localhost:9200"
  Logger.debug("Trying to connect to " + esURL)
  val esClient = new Client(esURL)

  val indexSettings = "{\"settings\": { \"index\": { \"number_of_shards\": 1 } } }"
  val indexName = "honeybird"
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
        "user": {
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

  def searchEvent(query: Query) = {

    val actualFilters: Seq[JsObject] = query.filters.flatMap({ kv =>
      kv._2.map({ v =>
        if(kv._1.equals("date_begun")) {
          Json.obj("range" -> Json.obj(
            "date_begun" -> Json.obj(
              "gte" -> v
            )
          ))
        } else if(kv._1.equals("date_ended")) {
          Json.obj("range" -> Json.obj(
            "date_begun" -> Json.obj(
              "lte" -> v
            )
          ))
        } else {
          Json.obj("term" -> Json.obj(kv._1 -> v))
        }
      })
    }).toSeq

    val allQuery = Json.obj(
      "match_all" -> Json.obj()
    )

    val actualQuery = if(actualFilters.isEmpty) {
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
      "from"  -> query.page,
      "size"  -> query.count,
      "query" -> actualQuery,
      "sort" -> Json.arr(
        Json.obj("date_begun" -> Json.obj("order" -> "desc"))
      ),
      "facets" -> Json.obj(
        "service" -> Json.obj(
          "terms" -> Json.obj(
            "field" -> "service"
          )
        ),
        "source" -> Json.obj(
          "terms" -> Json.obj(
            "field" -> "source"
          )
        ),
        "etype" -> Json.obj(
          "terms" -> Json.obj(
            "field" -> "etype"
          )
        ),
        "user" -> Json.obj(
          "terms" -> Json.obj(
            "field" -> "user"
          )
        )
      )
    )

    Logger.debug("Sending query to ES")
    Logger.debug(Json.prettyPrint(finalSearch))

    esClient.search(index = indexName, query = finalSearch.toString)
  }
}