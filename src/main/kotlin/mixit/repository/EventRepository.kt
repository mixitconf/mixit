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
                description = mapOf(Pair(Language.ENGLISH, "Ippon is a specialized global consulting on Digital, BigData and Cloud solutions." +
                    "We serve prestigious customers worldwide with teams of high-level consultants and a deep engagement for quality, performance and time-to-market." +
                    "Locations: France, USA, Australia and morocco. Key figures: M$25+, 250+ consultants, since 2002."),
                    Pair(Language.FRENCH, "Ippon est spécialisé dans le consulting Digital, Big Data et solutions Cloud." +
                    "Nous nous occupons de clients prestigieux dans le monde entier avec des équipes de consultants hautement qualifiés et un fort engagement en termes de qualité, de performance et de time-to-market." +
                    "Localisations : France, USA, Australie et Maroc. Chiffres clés: M$25+, 250+ consultants, depuis 2002.")),
                links = listOf(Link("Site", "http://www.ippon.fr/"), Link("Blog", "http://blog.ippon.fr/")))).block()

        userRepository.save(User("Hopwork", "", "", "contact@hopwork.fr", "Hopwork",
                logoUrl = "sponsor/logo-hopwork.svg",
                description = mapOf(Pair(Language.ENGLISH, "Best freelancers. Available now. Complete safety."), Pair(Language.FRENCH, "Les meilleurs freelances. Disponibles maintenant. En toute sécurité.")),
                links = listOf(Link("Site", "http://www.hopwork.fr/")))).block()

        userRepository.save(User("LDLC", "", "", "c.bar@ldlc.com", "LDLC",
                logoUrl = "sponsor/logo-ldlc.png",
                description = mapOf(Pair(Language.ENGLISH, "Created in 1996 by the entrepreneur Laurent de la Clergerie from Lyon," +
                    "LDLC Group is currently the 5th e-commerce group in France. Its major shop, LDLC.com, is the french leader of computer and high-tech e-commerce." +
                    "Composed by 10 brands including 6 retail websites, the e-commerce pioneer gathers computing, high-tech, house and education activities."),
                    Pair(Language.FRENCH, "Créé en 1996 par l’entrepreneur lyonnais Laurent de la Clergerie," +
                    "le Groupe LDLC est aujourd’hui le 5ème groupe de e-commerce en France. Son enseigne majeure, LDLC.com, est leader français du e-commerce informatique" +
                    "et high-tech. Constitué de 10 marques dont 6 sites marchands, le pionnier du commerce en ligne conjugue des activités dans le domaine de l’informatique," +
                    "du high-tech, de la maison ou encore de l’éducation.")),
                links = listOf(Link("Site", "http://www.groupe-ldlc.com/"), Link("Site public", "http://www.ldlc.com/"),
                        Link("Site pro", "http://www.ldlc-pro.com/"), Link("Linkedin", "https://www.linkedin.com/company/ldlc"),
                        Link("Twitter", "https://twitter.com/groupeldlc"), Link("Facebook", "https://www.facebook.com/LDLC.com/")))).block()

        userRepository.save(User("Algolia", "", "", "hey@algolia.com?subject=MiXit", "Algolia",
                logoUrl = "sponsor/logo-algolia.svg",
                description = mapOf(Pair(Language.ENGLISH, "Founded in 2012, Algolia is a privately held company created by veterans in the fields of algorithms, search engines and text mining."),
                 Pair(Language.FRENCH, "Créée en 2012, Algolia est une société privée créée par des experts dans les domaines des algorithmes relatifs aux moteurs de recherche et à l'analyse de textes.")),
                links = listOf(Link("Site", "https://www.algolia.com/"), Link("Google+", "https://plus.google.com/+Algolia"), Link("Linkedin", "https://www.linkedin.com/company/algolia"),
                        Link("Twitter", "https://twitter.com/Algolia"), Link("Facebook", "https://www.facebook.com/algolia")))).block()

        userRepository.save(User("Enedis", "", "", "contact@enedis.fr", "Enedis",
                logoUrl="sponsor/logo-enedis.svg",
                description = mapOf(Pair(Language.FRENCH, "Enedis est une entreprise de service public, gestionnaire du réseau de distribution d'électricité qui développe, exploite, modernise le réseau électrique et gère les données associées."),
                Pair(Language.ENGLISH, "Enedis is a public company, electricity distribution network operator which develops, operates, modernizes the electricity grid and manages the associated data.")),
                links = listOf(Link("Site", "http://www.enedis.fr")))).block()

        val sword = userRepository.findOne("Sword").block()
        sword.description =  mapOf(Pair(Language.FRENCH, "SWORD, c’est 1 500 spécialistes en IT/Digital & Software actifs sur les 5 continents, qui vous accompagnent dans l’évolution " +
            "de votre organisation à l’ère du numérique. Depuis 2000, SWORD a acquis une solide réputation dans l’édition de logiciels et dans la conduite de " +
            "projets IT & business complexes en tant que leader de la transformation technologique et digitale. Avec SWORD c’est la garantie d’un engagement " +
            "de proximité dont le but est d’optimiser vos processus & de valoriser vos données. Notre volonté est avant tout de construire dans la durée et de " +
            "fidéliser nos collaborateurs, nos clients et nos partenaires en nous appuyant sur nos valeurs et notre enthousiasme qui vous accompagneront tout " +
            "au long de vos projets."), Pair(Language.ENGLISH, "Sword Group is an international Consulting, Service and Software company driving global leaders in" +
            "their digital & technology transformation. Created in November 2000, the group has a current headcount of 1,600+ staff operating in more than 50 countries." +
            "Sword offers its customers comprehensive and integrated responses, on both the strategic approach and the execution."))
        sword.links = listOf(Link("Site", "http://www.sword-services.com/fr/"))
        sword.logoUrl = "sponsor/logo-sword.svg"
        userRepository.save(sword).block()

        val worldline = userRepository.findOne("WorldlineFrance").block()
        worldline.description = mapOf(Pair(Language.FRENCH, "Worldline [Euronext : WLN] est le leader européen et un acteur mondial de référence dans le secteur des paiements et des services " +
            "transactionnels. Worldline met en place des services nouvelle génération, permettant à ses clients d’offrir au consommateur final des solutions " +
            "innovantes et fluides. Acteur clef du B2B2C, riche de plus de 40 ans d’expérience, Worldline sert et contribue au succès de toutes les entreprises " +
            "et administrations, dans un marché en perpétuelle évolution. Worldline propose un Business Model unique et flexible, construit autour d’un portefeuille " +
            "d’offres évolutif et global permettant une prise en charge end-to-end. Les activités de Worldline sont organisées autour de trois axes : Merchant Services " +
            "& Terminals, Mobility & e-Transactional Services, Financial Processing & Software Licensing incluant equensWorldline. Worldline emploie plus de 8 600 " +
            "collaborateurs dans le monde entier et génère un chiffre d’affaires estimé à  environ 1,5 milliard d’euros sur une base annuelle. Worldline est une entreprise " +
            "du Groupe Atos."), Pair(Language.ENGLISH, "Worldline [Euronext: WLN] is the European leader in the payments and transactional services industry." +
            "Worldline delivers new-generation services, enabling its customers to offer smooth and innovative solutions to the end consumer." +
            "Key actor for B2B2C industries, with over 40 years of experience, Worldline supports and contributes to the success of all businesses and administrative services" +
            "in a perpetually evolving market. Worldline offers a unique and flexible business model built around a global and growing portfolio, thus enabling end-to-end support." +
            "Worldline activities are organized around three axes: Merchant Services, Mobility & e-Transactional Services, Financial Processing & Software Licensing including equensWorldline." +
            "Worldline employs more than 8,600 people worldwide, with estimated revenue of circa 1.5 billion euros on a yearly basis. Worldline is an Atos company"))
        worldline.links = listOf(
            Link("Site", "http://fr.worldline.com"), Link("Blog", "http://fr.worldline.com/blog"), Link("Twitter", "https://twitter.com/WorldlineGlobal"),
            Link("YouTube", "http://fr.worldline.com/youtube"), Link("Facebook", "http://fr.worldline.com/facebook"))
        worldline.logoUrl = "sponsor/logo-worldline.svg"
        userRepository.save(worldline).block()

        val pivotal = userRepository.findOne("pivotal").block()
        pivotal.description = mapOf(Pair(Language.ENGLISH, "Pivotal’s Cloud-Native platform drives software innovation for many of the world’s most admired brands. With millions of developers in communities around the world," +
            "Pivotal technology touches billions of users every day. After shaping the software development culture of Silicon Valley's most valuable companies for over a decade, today Pivotal " +
            "leads a global technology movement transforming how the world builds software."), Pair(Language.FRENCH, "La plateforme cloud native de Pivotal s’impose comme une force d’innovation logicielle pour les plus grandes" +
            "marques internationales. Chaque jour, les millions de développeurs des communautés open source du monde entier permettent à nos technologies d’atteindre des milliards d'utilisateurs. Pendant plus d’une décennie," +
            "Pivotal a su insuffler la culture du développement logiciel chez les plus grands groupes de la Silicon Valley. Aujourd’hui, nous nous plaçons en chef de file d’un mouvement mondial qui révolutionne les méthodes de conception des logiciels."))
        pivotal.links = listOf(Link("Site", "http://run.pivotal.io/"), Link("Pivotal", "http://pivotal.io/"))
        userRepository.save(pivotal).block()

        val zenika = userRepository.findOne("Zenika Lyon").block()
        zenika.description = mapOf(Pair(Language.FRENCH, "Spécialisé dans l'architecture informatique et les méthodes agiles, Zenika compte 200 collaborateurs répartis sur des domaines aussi variés que l'expertise Java, le BigData, le Web, la Mobilité, l'IoT, DevOps, le Craftsmanship et l'Agilité."),
            Pair(Language.ENGLISH, "IT architecture and Agile methods specialist, Zenika has 200 employees dispatched on various fields like Java, BigData, Web, Mobility, IoT, DevOps, Craftsmanship and Agile."))
        zenika.logoUrl = "sponsor/logo-zenika.png"
        userRepository.save(zenika).block()

        val sopraSteria = userRepository.findOne("Sopra Steria").block()
        sopraSteria.description = mapOf(Pair(Language.FRENCH, "Sopra Steria, leader européen de la transformation numérique, propose l’un des portefeuilles d’offres les plus complets du marché : conseil, intégration" +
            "de systèmes, édition de solutions métier, infrastructure management et business process services."),
            Pair(Language.ENGLISH, "Sopra Steria, a European leader in digital transformation, provides one of the most comprehensive portfolios of end-to-end service offerings on the market: consulting, systems integration," +
            "software development, infrastructure management and business process services. Sopra Steria is trusted by leading private and public-sector organisations to deliver successful transformation programmes that address" +
            "their most complex and critical business challenges."))
        sopraSteria.logoUrl = "sponsor/logo-sopra-steria.svg"
        userRepository.save(sopraSteria).block()

        val onlylyon = userRepository.findOne("onlylyon").block()
        onlylyon.description = mapOf(Pair(Language.FRENCH, "OnlyLyon a été créé en 2007 pour assurer la promotion internationale de Lyon."),
            Pair(Language.ENGLISH, "OnlyLyon has been created in 2007 to promote Lyon worldwide."))
        onlylyon.logoUrl = "sponsor/logo-onlylyon.png"
        userRepository.save(onlylyon).block()

        val sii = userRepository.findOne("SII_rhonealpes").block()
        sii.description = mapOf(Pair(Language.FRENCH, "SII Rhône Alpes: une agence dynamique et en plein développement."),
            Pair(Language.ENGLISH, "SII Rhône Alpes: a dynamic agency that keeps growing."))
        sii.logoUrl = "sponsor/logo-sii.svg"
        userRepository.save(sii).block()

        return listOf(
                EventSponsoring(GOLD, userRepository.findOne("Zenika Lyon").block(), LocalDate.of(2016, 11, 4)),
                EventSponsoring(GOLD, sword, LocalDate.of(2016, 12, 7)),
                EventSponsoring(GOLD, userRepository.findOne("Ippon").block(), LocalDate.of(2016, 12, 14)),
                EventSponsoring(GOLD, sopraSteria, LocalDate.of(2016, 12, 23)),
                EventSponsoring(GOLD, userRepository.findOne("annick.challancin@esker.fr").block(), LocalDate.of(2017, 1, 10)),
                EventSponsoring(GOLD, userRepository.findOne("LDLC").block(), LocalDate.of(2017, 1, 20)),
                EventSponsoring(GOLD, userRepository.findOne("VISEO").block(), LocalDate.of(2017, 2, 20)),
                EventSponsoring(LANYARD, worldline, LocalDate.of(2016, 10, 19)),
                EventSponsoring(PARTY, onlylyon, LocalDate.of(2017, 1, 1)),
                EventSponsoring(PARTY, userRepository.findOne("Hopwork").block(), LocalDate.of(2016, 11, 2)),
                EventSponsoring(SILVER, userRepository.findOne("SerliFr").block(), LocalDate.of(2016, 12, 13)),
                EventSponsoring(SILVER, sii, LocalDate.of(2016, 12, 20)),
                EventSponsoring(SILVER, userRepository.findOne("woonoz").block(), LocalDate.of(2017, 1, 20)),
                EventSponsoring(SILVER, userRepository.findOne("Algolia").block(), LocalDate.of(2017, 1, 23)),
                EventSponsoring(SILVER, userRepository.findOne("Enedis").block(), LocalDate.of(2017, 1, 24)),
                EventSponsoring(SILVER, userRepository.findOne("sourcingisr").block(), LocalDate.of(2017, 1, 25)),
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
