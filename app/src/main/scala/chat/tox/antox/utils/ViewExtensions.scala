package chat.tox.antox.utils

import android.view.View

object ViewExtensions {

  implicit class RichView(val view: View) extends AnyVal {
    def getLocationOnScreen(): LocationJ = {
      val rawLocation = Array.ofDim[Int](2)
      view.getLocationOnScreen(rawLocation)
      new LocationJ(rawLocation(0), rawLocation(1));
    }

    def getCenterLocationOnScreen(): LocationJ = {
      val upperLocation = getLocationOnScreen()
      new LocationJ(upperLocation.x + view.getWidth / 2, upperLocation.y + view.getHeight / 2);
    }
  }

}
