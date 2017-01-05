package mixit.model

enum class SessionFormat(val duration: Int) {
    Talk(50),
    LightningTalk(5),
    Workshop(110),
    Random(25),
    Keynote(25)
}
