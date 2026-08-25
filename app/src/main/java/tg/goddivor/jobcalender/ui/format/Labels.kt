package tg.goddivor.jobcalender.ui.format

import androidx.annotation.StringRes
import tg.goddivor.jobcalender.R
import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status

/** Every enum reaches the screen through a string resource, never through its own name. */

@get:StringRes
val Status.label: Int
    get() = when (this) {
        Status.DRAFT -> R.string.status_draft
        Status.SENT -> R.string.status_sent
        Status.ACKNOWLEDGED -> R.string.status_acknowledged
        Status.READ -> R.string.status_read
        Status.SHORTLISTED -> R.string.status_shortlisted
        Status.TEST -> R.string.status_test
        Status.INTERVIEW -> R.string.status_interview
        Status.OFFER -> R.string.status_offer
        Status.REJECTED -> R.string.status_rejected
        Status.NO_REPLY -> R.string.status_no_reply
    }

@get:StringRes
val Channel.label: Int
    get() = when (this) {
        Channel.EMAIL -> R.string.channel_email
        Channel.WEB_FORM -> R.string.channel_web_form
        Channel.EMPLOI_TG -> R.string.channel_emploi_tg
        Channel.LINKEDIN -> R.string.channel_linkedin
        Channel.WHATSAPP -> R.string.channel_whatsapp
        Channel.DIRECT -> R.string.channel_direct
    }

@get:StringRes
val EventType.label: Int
    get() = when (this) {
        EventType.ACKNOWLEDGEMENT -> R.string.event_type_acknowledgement
        EventType.READ -> R.string.event_type_read
        EventType.SHORTLIST -> R.string.event_type_shortlist
        EventType.TEST -> R.string.event_type_test
        EventType.INTERVIEW -> R.string.event_type_interview
        EventType.MEETING -> R.string.event_type_meeting
        EventType.DEADLINE -> R.string.event_type_deadline
        EventType.FOLLOW_UP -> R.string.event_type_follow_up
        EventType.REJECTION -> R.string.event_type_rejection
        EventType.OFFER -> R.string.event_type_offer
    }

@get:StringRes
val EventMode.label: Int
    get() = when (this) {
        EventMode.ONSITE -> R.string.event_mode_onsite
        EventMode.VIDEO -> R.string.event_mode_video
        EventMode.TEAMS -> R.string.event_mode_teams
        EventMode.GOOGLE_MEET -> R.string.event_mode_google_meet
        EventMode.PHONE -> R.string.event_mode_phone
    }

@get:StringRes
val EventOutcome.label: Int
    get() = when (this) {
        EventOutcome.PENDING -> R.string.event_outcome_pending
        EventOutcome.DONE -> R.string.event_outcome_done
        EventOutcome.MISSED -> R.string.event_outcome_missed
        EventOutcome.CANCELLED -> R.string.event_outcome_cancelled
    }
