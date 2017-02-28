package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.model.Article
import mixit.model.Language
import org.springframework.core.io.ClassPathResource
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction
import org.springframework.data.domain.Sort.Order
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import mixit.support.*
import org.springframework.data.mongodb.core.query.Criteria.*
import reactor.core.publisher.Mono


@Repository
class ArticleRepository(val template: ReactiveMongoTemplate) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()
        deleteAll().block()
        val articlesResource = ClassPathResource("data/articles.json")
        val articles: List<Article> = objectMapper.readValue(articlesResource.inputStream)
        articles.forEach { save(it).block() }
    }

    fun findOne(id: String) = template.findById(id, Article::class)

    fun findBySlug(slug: String, lang: Language) : Mono<Article> =
            template.findOne(Query().addCriteria(where("slug.$lang").`is`(slug)))

    fun findAll(lang: Language? = null): Flux<Article> {
        val query = Query()
        query.with(Sort(Order(Direction.DESC, "addedAt")))
        query.fields().exclude("content")
        if (lang != null) {
            query.addCriteria(where("content.$lang").exists(true))
        }
        return template.find(query)
    }

    fun deleteAll() = template.remove(Query(), Article::class)

    fun save(article: Article) = template.save(article)

}


