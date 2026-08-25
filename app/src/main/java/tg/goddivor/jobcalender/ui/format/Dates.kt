package tg.goddivor.jobcalender.ui.format

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Everything is Lomé time: GMT, no daylight saving. The app never renders a UTC offset and never
 * converts for display, because the user lives in that timezone and reads invitations written in it.
 */
val LOME: ZoneId = ZoneId.of("Africa/Lome")

private val SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.FRENCH)
private val LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val MONTH_YEAR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH)
private val TIME = DateTimeFormatter.ofPattern("HH'h'mm", Locale.FRENCH)

/** "lun. 24 août" */
fun LocalDate.short(): String = format(SHORT_DATE)

/** "lundi 24 août 2026" */
fun LocalDate.long(): String = format(LONG_DATE)

/** "août 2026" */
fun LocalDate.monthYear(): String = format(MONTH_YEAR)

/** "11h30", on 24 hours, as every invitation in the dataset is written. */
fun LocalTime.hhmm(): String = format(TIME)

fun today(): LocalDate = LocalDate.now(LOME)
