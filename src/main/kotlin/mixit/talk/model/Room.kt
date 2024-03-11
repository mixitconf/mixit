package mixit.talk.model

@Suppress("UNUSED_PARAMETER")
enum class Room(capacity: Int, val hasLink: Boolean, val order: Int = 99) {
    AMPHI1(500, true, 1),
    AMPHI2(200, true, 2),
    AMPHIC(445, false, 1),
    AMPHID(445, false, 2),
    AMPHIK(300, false, 3),
    ROOM1(48, true, 4),
    ROOM2(48, true, 5),
    ROOM3(36, true, 7),
    ROOM4(36, true, 8),
    ROOM5(36, true, 6),
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
