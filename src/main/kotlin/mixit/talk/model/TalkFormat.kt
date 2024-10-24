package mixit.talk.model

enum class TalkFormat(val duration: Int) {
    TALK(50),
    LIGHTNING_TALK(20),
    WORKSHOP(110),
    RANDOM(25),
    KEYNOTE(25),
    KEYNOTE_SURPRISE(25),
    CLOSING_SESSION(25),
    INTERVIEW(45),
    ON_AIR(30);

    fun isKeynote(): Boolean =
        this == KEYNOTE || this == KEYNOTE_SURPRISE || this == CLOSING_SESSION
}
