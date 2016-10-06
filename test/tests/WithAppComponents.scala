package tests

import com.malliina.pimpcloud.CloudComponents
import play.api.ApplicationLoader.Context

trait WithAppComponents extends WithComponents[CloudComponents] {
  override def createComponents(context: Context): CloudComponents = new CloudComponents(context)
}
