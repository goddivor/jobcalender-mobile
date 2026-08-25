package tg.goddivor.jobcalender.data.seed

import tg.goddivor.jobcalender.domain.model.Channel
import tg.goddivor.jobcalender.domain.model.EventMode
import tg.goddivor.jobcalender.domain.model.EventOutcome
import tg.goddivor.jobcalender.domain.model.EventType
import tg.goddivor.jobcalender.domain.model.Status

/**
 * The seed speaks French and lowercase; the code speaks English enums. Every mapping is explicit
 * and every unknown value throws, because a silently dropped application is worse than a crash at
 * import: the whole point of this app is that nothing goes missing.
 */

class UnknownSeedValueException(kind: String, value: String) :
    IllegalArgumentException("Unknown $kind in the seed file: '$value'")

fun statusOf(value: String): Status = when (value) {
    "brouillon" -> Status.DRAFT
    "envoyee" -> Status.SENT
    "accusee" -> Status.ACKNOWLEDGED
    "lue" -> Status.READ
    "preselection" -> Status.SHORTLISTED
    "test" -> Status.TEST
    "entretien" -> Status.INTERVIEW
    "offre" -> Status.OFFER
    "refus" -> Status.REJECTED
    "sans_reponse" -> Status.NO_REPLY
    else -> throw UnknownSeedValueException("status", value)
}

fun channelOf(value: String): Channel = when (value) {
    "email" -> Channel.EMAIL
    "formulaire_web" -> Channel.WEB_FORM
    "emploi_tg" -> Channel.EMPLOI_TG
    "linkedin" -> Channel.LINKEDIN
    "whatsapp" -> Channel.WHATSAPP
    "depot_direct" -> Channel.DIRECT
    else -> throw UnknownSeedValueException("channel", value)
}

fun eventTypeOf(value: String): EventType = when (value) {
    "accuse" -> EventType.ACKNOWLEDGEMENT
    "lue" -> EventType.READ
    "preselection" -> EventType.SHORTLIST
    "test" -> EventType.TEST
    "entretien" -> EventType.INTERVIEW
    "reunion" -> EventType.MEETING
    "deadline" -> EventType.DEADLINE
    "relance" -> EventType.FOLLOW_UP
    "refus" -> EventType.REJECTION
    "offre" -> EventType.OFFER
    else -> throw UnknownSeedValueException("event type", value)
}

fun eventModeOf(value: String?): EventMode? = when (value) {
    null -> null
    "presentiel" -> EventMode.ONSITE
    "visio" -> EventMode.VIDEO
    "teams" -> EventMode.TEAMS
    "google_meet" -> EventMode.GOOGLE_MEET
    "telephone" -> EventMode.PHONE
    else -> throw UnknownSeedValueException("event mode", value)
}

fun eventOutcomeOf(value: String?): EventOutcome? = when (value) {
    null -> null
    "a_venir" -> EventOutcome.PENDING
    "fait", "faite" -> EventOutcome.DONE
    "manque", "manquee" -> EventOutcome.MISSED
    "annule", "annulee" -> EventOutcome.CANCELLED
    else -> throw UnknownSeedValueException("event outcome", value)
}
