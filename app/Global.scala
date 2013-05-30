import models.SearchModel
import play.api.{Application,GlobalSettings}

object Global extends GlobalSettings {

  override def onStart(app: Application) {
    SearchModel.checkIndices
  }
}