package com.itv.chuckwagon.sbt

import fansi.Color.{Cyan, Green}
import fansi.Str

object LoggingUtils {


  def logItemsMessage(prefix: String, items: String*): String = {

    val colouredItems =
      if (items.isEmpty) ""
      else
        items
          .map(Green(_).render)
          .mkString(Str("'").render, Str("', '").render, Str("'").render)

    (Cyan("Chuckwagon") ++ Str(s": $prefix ")).render ++ colouredItems
  }

  def logMessage(message: String): String = {
    (Cyan("Chuckwagon") ++ Str(s": ")).render ++ message
  }
}
