import com.mle.file.FileUtilities
import com.mle.pimpcloud.db.CloudDatabase
import com.mle.util.Log
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.gzip.GzipFilter

/**
 * @author Michael
 */
object Global extends WithFilters(new GzipFilter()) with Log {
  override def onStart(app: Application): Unit = {
    super.onStart(app)
    FileUtilities init "pimpstream"
  }

  override def onStop(app: Application): Unit = {
    CloudDatabase.default.close()
    super.onStop(app)
  }
}
