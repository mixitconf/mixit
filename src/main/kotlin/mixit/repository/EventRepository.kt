package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.MemberDataDto
import mixit.model.*
import mixit.model.SponsorshipLevel.*
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

    fun yearToId(year:String): String = "mixit${year.substring(2)}"

    fun createSponsorFor2017(): List<EventSponsoring>{
        userRepository.save(User("Ippon", "", "", "marketing@ippon.fr", "Ippon",
                logoUrl = "sponsor/logo-ippon.svg",
                shortDescription = "Ippon is a specialized global consulting on Digital, BigData and Cloud solutions. We serve prestigious customers " +
                        "worldwide with teams of high-level consultants and a deep engagement for quality, performance and time-to-market. " +
                        "\nLocations : France, USA, Australia and morocco \n Key figures : M$25+, 250+ consultants, since 2002 ",
                longDescription = "Ippon is a specialized global consulting on Digital, BigData and Cloud solutions. We serve prestigious customers " +
                        "worldwide with teams of high-level consultants and a deep engagement for quality, performance and time-to-market. " +
                        "\nLocations : France, USA, Australia and morocco \n Key figures : M$25+, 250+ consultants, since 2002 ",
                links = listOf(Link("Site", "http://www.ippon.fr/"), Link("Blog", "http://blog.ippon.fr/")))).block()

        userRepository.save(User("Hopwork", "", "", "contact@hopwork.fr", "Hopwork",
                logoUrl = "sponsor/logo-hopwork.svg",
                shortDescription = "Les meilleurs freelances. Disponibles maintenant. En toute sécurité.",
                longDescription = "Les meilleurs freelances. Disponibles maintenant. En toute sécurité.",
                links = listOf(Link("Site", "http://www.hopwork.fr/")))).block()

        userRepository.save(User("LDLC", "", "", "c.bar@ldlc.com", "LDLC",
                logoUrl = "sponsor/logo-ldlc.png",
                shortDescription = "Vente de matériel informatique en ligne, particuliers ou pro.",
                longDescription = "Vente de matériel informatique en ligne, particuliers ou pro.",
                links = listOf(Link("Site", "http://www.groupe-ldlc.com/"), Link("Site public", "http://www.ldlc.com/"),
                        Link("Site pro", "http://www.ldlc-pro.com/"), Link("Linkedin", "https://www.linkedin.com/company/ldlc"),
                        Link("Twitter", "https://twitter.com/groupeldlc"), Link("Facebook", "https://www.facebook.com/LDLC.com/")))).block()

        userRepository.save(User("Algolia", "", "", "hey@algolia.com?subject=MiXit", "Algolia",
                logoUrl = "sponsor/logo-algolia.svg",
                shortDescription = "Founded in 2012, Algolia is a privately held company created by veterans in the fields of algorithms, search engines and text mining.",
                longDescription = "We help users deliver an intuitive search-as-you-type experience on their websites and mobile apps. Basically, we make finding things easier. We are a customer-focused, dev-centric company dedicated to changing the way people think about search.",
                links = listOf(Link("Site", "https://www.algolia.com/"), Link("Google+", "https://plus.google.com/+Algolia"), Link("Linkedin", "https://www.linkedin.com/company/algolia"),
                        Link("Twitter", "https://twitter.com/Algolia"), Link("Facebook", "https://www.facebook.com/algolia")))).block()

        userRepository.save(User("Enedis", "", "", "contact@enedis.fr", "Enedis",
                logoUrl="sponsor/logo-enedis.svg",
                shortDescription = "Enedis est une entreprise de service public, gestionnaire du réseau de distribution d'électricité qui développe, exploite, modernise le réseau " +
                        "électrique et gère les données associées.",
                longDescription = "Indépendante des fournisseurs d'énergie chargés de la vente et de la gestion du contrat d'électricité, Enedis réalise " +
                        "les raccordements, le dépannage, le relevé des compteurs et toutes interventions techniques.",
                links = listOf(Link("Site", "http://www.enedis.fr")))).block()

        val sword = userRepository.findOne("Sword").block()
        sword.longDescription = "SWORD, c’est 1 500 spécialistes en IT/Digital & Software actifs sur les 5 continents, qui vous accompagnent dans l’évolution " +
                "de votre organisation à l’ère du numérique. Depuis 2000, SWORD a acquis une solide réputation dans l’édition de logiciels et dans la conduite de " +
                "projets IT & business complexes en tant que leader de la transformation technologique et digitale.\n\nAvec SWORD c’est la garantie d’un engagement " +
                "de proximité dont le but est d’optimiser vos processus & de valoriser vos données. Notre volonté est avant tout de construire dans la durée et de " +
                "fidéliser nos collaborateurs, nos clients et nos partenaires en nous appuyant sur nos valeurs et notre enthousiasme qui vous accompagneront tout " +
                "au long de vos projets."
        sword.links = listOf(Link("Site", "http://www.sword-services.com/fr/"))
        sword.logoUrl = "sponsor/logo-sword.svg"
        userRepository.save(sword).block()

        val worldline = userRepository.findOne("WorldlineFrance").block()
        worldline.longDescription = "Worldline [Euronext : WLN] est le leader européen et un acteur mondial de référence dans le secteur des paiements et des services " +
                "transactionnels. Worldline met en place des services nouvelle génération, permettant à ses clients d’offrir au consommateur final des solutions " +
                "innovantes et fluides. \n\nActeur clef du B2B2C, riche de plus de 40 ans d’expérience, Worldline sert et contribue au succès de toutes les entreprises " +
                "et administrations, dans un marché en perpétuelle évolution. Worldline propose un Business Model unique et flexible, construit autour d’un portefeuille " +
                "d’offres évolutif et global permettant une prise en charge end-to-end. Les activités de Worldline sont organisées autour de trois axes : Merchant Services " +
                "& Terminals, Mobility & e-Transactional Services, Financial Processing & Software Licensing incluant equensWorldline. Worldline emploie plus de 8 600 " +
                "collaborateurs dans le monde entier et génère un chiffre d’affaires estimé à  environ 1,5 milliard d’euros sur une base annuelle. Worldline est une entreprise " +
                "du Groupe Atos. worldline.com"
        worldline.links = listOf(
                Link("Site", "http://fr.worldline.com"), Link("Blog", "http://fr.worldline.com/blog"), Link("Twitter", "https://twitter.com/WorldlineGlobal"),
                Link("YouTube", "http://fr.worldline.com/youtube"), Link("Facebook", "http://fr.worldline.com/facebook"))
        worldline.logoUrl = "sponsor/logo-worldline.svg"
        userRepository.save(worldline).block()

        val pivotal = userRepository.findOne("pivotal").block()
        pivotal.links = listOf(Link("Site", "http://run.pivotal.io/"), Link("Pivotal", "http://pivotal.io/"))
        userRepository.save(pivotal).block()

        val zenika = userRepository.findOne("Zenika Lyon").block()
        zenika.logoUrl = "sponsor/logo-zenika.png"
        userRepository.save(zenika).block()

        val sopraSteria = userRepository.findOne("Sopra Steria").block()
        sopraSteria.logoUrl = "sponsor/logo-sopra-steria.png"
        userRepository.save(sopraSteria).block()

        val onlylyon = userRepository.findOne("onlylyon").block()
        onlylyon.logoUrl = "sponsor/logo-onlylyon.png"
        userRepository.save(onlylyon).block()

        val sii = userRepository.findOne("SII_rhonealpes").block()
        sii.logoUrl = "sponsor/logo-sii.svg"
        userRepository.save(sii).block()

        return listOf(
                EventSponsoring(GOLD, userRepository.findOne("Zenika Lyon").block(), LocalDate.of(2016, 11, 4)),
                EventSponsoring(GOLD, sword, LocalDate.of(2016, 12, 7)),
                EventSponsoring(GOLD, userRepository.findOne("Ippon").block(), LocalDate.of(2016, 12, 14)),
                EventSponsoring(GOLD, sopraSteria, LocalDate.of(2016, 12, 23)),
                EventSponsoring(GOLD, userRepository.findOne("annick.challancin@esker.fr").block(), LocalDate.of(2017, 1, 10)),
                EventSponsoring(GOLD, userRepository.findOne("LDLC").block(), LocalDate.of(2017, 1, 20)),
                EventSponsoring(LANYARD, worldline, LocalDate.of(2016, 10, 19)),
                EventSponsoring(PARTY, onlylyon, LocalDate.of(2017, 1, 1)),
                EventSponsoring(PARTY, userRepository.findOne("Hopwork").block(), LocalDate.of(2016, 11, 2)),
                EventSponsoring(SILVER, userRepository.findOne("SerliFr").block(), LocalDate.of(2016, 12, 13)),
                EventSponsoring(SILVER, sii, LocalDate.of(2016, 12, 20)),
                EventSponsoring(SILVER, userRepository.findOne("woonoz").block(), LocalDate.of(2017, 1, 20)),
                EventSponsoring(SILVER, userRepository.findOne("Algolia").block(), LocalDate.of(2017, 1, 23)),
                EventSponsoring(SILVER, userRepository.findOne("Enedis").block(), LocalDate.of(2017, 1, 24)),
                EventSponsoring(HOSTING, userRepository.findOne("pivotal").block(), LocalDate.of(2017, 1, 20))
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
