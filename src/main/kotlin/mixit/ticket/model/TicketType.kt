package mixit.ticket.model

enum class TicketType(val image: String, val typeDisplay: String) {
    SPEAKER("/images/svg/mxt-icon-cfp-dark.svg", "SPEAKER"),
    SPONSOR_LANYARD("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    SPONSOR_STAND("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    SPONSOR_PARTY("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    SPONSOR_PARTNER("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    SPONSOR_MIXTEEN("/images/svg/mxt-icon-sponsor-dark.svg", ""),
    STAFF("/images/svg/mxt-icon-tshirt-dark.svg", "STAFF"),
    MIXTEEN("/images/svg/mxt-icon-team-dark.svg", "MIXTEEN"),
    ATTENDEE("/images/svg/mxt-icon-lottery-dark.svg", "ATTENDEE"),
    ATT_29_APRIL("/images/svg/mxt-icon-lottery-dark.svg", "ATTENDEE"),
    ATT_30_APRIL("/images/svg/mxt-icon-lottery-dark.svg", "ATTENDEE"),
    ATT_29_APRIL_SPONSOR("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    ATT_30_APRIL_SPONSOR("/images/svg/mxt-icon-sponsor-dark.svg", "SPONSOR"),
    VOLUNTEER("/images/svg/mxt-icon-tshirt-dark.svg", "VOLUNTEER"),
    GUEST("/images/svg/mxt-icon-lottery-dark.svg", "GUEST"),
    FOOD("/images/svg/mxt-icon-tshirt-dark.svg", "FOOD"),
    MEDIA("/images/svg/mxt-icon-video-dark.svg", "MEDIA"),
    MIXETTE("/images/svg/mxt-icon-sponsor-dark.svg", "MIXETTE"),
}
