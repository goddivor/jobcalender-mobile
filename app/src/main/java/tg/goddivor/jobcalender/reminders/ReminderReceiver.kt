package tg.goddivor.jobcalender.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tg.goddivor.jobcalender.R

/** Woken by the alarm, posts the notification and gets out of the way. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val employer = intent.getStringExtra(EXTRA_EMPLOYER).orEmpty()
        val position = intent.getStringExtra(EXTRA_POSITION).orEmpty()
        val kind = intent.getStringExtra(EXTRA_KIND) ?: return
        val when_ = intent.getStringExtra(EXTRA_WHEN).orEmpty()
        val id = intent.getIntExtra(EXTRA_ID, 0)

        val title = when (ReminderKind.valueOf(kind)) {
            ReminderKind.DAY_BEFORE -> context.getString(R.string.reminder_tomorrow_title, employer)
            ReminderKind.HOUR_BEFORE -> context.getString(R.string.reminder_soon_title, employer)
            ReminderKind.CLOSING -> context.getString(R.string.reminder_closing_title, employer)
        }
        val body = context.getString(R.string.reminder_body, position, when_)

        ReminderNotifier.post(context, title, body, id)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_KIND = "kind"
        const val EXTRA_EMPLOYER = "employer"
        const val EXTRA_POSITION = "position"
        const val EXTRA_WHEN = "when"
    }
}
