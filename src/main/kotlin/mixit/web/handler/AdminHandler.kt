package mixit.web.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.MixitProperties
import mixit.model.*
import mixit.model.Language.ENGLISH
import mixit.model.Language.FRENCH
import mixit.model.Role.*
import mixit.model.Room.*
import mixit.model.TalkFormat.*
import mixit.repository.*
import mixit.util.Cryptographer
import mixit.util.language
import mixit.util.seeOther
import mixit.util.toSlug
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyExtractors
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.ServerResponse.ok
import reactor.core.publisher.Mono
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.streams.toList


@Component
class AdminHandler(private val ticketRepository: TicketRepository,
                   private val talkRepository: TalkRepository,
                   private val userRepository: UserRepository,
                   private val eventRepository: EventRepository,
                   private val postRepository: PostRepository,
                   private val properties: MixitProperties,
                   private val objectMapper: ObjectMapper,
                   private val cryptographer: Cryptographer) {

    fun admin(req: ServerRequest) =
            ok().render("admin", mapOf(Pair("title", "admin.title")))

    fun adminTicketing(req: ServerRequest) =
            ok().render("admin-ticketing", mapOf(
                    Pair("tickets", ticketRepository.findAll()
                            .map {
                                Ticket(
                                        it.email,
                                        it.firstname.trim().toLowerCase().capitalize(),
                                        it.lastname.trim().toLowerCase().capitalize())
                            }
                            .sort(Comparator.comparing(Ticket::lastname)
                                    .thenComparing(Comparator.comparing(Ticket::firstname)))),
                    Pair("title", "admin.ticketing.title")
            ))

    fun adminDeleteTicketing(req: ServerRequest): Mono<ServerResponse> =
            req.body(BodyExtractors.toFormData()).flatMap {
                val formData = it.toSingleValueMap()
                ticketRepository
                        .deleteOne(formData["email"]!!)
                        .then(seeOther("${properties.baseUri}/admin/ticketing"))
            }

    fun adminTalks(req: ServerRequest, year: String) =
            ok().render("admin-talks", mapOf(
                    Pair("year", year),
                    Pair("talks", talkRepository
                            .findByEvent(year)
                            .collectList()
                            .flatMap { talks ->
                                userRepository
                                        .findMany(talks.flatMap(Talk::speakerIds))
                                        .collectMap(User::login)
                                        .map { speakers -> talks.map { it.toDto(req.language(), it.speakerIds.mapNotNull { speakers[it] }) } }
                            }),
                    Pair("title", "admin.talks.title")
            ))


    fun adminUsers(req: ServerRequest) = ok().render("admin-users", mapOf(Pair("users", userRepository.findAll()), Pair("title", "admin.users.title")))


    fun createTalk(req: ServerRequest): Mono<ServerResponse> = this.adminTalk()

    fun editTalk(req: ServerRequest): Mono<ServerResponse> = talkRepository.findBySlug(req.pathVariable("slug")).flatMap(this::adminTalk)

    fun adminSaveTalk(req: ServerRequest): Mono<ServerResponse> {
        return req.body(BodyExtractors.toFormData()).flatMap {
            val formData = it.toSingleValueMap()
            val talk = Talk(
                    id = formData["id"],
                    event = formData["event"]!!,
                    format = TalkFormat.valueOf(formData["format"]!!),
                    title = formData["title"]!!,
                    summary = formData["summary"]!!,
                    description = if (formData["description"] == "") null else formData["description"],
                    topic = formData["topic"],
                    language = Language.valueOf(formData["language"]!!),
                    speakerIds = formData["speakers"]!!.split(","),
                    video = if (formData["video"] == "") null else formData["video"],
                    room = Room.valueOf(formData["room"]!!),
                    addedAt = LocalDateTime.parse(formData["addedAt"]),
                    start = LocalDateTime.parse(formData["start"]),
                    end = LocalDateTime.parse(formData["end"]),
                    photoUrls = if (formData["photoUrls"].isNullOrEmpty()) emptyList() else formData["photoUrls"]!!.toLinks()
            )
            talkRepository.save(talk).then(seeOther("${properties.baseUri}/admin/talks"))
        }
    }

    fun adminDeleteTalk(req: ServerRequest): Mono<ServerResponse> =
            req.body(BodyExtractors.toFormData()).flatMap {
                val formData = it.toSingleValueMap()
                talkRepository
                        .deleteOne(formData["id"]!!)
                        .then(seeOther("${properties.baseUri}/admin/talks"))
            }


    private fun adminTalk(talk: Talk = Talk(TALK, "2019", "", "")) = ok().render("admin-talk", mapOf(
            Pair("talk", talk),
            Pair("title", "admin.talk.title"),
            Pair("rooms", listOf(
                    Triple(AMPHI1, "rooms.${AMPHI1.name.toLowerCase()}", AMPHI1 == talk.room),
                    Triple(AMPHI2, "rooms.${AMPHI2.name.toLowerCase()}", AMPHI2 == talk.room),
                    Triple(ROOM1, "rooms.${ROOM1.name.toLowerCase()}", ROOM1 == talk.room),
                    Triple(ROOM2, "rooms.${ROOM2.name.toLowerCase()}", ROOM2 == talk.room),
                    Triple(ROOM3, "rooms.${ROOM3.name.toLowerCase()}", ROOM3 == talk.room),
                    Triple(ROOM4, "rooms.${ROOM4.name.toLowerCase()}", ROOM4 == talk.room),
                    Triple(ROOM5, "rooms.${ROOM5.name.toLowerCase()}", ROOM5 == talk.room),
                    Triple(ROOM6, "rooms.${ROOM6.name.toLowerCase()}", ROOM6 == talk.room),
                    Triple(ROOM7, "rooms.${ROOM7.name.toLowerCase()}", ROOM7 == talk.room),
                    Triple(UNKNOWN, "rooms.${UNKNOWN.name.toLowerCase()}", UNKNOWN == talk.room)
            )),
            Pair("formats", listOf(
                    Pair(TALK, TALK == talk.format),
                    Pair(LIGHTNING_TALK, LIGHTNING_TALK == talk.format),
                    Pair(WORKSHOP, WORKSHOP == talk.format),
                    Pair(RANDOM, RANDOM == talk.format),
                    Pair(KEYNOTE, KEYNOTE == talk.format),
                    Pair(KEYNOTE_SURPRISE, KEYNOTE_SURPRISE == talk.format)
            )),
            Pair("languages", listOf(
                    Pair(ENGLISH, ENGLISH == talk.language),
                    Pair(FRENCH, FRENCH == talk.language)
            )),
            Pair("topics", listOf(
                    Pair("makers", "makers" == talk.topic),
                    Pair("aliens", "aliens" == talk.topic),
                    Pair("tech", "tech" == talk.topic),
                    Pair("team", "team" == talk.topic),
                    Pair("design", "design" == talk.topic),
                    Pair("hacktivism", "hacktivism" == talk.topic),
                    Pair("learn", "learn" == talk.topic)
            )),
            Pair("speakers", talk.speakerIds.joinToString(separator = ",")),
            Pair("photos", talk.photoUrls.toJson())

    ))

    fun adminEvents(req: ServerRequest) = ok().render("admin-events", mapOf(Pair("events", eventRepository.findAll()), Pair("title", "admin.events.title")))

    fun createEvent(req: ServerRequest): Mono<ServerResponse> = this.adminEvent()

    fun editEvent(req: ServerRequest): Mono<ServerResponse> = eventRepository.findOne(req.pathVariable("eventId")).flatMap { this.adminEvent(it) }

    private fun adminEvent(event: Event = Event("", LocalDate.now(), LocalDate.now())) = ok().render("admin-event", mapOf(
            Pair("creationMode", event.id == ""),
            Pair("event", event),
            Pair("links", event.photoUrls.toJson()),
            Pair("videolink", if (event.videoUrl == null) "" else event.videoUrl.toJson())
    ))

    fun adminSaveEvent(req: ServerRequest): Mono<ServerResponse> {
        return req.body(BodyExtractors.toFormData()).flatMap {
            val formData = it.toSingleValueMap()

            // We need to find the event in database
            eventRepository
                    .findOne(formData["eventId"]!!)
                    .flatMap {
                        val event = Event(
                                it.id,
                                LocalDate.parse(formData["start"]!!),
                                LocalDate.parse(formData["end"]!!),
                                if (formData["current"] == null) false else formData["current"]!!.toBoolean(),
                                it.sponsors,
                                if (formData["photoUrls"].isNullOrEmpty()) emptyList() else formData["photoUrls"]!!.toLinks(),
                                if (formData["videoUrl"].isNullOrEmpty()) null else formData["videoUrl"]!!.toLink()
                        )
                        eventRepository.save(event).then(seeOther("${properties.baseUri}/admin/events"))
                    }
                    .switchIfEmpty(eventRepository.save(Event(
                            formData["eventId"]!!,
                            LocalDate.parse(formData["start"]!!),
                            LocalDate.parse(formData["end"]!!),
                            if (formData["current"] == null) false else formData["current"]!!.toBoolean()
                    )).then(seeOther("${properties.baseUri}/admin/events")))
        }
    }

    fun createEventSponsoring(req: ServerRequest): Mono<ServerResponse> = adminEventSponsoring(req.pathVariable("eventId"))

    fun editEventSponsoring(req: ServerRequest): Mono<ServerResponse> = eventRepository
            .findOne(req.pathVariable("eventId"))
            .flatMap { adminEventSponsoring(req.pathVariable("eventId"), it.sponsors.stream().filter { eventSponsoringMatch(req.pathVariable("sponsorId"), req.pathVariable("level"), it) }.findAny().get()) }

    private fun adminEventSponsoring(eventId: String, eventSponsoring: EventSponsoring = EventSponsoring(SponsorshipLevel.NONE, "", LocalDate.now())) = ok().render("admin-event-sponsor", mapOf(
            Pair("creationMode", eventSponsoring.sponsorId == ""),
            Pair("eventId", eventId),
            Pair("eventSponsoring", eventSponsoring),
            Pair("levels", listOf(
                    Pair(SponsorshipLevel.ACCESSIBILITY, SponsorshipLevel.ACCESSIBILITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.BREAKFAST, SponsorshipLevel.ACCESSIBILITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.BRONZE, SponsorshipLevel.BRONZE == eventSponsoring.level),
                    Pair(SponsorshipLevel.COMMUNITY, SponsorshipLevel.COMMUNITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.ECOCUP, SponsorshipLevel.ECOCUP == eventSponsoring.level),
                    Pair(SponsorshipLevel.GOLD, SponsorshipLevel.GOLD == eventSponsoring.level),
                    Pair(SponsorshipLevel.HOSTING, SponsorshipLevel.ACCESSIBILITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.LANYARD, SponsorshipLevel.ACCESSIBILITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.LUNCH, SponsorshipLevel.ACCESSIBILITY == eventSponsoring.level),
                    Pair(SponsorshipLevel.MIXTEEN, SponsorshipLevel.MIXTEEN == eventSponsoring.level),
                    Pair(SponsorshipLevel.NONE, SponsorshipLevel.NONE == eventSponsoring.level),
                    Pair(SponsorshipLevel.PARTY, SponsorshipLevel.PARTY == eventSponsoring.level),
                    Pair(SponsorshipLevel.SILVER, SponsorshipLevel.SILVER == eventSponsoring.level),
                    Pair(SponsorshipLevel.VIDEO, SponsorshipLevel.VIDEO == eventSponsoring.level)
            ))
    ))

    private fun eventSponsoringMatch(sponsorId: String, level: String, eventSponsoring: EventSponsoring): Boolean =
            sponsorId.equals(eventSponsoring.sponsorId) && level.equals(eventSponsoring.level.name)

    fun adminUpdateEventSponsoring(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()
        // We need to find the event in database
        eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // We create a mutable list
                    val sponsors = it.sponsors
                            .stream()
                            .map {
                                if (eventSponsoringMatch(formData["eventId"]!!, formData["sponsorId"]!!, it)) {
                                    EventSponsoring(it.level, it.sponsorId, if (formData["subscriptionDate"] == null) LocalDate.now() else LocalDate.parse(formData["subscriptionDate"]!!))
                                } else {
                                    it
                                }
                            }
                            .toList()

                    eventRepository.save(Event(it.id, it.start, it.end, it.current, sponsors)).then(seeOther("${properties.baseUri}/admin/events/edit/${formData["eventId"]!!}"))
                }
    }

    fun adminCreateEventSponsoring(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()
        // We need to find the event in database
        eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // We create a mutable list
                    val sponsors = it.sponsors.toMutableList()
                    sponsors.add(
                            EventSponsoring(
                                    SponsorshipLevel.valueOf(formData["level"]!!),
                                    formData["sponsorId"]!!,
                                    if (formData["subscriptionDate"] == null) LocalDate.now() else LocalDate.parse(formData["subscriptionDate"]!!)))

                    eventRepository.save(Event(it.id, it.start, it.end, it.current, sponsors)).then(seeOther("${properties.baseUri}/admin/events/edit/${formData["eventId"]!!}"))
                }
    }

    fun adminDeleteEventSponsoring(req: ServerRequest): Mono<ServerResponse> = req.body(BodyExtractors.toFormData()).flatMap {
        val formData = it.toSingleValueMap()
        // We need to find the event in database
        eventRepository
                .findOne(formData["eventId"]!!)
                .flatMap {
                    // We create a mutable list
                    val sponsors = it.sponsors
                            .stream()
                            .map {
                                if (eventSponsoringMatch(formData["sponsorId"]!!, formData["level"]!!, it)) null else it
                            }
                            .filter(Objects::nonNull)
                            .toList()
                            .requireNoNulls()

                    eventRepository.save(Event(it.id, it.start, it.end, it.current, sponsors)).then(seeOther("${properties.baseUri}/admin/events/edit/${formData["eventId"]!!}"))
                }
    }


    fun createUser(req: ServerRequest): Mono<ServerResponse> = this.adminUser()

    fun editUser(req: ServerRequest): Mono<ServerResponse> = userRepository.findOne(req.pathVariable("login")).flatMap(this::adminUser)

    fun adminDeleteUser(req: ServerRequest): Mono<ServerResponse> =
            req.body(BodyExtractors.toFormData()).flatMap {
                val formData = it.toSingleValueMap()
                userRepository
                        .deleteOne(formData["login"]!!)
                        .then(seeOther("${properties.baseUri}/admin/users"))
            }

    private fun adminUser(user: User = User("", "", "", "")) = ok().render("admin-user", mapOf(
            Pair("user", user),
            Pair("usermail", cryptographer.decrypt(user.email)),
            Pair("description-fr", user.description[FRENCH]),
            Pair("description-en", user.description[ENGLISH]),
            Pair("roles", listOf(
                    Pair(USER, USER == user.role),
                    Pair(STAFF, STAFF == user.role),
                    Pair(STAFF_IN_PAUSE, STAFF_IN_PAUSE == user.role)
            )),
            Pair("links", user.links.toJson())

    ))

    fun adminSaveUser(req: ServerRequest): Mono<ServerResponse> {
        return req.body(BodyExtractors.toFormData()).flatMap {
            val formData = it.toSingleValueMap()
            val user = User(
                    login = formData["login"]!!,
                    firstname = formData["firstname"]!!,
                    lastname = formData["lastname"]!!,
                    email = if (formData["email"] == "") null else cryptographer.encrypt(formData["email"]),
                    emailHash = if (formData["emailHash"] == "") null else formData["emailHash"],
                    photoUrl = if (formData["photoUrl"] == "") {
                        if (formData["emailHash"] == "") "/images/png/mxt-icon--default-avatar.png" else null
                    } else {
                        if (formData["emailHash"] == "") formData["photoUrl"] else null
                    },
                    company = if (formData["company"] == "") null else formData["company"],
                    description = mapOf(Pair(FRENCH, formData["description-fr"]!!), Pair(ENGLISH, formData["description-en"]!!)),
                    role = Role.valueOf(formData["role"]!!),
                    links = formData["links"]!!.toLinks(),
                    legacyId = if (formData["legacyId"] == "") null else formData["legacyId"]!!.toLong()
            )
            userRepository.save(user).then(seeOther("${properties.baseUri}/admin/users"))
        }
    }


    fun adminBlog(req: ServerRequest): Mono<ServerResponse> = ok().render("admin-blog", mapOf(Pair("posts", postRepository.findAll().collectList()
            .flatMap { posts ->
                userRepository
                        .findMany(posts.map { it.authorId })
                        .collectMap(User::login)
                        .map { authors -> posts.map { it.toDto(if (authors[it.authorId] == null) User("mixit", "", "MiXiT", "") else authors[it.authorId]!!, req.language()) } }
            }), Pair("title", "admin.blog.title")))


    fun createPost(req: ServerRequest): Mono<ServerResponse> = this.adminPost()

    fun editPost(req: ServerRequest): Mono<ServerResponse> = postRepository.findOne(req.pathVariable("id")).flatMap(this::adminPost)

    fun adminDeletePost(req: ServerRequest): Mono<ServerResponse> =
            req.body(BodyExtractors.toFormData()).flatMap {
                val formData = it.toSingleValueMap()
                postRepository
                        .deleteOne(formData["id"]!!)
                        .then(seeOther("${properties.baseUri}/admin/blog"))
            }

    private fun adminPost(post: Post = Post("")) = ok().render("admin-post", mapOf(
            Pair("post", post),
            Pair("title-fr", post.title[FRENCH]),
            Pair("title-en", post.title[ENGLISH]),
            Pair("headline-fr", post.headline[FRENCH]),
            Pair("headline-en", post.headline[ENGLISH]),
            Pair("content-fr", post.content?.get(FRENCH)),
            Pair("content-en", post.content?.get(ENGLISH))
    ))

    fun adminSavePost(req: ServerRequest): Mono<ServerResponse> {
        return req.body(BodyExtractors.toFormData()).flatMap {
            val formData = it.toSingleValueMap()
            val post = Post(
                    id = formData["id"],
                    addedAt = LocalDateTime.parse(formData["addedAt"]),
                    authorId = formData["authorId"]!!,
                    title = mapOf(Pair(FRENCH, formData["title-fr"]!!), Pair(ENGLISH, formData["title-en"]!!)),
                    slug = mapOf(Pair(FRENCH, formData["title-fr"]!!.toSlug()), Pair(ENGLISH, formData["title-en"]!!.toSlug())),
                    headline = mapOf(Pair(FRENCH, formData["headline-fr"]!!), Pair(ENGLISH, formData["headline-en"]!!)),
                    content = mapOf(Pair(FRENCH, formData["content-fr"]!!), Pair(ENGLISH, formData["content-en"]!!))
            )
            postRepository.save(post).then(seeOther("${properties.baseUri}/admin/blog"))
        }
    }

    private fun Any.toJson() = objectMapper.writeValueAsString(this).replace("\"", "&quot;")

    private fun String.toLinks() = objectMapper.readValue<List<Link>>(this)

    private fun String.toLink() = objectMapper.readValue<Link>(this)
}


