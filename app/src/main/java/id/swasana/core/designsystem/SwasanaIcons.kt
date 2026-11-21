package id.swasana.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Ikon garis 24×24 yang diambil dari path SVG pada prototipe desain. Warna diatur lewat tint
 * pada `Icon`, jadi semua ikon ikut tema terang/gelap.
 */
object SwasanaIcons {

  private data class Seg(val d: String, val filled: Boolean = false)

  private fun circle(cx: Double, cy: Double, r: Double) =
    "M${cx - r},$cy a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

  private fun rrect(x: Double, y: Double, w: Double, h: Double, r: Double) =
    "M${x + r},$y h${w - 2 * r} a$r,$r 0 0 1 $r,$r v${h - 2 * r} a$r,$r 0 0 1 ${-r},$r " +
      "h${-(w - 2 * r)} a$r,$r 0 0 1 ${-r},${-r} v${-(h - 2 * r)} a$r,$r 0 0 1 $r,${-r} z"

  private fun build(name: String, stroke: Float, vararg segs: Seg): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
      )
      .apply {
        for (seg in segs) {
          val nodes = PathParser().parsePathString(seg.d).toNodes()
          if (seg.filled) {
            addPath(pathData = nodes, fill = SolidColor(Color.Black))
          } else {
            addPath(
              pathData = nodes,
              stroke = SolidColor(Color.Black),
              strokeLineWidth = stroke,
              strokeLineCap = StrokeCap.Round,
              strokeLineJoin = StrokeJoin.Round,
            )
          }
        }
      }
      .build()

  private fun icon(name: String, vararg paths: String, stroke: Float = 2f) =
    build(name, stroke, *paths.map { Seg(it) }.toTypedArray())

  /** Logo "angin" Swasana. */
  val Logo by lazy {
    icon("logo", "M3 8h11a3 3 0 1 0-3-3", "M3 12h16a3 3 0 1 1-3 3", "M3 16h7")
  }

  val Radar by lazy {
    build(
      "radar",
      1.7f,
      Seg(circle(12.0, 12.0, 9.0)),
      Seg(circle(12.0, 12.0, 5.0)),
      Seg("M12 12l6-6"),
      Seg(circle(12.0, 12.0, 1.2), filled = true),
    )
  }

  val Lungs by lazy {
    icon(
      "lungs",
      "M12 4v8",
      "M12 11c-1.2 1.2-2.4 1.8-3.6 2",
      "M12 11c1.2 1.2 2.4 1.8 3.6 2",
      "M8.6 6.5C6 6.5 4 10.4 4 15c0 3 1.1 5 3.1 5S10.4 18.4 10.4 16V8.5",
      "M15.4 6.5C18 6.5 20 10.4 20 15c0 3-1.1 5-3.1 5s-3.3-1.6-3.3-4V8.5",
      stroke = 1.8f,
    )
  }

  val Journal by lazy {
    icon("journal", rrect(5.0, 3.0, 14.0, 18.0, 2.5), "M9 8h6M9 12h6M9 16h3.5")
  }

  val Pin by lazy {
    icon("pin", "M12 21s-7-6.2-7-11.5A7 7 0 0 1 19 9.5C19 14.8 12 21 12 21z", circle(12.0, 9.5, 2.5))
  }

  val Home by lazy {
    icon("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z")
  }

  val Bell by lazy { icon("bell", "M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z", "M10 20a2 2 0 0 0 4 0") }

  val ShieldCheck by lazy {
    icon("shield_check", "M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6z", "M9 12l2 2 4-4")
  }

  val Megaphone by lazy { icon("megaphone", "M4 10v4h3l6 4V6L7 10z", "M17 9a4 4 0 0 1 0 6") }

  val Search by lazy { icon("search", circle(11.0, 11.0, 6.5), "M20 20l-4-4") }

  val Back by lazy { icon("back", "M15 5l-7 7 7 7") }

  val ChevronRight by lazy { icon("chevron_right", "M9 5l7 7-7 7") }

  val ArrowRight by lazy { icon("arrow_right", "M5 12h14M13 6l6 6-6 6") }

  val Check by lazy { icon("check", "M5 12.5l4.5 4.5L19 7.5", stroke = 2.6f) }

  val Plus by lazy { icon("plus", "M12 5v14M5 12h14") }

  val Minus by lazy { icon("minus", "M5 12h14") }

  val Close by lazy { icon("close", "M6 6l12 12M18 6L6 18") }

  val Camera by lazy { icon("camera", "M4 8h3l2-2.5h6L17 8h3v11H4z", circle(12.0, 13.0, 3.5)) }

  val Locate by lazy {
    icon("locate", circle(12.0, 12.0, 3.0), "M12 2v3M12 19v3M2 12h3M19 12h3")
  }

  val Calendar by lazy {
    icon("calendar", rrect(3.5, 5.0, 17.0, 15.0, 2.5), "M3.5 10h17M8 3v4M16 3v4")
  }

  val Mask by lazy {
    icon("mask", "M4 9c3 0 5-2 8-2s5 2 8 2v3c0 4-4 6-8 6s-8-2-8-6z", "M8 12h8M9 15h6")
  }

  val Sliders by lazy {
    icon(
      "sliders",
      "M4 7h9M17 7h3M4 17h3M11 17h9",
      circle(15.0, 7.0, 2.0),
      circle(9.0, 17.0, 2.0),
    )
  }

  val Download by lazy { icon("download", "M12 4v11M7 11l5 5 5-5M5 20h14") }

  val Phone by lazy {
    icon("phone", "M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2z")
  }

  val Warning by lazy { icon("warning", "M12 3l10 18H2z", "M12 10v5", "M12 18v.5") }

  val Person by lazy {
    icon("person", circle(12.0, 8.0, 4.0), "M4 21a8 8 0 0 1 16 0")
  }

  val Trash by lazy { icon("trash", "M4 7h16M9 7V4h6v3M6 7l1 13h10l1-13") }

  val Lock by lazy { icon("lock", rrect(5.0, 11.0, 14.0, 10.0, 2.5), "M8 11V8a4 4 0 0 1 8 0v3") }

  val Info by lazy { icon("info", circle(12.0, 12.0, 9.0), "M12 11v6", "M12 7.5v.5") }

  val Clock by lazy { icon("clock", circle(12.0, 12.0, 9.0), "M12 7v5l3 2") }

  val Admin by lazy {
    icon("admin", "M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6z", "M12 8v5", "M12 15.5v.5")
  }
}
