package mixit.talk.handler

import mixit.MixitApplication
import mixit.talk.handler.TalksTabs.Favorites
import mixit.talk.handler.TalksTabs.MainVideo
import mixit.talk.handler.TalksTabs.MiXiTonAir
import mixit.talk.handler.TalksTabs.Mixette
import mixit.talk.handler.TalksTabs.Photos
import mixit.talk.handler.TalksTabs.Schedule
import mixit.talk.handler.TalksTabs.Speakers
import mixit.talk.handler.TalksTabs.Talks
import mixit.talk.handler.TalksTabs.TalksWithVideo
import mixit.util.mustache.MustacheTemplate
import mixit.util.mustache.MustacheTemplate.Media
import mixit.util.mustache.MustacheTemplate.MediaAllImages
import mixit.util.mustache.MustacheTemplate.MediaImages
import mixit.util.mustache.MustacheTemplate.MediaVideo
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.queryParamOrNull

enum class TalkListType { SimpleList, ListByDate, Agenda }

data class TalkViewConfig(
    val year: Int,
    val req: ServerRequest,
    val tabs: TalksTabs,
    val topic: String? = null,
    val filterOnFavorite: Boolean = false,
    val template: MustacheTemplate = MustacheTemplate.TalkList,
    val album: String? = null,
    val url: String? = null,
    val viewWorkshop: Boolean = true,
    val viewFavorites: Boolean = true,
) {
    companion object {
        fun media(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                TalksWithVideo,
                topic,
                template = Media
            )

        fun video(req: ServerRequest, year: Int, topic: String? = null) =
            media(req, year, topic).copy(tabs = MainVideo, template = MediaVideo)

        fun images(req: ServerRequest, year: Int, topic: String? = null) =
            media(req, year, topic).copy(tabs = Photos, template = MediaAllImages)

        fun mediaWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            media(req, year, topic).copy(filterOnFavorite = true, tabs = Favorites)

        fun imageAlbum(req: ServerRequest, year: Int, topic: String? = null) =
            TalkViewConfig(
                year,
                req,
                Photos,
                topic,
                template = MediaImages,
                album = req.pathVariable("album"),
                url = req.queryParamOrNull("url")
            )

        fun talks(req: ServerRequest, year: Int, topic: String? = null): TalkViewConfig {
            val isAgenda = (req.queryParamOrNull("agenda") ?: "false") == "true"
            val mode = if (isAgenda) TalkListType.Agenda else TalkListType.ListByDate
            return TalkViewConfig(
                year,
                req,
                tabs = if (year == MixitApplication.CURRENT_EVENT.toInt()) Schedule else Talks,
                topic,
                viewWorkshop = (req.queryParamOrNull("workshop") ?: "true") == "true",
                viewFavorites = (req.queryParamOrNull("favorites") ?: "false") == "true",
            )
        }

        fun mixitonair(req: ServerRequest, year: Int): TalkViewConfig =
            TalkViewConfig(
                year,
                req,
                tabs = MiXiTonAir,
                template = MustacheTemplate.MiXiTOnAir,
                viewFavorites = (req.queryParamOrNull("favorites") ?: "false") == "true",
            )

        fun mixette(req: ServerRequest, year: Int) =
            TalkViewConfig(
                year,
                req,
                Mixette,
                template = MustacheTemplate.Mixette,
            )

        fun speakers(req: ServerRequest, year: Int) =
            TalkViewConfig(
                year,
                req,
                Speakers,
                template = MustacheTemplate.Speakers,
            )

        fun talksWithFavorites(req: ServerRequest, year: Int, topic: String? = null) =
            talks(req, year, topic).copy(filterOnFavorite = true, tabs = Favorites)
    }
}
