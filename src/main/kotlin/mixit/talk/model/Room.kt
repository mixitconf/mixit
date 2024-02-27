package mixit.talk.model

@Suppress("UNUSED_PARAMETER")
enum class Room(capacity: Int, val hasLink: Boolean) {
    AMPHI1(500, true),
    AMPHI2(200, true),
    AMPHIC(445, false),
    AMPHID(445, false),
    AMPHIK(300, false),
    ROOM1(48, true),
    ROOM2(48, true),
    ROOM3(36, true),
    ROOM4(36, true),
    ROOM5(36, true),
    ROOM6(36, true),
    ROOM7(36, true),
    ROOM8(36, false),
    OUTSIDE(50, false),
    MUMMY(30, false),
    SPEAKER(16, false),
    UNKNOWN(0, false),
    SURPRISE(0, false),
    TWITCH(0, false);
}
