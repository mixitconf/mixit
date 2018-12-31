package mixit.config

import mixit.repository.*
import mixit.util.Cryptographer
import org.springframework.boot.CommandLineRunner
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.mongo.mongodb

val databaseConfig = configuration {
    beans {
        bean {
            // Use listener<ApplicationReadyEvent> { } when SPR-17298 will be fixed
            CommandLineRunner {
                ref<UserRepository>().initData()
                ref<EventRepository>().initData()
                ref<TalkRepository>().initData()
                ref<PostRepository>().initData()
                ref<TicketRepository>().initData()
                ref<FavoriteRepository>().initData()
            }
        }
        bean<EventRepository>()
        bean<FavoriteRepository>()
        bean<PostRepository>()
        bean<TalkRepository>()
        bean<TicketRepository>()
        bean<UserRepository>()
        bean<Cryptographer>()
    }
    mongodb()
}
