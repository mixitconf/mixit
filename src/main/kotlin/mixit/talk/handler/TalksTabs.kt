package mixit.talk.handler

data class TabDto(val name: String, val active: Boolean, val url: String)

enum class TalksTabs(val url: String) {
    Talks(""),
    Speakers("/speakers"),
    Schedule("?agenda=true"),
    Favorites("/favorites"),
    MainVideo("/medias/video"),
    TalksWithVideo("/medias"),
    Mixette("/mixette"),
    MiXiTonAir("/mixit-on-air"),
    Photos("/medias/images");

    companion object {
        private fun current(
            isCurrent: Boolean,
            hasMixette: Boolean,
            hasOnAir: Boolean,
            isConnected: Boolean,
            canDisplayAgenda: Boolean
        ) =
            if (isCurrent && canDisplayAgenda) {
                listOf(Schedule, Speakers).addOnAir(hasOnAir).addMixette(hasMixette)
            } else {
                listOf(Talks, Speakers).addOnAir(hasOnAir).addMixette(hasMixette)
            }
//            if (isCurrent && isConnected && canDisplayAgenda) {
//                listOf(Schedule, Speakers, Favorites).addOnAir(hasOnAir).addMixette(hasMixette)
//            } else if (isCurrent && canDisplayAgenda) {
//                listOf(Schedule, Speakers).addOnAir(hasOnAir).addMixette(hasMixette)
//            } else if (isConnected) {
//                listOf(Talks, Speakers, Favorites).addOnAir(hasOnAir).addMixette(hasMixette)
//            } else {
//                listOf(Talks, Speakers).addOnAir(hasOnAir).addMixette(hasMixette)
//            }

        private fun List<TalksTabs>.addMixette(hasMixette: Boolean) =
            if (hasMixette) this + listOf(Mixette) else this

        private fun List<TalksTabs>.addOnAir(hasOnAir: Boolean) =
            if (hasOnAir) this + listOf(MiXiTonAir) else this

        private fun old(hasMixette: Boolean, hasOnAir: Boolean) =
            listOf(TalksWithVideo, Speakers, MainVideo, Photos).addOnAir(hasOnAir).addMixette(hasMixette)
    }

    fun tabs(
        hasMixette: Boolean,
        hasOnAir: Boolean,
        isCurrent: Boolean,
        isConnected: Boolean,
        canDisplayAgenda: Boolean
    ): List<TabDto> =
        isCurrent
            .let {
                if (isCurrent) {
                    current(true, hasMixette, hasOnAir, isConnected, canDisplayAgenda)
                } else {
                    old(hasMixette, hasOnAir)
                }
            }
            .let {
                it.map { tab ->
                    TabDto(tab.name.lowercase(), tab == this, tab.url)
                }
            }
}
