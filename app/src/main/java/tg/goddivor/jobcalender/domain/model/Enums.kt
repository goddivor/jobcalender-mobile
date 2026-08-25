package tg.goddivor.jobcalender.domain.model

/**
 * Progression is deliberately not enforced: a real application jumped from ACKNOWLEDGED straight to
 * TEST with no visible step between. Never block a transition and never compute a "next" status.
 */
enum class Status { DRAFT, SENT, ACKNOWLEDGED, READ, SHORTLISTED, TEST, INTERVIEW, OFFER, REJECTED, NO_REPLY }

/** Where to go back and read the exchange. Always displayed. */
enum class Channel { EMAIL, WEB_FORM, EMPLOI_TG, LINKEDIN, WHATSAPP, DIRECT }

enum class EventType { ACKNOWLEDGEMENT, READ, SHORTLIST, TEST, INTERVIEW, MEETING, DEADLINE, FOLLOW_UP, REJECTION, OFFER }

enum class EventMode { ONSITE, VIDEO, TEAMS, GOOGLE_MEET, PHONE }

enum class EventOutcome { PENDING, DONE, MISSED, CANCELLED }
