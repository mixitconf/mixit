package mixit.talk.model

enum class Topic(val value: String) {
    MAKERS("makers"),
    ALIENS("aliens"),
    TECH("tech"),
    TEAM("team"),
    OTHER("other"),
    DESIGN("design"),
    HACK("hacktivism"),
    ETHICS("ethics"),
    LIFE_STYLE("lifestyle"),
    LEARN("learn"),
    ON_AIR("onair");

    companion object {
        fun of(label: String) = entries.firstOrNull { it.value == label } ?: OTHER
    }
}
