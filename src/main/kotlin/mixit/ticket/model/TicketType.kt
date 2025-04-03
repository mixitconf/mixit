package mixit.ticket.model

enum class TicketType(val image: String) {
    SPEAKER("/images/svg/mxt-icon-cfp-dark.svg"),
    SPONSOR_LANYARD("/images/svg/mxt-icon-sponsor-dark.svg"),
    SPONSOR_STAND("/images/svg/mxt-icon-sponsor-dark.svg"),
    SPONSOR_PARTY("/images/svg/mxt-icon-sponsor-dark.svg"),
    SPONSOR_PARTNER("/images/svg/mxt-icon-sponsor-dark.svg"),
    SPONSOR_MIXTEEN("/images/svg/mxt-icon-sponsor-dark.svg"),
    STAFF("/images/svg/mxt-icon-tshirt-dark.svg"),
    MIXTEEN("/images/svg/mxt-icon-team-dark.svg"),
    ATTENDEE("/images/svg/mxt-icon-lottery-dark.svg"),
    ATT_29_APRIL("/images/svg/mxt-icon-lottery-dark.svg"),
    ATT_30_APRIL("/images/svg/mxt-icon-lottery-dark.svg"),
    VOLUNTEER("/images/svg/mxt-icon-tshirt-dark.svg"),
    GUEST("/images/svg/mxt-icon-lottery-dark.svg"),
    FOOD("/images/svg/mxt-icon-tshirt-dark.svg"),
    MEDIA("/images/svg/mxt-icon-video-dark.svg"),
    MIXETTE("/images/svg/mxt-icon-sponsor-dark.svg"),
}
