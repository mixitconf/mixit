package mixit.mixette.handler

import mixit.MixitProperties
import mixit.event.model.EventService
import mixit.mixette.repository.MixetteDonationRepository
import mixit.user.model.UserService
import org.springframework.stereotype.Component

@Component
class MixetteHandler(
    private val repository: MixetteDonationRepository,
    private val userService: UserService,
    private val service: EventService,
    private val properties: MixitProperties
) {

    /**
     * This handler is used to redirect a scan to donation form if donation is available and if user is an admin.
     * If user has no right, or is not connected user is redirected the scan on the attendee profile only if mixette
     * feature is enabled
     */
//    fun scanToDonationOrProfile(req: ServerRequest): Mono<ServerResponse> {
//        req.currentUserEmail()
//            .flatMap { currentUserEmail -> userService.findByEmail(currentUserEmail).collectList() }
//            .switchIfEmpty { Mono.just(emptyList<Favorite>()) }
//    }
}
