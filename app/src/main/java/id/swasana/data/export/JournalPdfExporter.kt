package id.swasana.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import id.swasana.R
import id.swasana.core.util.Formatters
import id.swasana.domain.model.HealthJournalEntry
import id.swasana.domain.model.SymptomType
import id.swasana.domain.usecase.InsightKind
import id.swasana.domain.usecase.JournalInsight
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Mengekspor ringkasan jurnal ke PDF (A4) untuk dibawa ke dokter. */
@Singleton
class JournalPdfExporter @Inject constructor(@ApplicationContext private val context: Context) {

  suspend fun export(
    entries: List<HealthJournalEntry>,
    insight: JournalInsight?,
    userName: String,
  ): Uri =
    withContext(Dispatchers.IO) {
      val dir = File(context.cacheDir, "exports").apply { mkdirs() }
      // Bersihkan ekspor lama agar cache tidak menumpuk.
      dir.listFiles()?.filter { it.extension == "pdf" }?.forEach { it.delete() }
      val file = File(dir, "jurnal-napas-${LocalDate.now()}.pdf")
      write(file, entries.sortedBy { it.date }, insight, userName)
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

  private fun symptomLabel(type: SymptomType): String =
    context.getString(
      when (type) {
        SymptomType.COUGH -> R.string.symptom_cough
        SymptomType.SHORTNESS_OF_BREATH -> R.string.symptom_shortness
        SymptomType.WHEEZING -> R.string.symptom_wheezing
        SymptomType.CHEST_DISCOMFORT -> R.string.symptom_chest
        SymptomType.THROAT_IRRITATION -> R.string.symptom_throat
        SymptomType.NONE -> R.string.symptom_none
      }
    )

  private fun write(file: File, entries: List<HealthJournalEntry>, insight: JournalInsight?, userName: String) {
    val doc = PdfDocument()
    val pageW = 595
    val pageH = 842
    val margin = 40f
    val title = Paint().apply { textSize = 20f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.rgb(10, 76, 192); isAntiAlias = true }
    val h2 = Paint().apply { textSize = 13f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD); color = Color.rgb(15, 35, 64); isAntiAlias = true }
    val body = Paint().apply { textSize = 10.5f; color = Color.rgb(40, 52, 72); isAntiAlias = true }
    val muted = Paint().apply { textSize = 9f; color = Color.rgb(86, 103, 127); isAntiAlias = true }
    val line = Paint().apply { color = Color.rgb(214, 228, 245); strokeWidth = 0.8f }

    var pageNo = 1
    var page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
    var canvas = page.canvas
    var y = margin + 10

    fun footer() {
      canvas.drawText(context.getString(R.string.pdf_footer), margin, pageH - 24f, muted)
      canvas.drawText(pageNo.toString(), pageW - margin - 8, pageH - 24f, muted)
    }

    fun newPage() {
      footer()
      doc.finishPage(page)
      pageNo++
      page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create())
      canvas = page.canvas
      y = margin + 10
    }

    fun ensure(space: Float) {
      if (y + space > pageH - 50) newPage()
    }

    canvas.drawText(context.getString(R.string.pdf_title), margin, y, title)
    y += 18
    canvas.drawText(context.getString(R.string.pdf_generated, Formatters.longDate(LocalDate.now())), margin, y, muted)
    y += 14
    if (userName.isNotBlank()) {
      canvas.drawText(context.getString(R.string.pdf_user, userName), margin, y, body)
      y += 14
    }
    if (entries.isNotEmpty()) {
      canvas.drawText(
        context.getString(R.string.pdf_period, Formatters.shortDate(entries.first().date), Formatters.shortDate(entries.last().date)),
        margin, y, body,
      )
      y += 14
    }
    y += 6

    // Ringkasan
    canvas.drawText(context.getString(R.string.pdf_summary), margin, y, h2)
    y += 16
    val withAqi = entries.mapNotNull { it.aqiAtEntry }
    val symptomDays = entries.count { it.symptomScore > 0 }
    val inhaler = entries.sumOf { it.inhalerUses }
    listOf(
      context.getString(R.string.pdf_summary_days, entries.size),
      context.getString(R.string.pdf_summary_symptom_days, symptomDays),
      context.getString(R.string.pdf_summary_inhaler, inhaler),
      if (withAqi.isNotEmpty()) context.getString(R.string.pdf_summary_avg_aqi, Formatters.decimal(withAqi.average(), 0)) else null,
    ).filterNotNull().forEach {
      canvas.drawText("• $it", margin + 6, y, body)
      y += 14
    }
    insight?.let {
      val text =
        when (it.kind) {
          InsightKind.NOT_ENOUGH_DATA -> context.getString(R.string.insight_not_enough, it.daysUsed)
          InsightKind.POSITIVE -> context.getString(R.string.insight_positive)
          InsightKind.NEGATIVE -> context.getString(R.string.insight_negative)
          InsightKind.NO_PATTERN -> context.getString(R.string.insight_none)
        }
      y += 4
      canvas.drawText(context.getString(R.string.pdf_insight), margin, y, h2)
      y += 15
      wrap(text, body, pageW - 2 * margin).forEach { l -> ensure(14f); canvas.drawText(l, margin + 6, y, body); y += 13 }
    }
    y += 10

    // Tabel
    val cols = floatArrayOf(margin, margin + 70, margin + 108, margin + 270, margin + 318, margin + 362, margin + 400)
    fun header() {
      canvas.drawText(context.getString(R.string.pdf_col_date), cols[0], y, h2.apply { textSize = 10f })
      canvas.drawText(context.getString(R.string.pdf_col_aqi), cols[1], y, h2)
      canvas.drawText(context.getString(R.string.pdf_col_symptoms), cols[2], y, h2)
      canvas.drawText(context.getString(R.string.pdf_col_severity), cols[3], y, h2)
      canvas.drawText(context.getString(R.string.pdf_col_inhaler), cols[4], y, h2)
      canvas.drawText(context.getString(R.string.pdf_col_smoke), cols[5], y, h2)
      canvas.drawText(context.getString(R.string.pdf_col_notes), cols[6], y, h2)
      y += 6
      canvas.drawLine(margin, y, pageW - margin, y, line)
      y += 12
    }
    ensure(40f)
    canvas.drawText(context.getString(R.string.pdf_table), margin, y, h2.apply { textSize = 13f })
    y += 16
    header()
    for (e in entries.asReversed()) {
      ensure(30f)
      if (y > pageH - 60) { newPage(); header() }
      val symptoms = e.symptoms.map { symptomLabel(it.type) }.joinToString(", ").ifBlank { symptomLabel(SymptomType.NONE) }
      val symptomLines = wrap(symptoms, body, cols[3] - cols[2] - 6)
      val noteLines = wrap(e.notes, body, pageW - margin - cols[6])
      val rows = maxOf(symptomLines.size, noteLines.size, 1)
      canvas.drawText(Formatters.dayMonth(e.date), cols[0], y, body)
      canvas.drawText(e.aqiAtEntry?.toString() ?: "-", cols[1], y, body)
      canvas.drawText(e.severity.toString(), cols[3], y, body)
      canvas.drawText(e.inhalerUses.toString(), cols[4], y, body)
      canvas.drawText(e.cigarettesToday?.toString() ?: "-", cols[5], y, body)
      for (i in 0 until rows) {
        symptomLines.getOrNull(i)?.let { canvas.drawText(it, cols[2], y + i * 12f, body) }
        noteLines.getOrNull(i)?.let { canvas.drawText(it, cols[6], y + i * 12f, body) }
      }
      y += rows * 12f + 4
      canvas.drawLine(margin, y - 8, pageW - margin, y - 8, line)
    }
    if (entries.isEmpty()) {
      canvas.drawText(context.getString(R.string.pdf_no_entries), margin, y, body)
    }
    footer()
    doc.finishPage(page)
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
  }

  private fun wrap(text: String, paint: Paint, width: Float): List<String> {
    if (text.isBlank()) return emptyList()
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in text.split(" ")) {
      val candidate = if (current.isEmpty()) word else "$current $word"
      if (paint.measureText(candidate) <= width) {
        current = StringBuilder(candidate)
      } else {
        if (current.isNotEmpty()) lines += current.toString()
        current = StringBuilder(word)
      }
    }
    if (current.isNotEmpty()) lines += current.toString()
    return lines
  }
}
