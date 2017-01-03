package mixit.model

enum class Room(name: String, capacity: Int) {
    Amphi1("Grand Amphi", 500),
    Amphi2("Petit Amphi", 200),
    Salle1("Gosling", 110),
    Salle2("Eich", 110),
    Salle3("Nonaka", 30),
    Salle4("Dijkstra", 30),
    Salle5("Turing", 30),
    Salle6("Lovelace", 30),
    Salle7("Mezzanine", 50),
    Unknown("Inconnue", 0);

    companion object {
        fun findByName(name: String?): Room {
            val room = Room.values().filter { value -> value.name.equals(name) }
            if (room.isEmpty()) {
                return Unknown;
            }
            return room.first();
        }
    }
}