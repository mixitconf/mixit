package mixit.model

enum class Room(name: String, capacity: Int) {
    AMPHI1("Grand Amphi", 500),
    AMPHI2("Petit Amphi", 200),
    ROOM1("Gosling", 110),
    ROOM2("Eich", 110),
    ROOM3("Nonaka", 30),
    ROOM4("Dijkstra", 30),
    ROOM5("Turing", 30),
    ROOM6("Lovelace", 30),
    ROOM7("Mezzanine", 50),
    UNKNOWN("Inconnue", 0);

    companion object {
        fun findByName(name: String): Room {
            val room = Room.values().filter { value -> value.name == name }
            if (room.isEmpty()) {
                return UNKNOWN
            }
            return room.first()
        }
    }
}
