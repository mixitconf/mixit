package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.*
import mixit.support.*
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.LocalDate

@Repository
class EventRepository(val template: ReactiveMongoTemplate, val userRepository: UserRepository) {


    fun initData() {
        deleteAll().block()

        val events = listOf(
                Event("mixit12", LocalDate.of(2012, 4, 26), LocalDate.of(2012, 4, 26), sponsors = readSponsorsForEvent(12)),
                Event("mixit13", LocalDate.of(2013, 4, 25), LocalDate.of(2013, 4, 26), sponsors = readSponsorsForEvent(13)),
                Event("mixit14", LocalDate.of(2014, 4, 29), LocalDate.of(2014, 4, 30), sponsors = readSponsorsForEvent(14)),
                Event("mixit15", LocalDate.of(2015, 4, 16), LocalDate.of(2015, 4, 17), sponsors = readSponsorsForEvent(15)),
                Event("mixit16", LocalDate.of(2016, 4, 21), LocalDate.of(2016, 4, 22), sponsors = readSponsorsForEvent(16)),
                Event("mixit17", LocalDate.of(2017, 4, 20), LocalDate.of(2017, 4, 21), true, createSponsorFor2017())
        )
        events.forEach { event -> save(event).block() }
    }

    fun createSponsorFor2017(): List<EventSponsoring>{
        userRepository.save(User("Ippon", "", "", "marketing@ippon.fr ", "Ippon",
                logoUrl = "sponsor/logo-ippon.svg",
                shortDescription = "Ippon is a specialized global consulting on Digital, BigData and Cloud solutions. We serve prestigious customers " +
                        "worldwide with teams of high-level consultants and a deep engagement for quality, performance and time-to-market. " +
                        "\nLocations : France, USA, Australia and morocco \n Key figures : M$25+, 250+ consultants, since 2002 ",
                links = listOf(Link("Site", "http://www.ippon.fr/"), Link("Blog", "http://blog.ippon.fr/")))).block()

        userRepository.save(User("Hopwork", "", "", "contact@hopwork.fr ", "Hopwork",
                logoUrl = "sponsor/logo-hopwork.svg",
                shortDescription = "TODO ",
                links = listOf(Link("Site", "http://www.hopwork.fr/")))).block()

        return listOf(
                EventSponsoring(SponsorshipLevel.GOLD, userRepository.findOne("Zenika Lyon").block(), LocalDate.of(2016, 11, 4)),
                EventSponsoring(SponsorshipLevel.GOLD, userRepository.findOne("Sword").block(), LocalDate.of(2016, 12, 7)),
                EventSponsoring(SponsorshipLevel.GOLD, userRepository.findOne("Ippon").block(), LocalDate.of(2016, 12, 14)),
                EventSponsoring(SponsorshipLevel.GOLD, userRepository.findOne("Sopra Steria").block(), LocalDate.of(2016, 12, 23)),
                EventSponsoring(SponsorshipLevel.GOLD, userRepository.findOne("annick.challancin@esker.fr").block(), LocalDate.of(2017, 1, 10)),
                EventSponsoring(SponsorshipLevel.LANYARD, userRepository.findOne("WorldlineFrance").block(), LocalDate.of(2016, 10, 19)),
                EventSponsoring(SponsorshipLevel.PARTY, userRepository.findOne("onlylyon").block(), LocalDate.of(2017, 1, 1)),
                EventSponsoring(SponsorshipLevel.PARTY_DRINKS, userRepository.findOne("Hopwork").block(), LocalDate.of(2016, 11, 2)),
                EventSponsoring(SponsorshipLevel.SILVER, userRepository.findOne("SerliFr").block(), LocalDate.of(2016, 12, 13)),
                EventSponsoring(SponsorshipLevel.SILVER, userRepository.findOne("SII_rhonealpes").block(), LocalDate.of(2016, 12, 20))
        )
    }

    /**
     * Loads data from the json sponsor files
     */
    fun readSponsorsForEvent(year: Int): List<EventSponsoring> {
        val file = ClassPathResource("data/sponsor/sponsor_mixit$year.json")
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        var sponsors: List<MemberDataDto> = objectMapper.readValue(file.inputStream)
        return sponsors.flatMap { sponsor -> sponsor.toEventSponsoring(userRepository.findOne("${sponsor.login}").block()) }
    }

    fun findAll(): Flux<Event> = template.find(Query().with(Sort("year")), Event::class)

    fun findOne(id: String) = template.findById(id, Event::class)

    fun deleteAll() = template.remove(Query(), Event::class)

    fun save(event: Event) = template.save(event)


}
