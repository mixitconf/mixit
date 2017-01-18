package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.ArticleDataDto
import mixit.model.Article
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


@Repository
class ArticleRepository(val template: ReactiveMongoTemplate) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        deleteAll().block()

        val articleResource = ClassPathResource("data/article_mixit.json")
        val articles: List<ArticleDataDto> = objectMapper.readValue(articleResource.inputStream)
        articles
                .map(ArticleDataDto::toArticle)
                .forEach { article -> save(article).block() }
    }

    fun findOne(id: String) = template.findById(id, Article::class)

    fun findAll(): Flux<Article> {
        val query = Query()
        query.with(Sort(Order(Direction.DESC, "addedAt")))
        query.fields().exclude("content")
        return template.find(query)
    }

    fun deleteAll() = template.remove(Query(), Article::class)

    fun save(article: Article) = template.save(article)

}


