package id.swasana.domain.usecase

import kotlin.math.sqrt

object StatsUtil {
  /** Korelasi Pearson; null bila data kurang dari 2 titik atau salah satu variabel konstan. */
  fun pearson(xs: List<Double>, ys: List<Double>): Double? {
    require(xs.size == ys.size) { "Ukuran data tidak sama" }
    val n = xs.size
    if (n < 2) return null
    val mx = xs.average()
    val my = ys.average()
    var sxy = 0.0
    var sxx = 0.0
    var syy = 0.0
    for (i in 0 until n) {
      val dx = xs[i] - mx
      val dy = ys[i] - my
      sxy += dx * dy
      sxx += dx * dx
      syy += dy * dy
    }
    if (sxx == 0.0 || syy == 0.0) return null
    return (sxy / sqrt(sxx * syy)).coerceIn(-1.0, 1.0)
  }
}
