package mixit.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import mixit.data.dto.ArticleDataDto
import mixit.model.Article
import mixit.support.getEntityInformation
import org.springframework.core.io.ClassPathResource
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.support.ReactiveMongoRepositoryFactory
import org.springframework.data.mongodb.repository.support.SimpleReactiveMongoRepository
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder


class ArticleRepository(db: ReactiveMongoTemplate, f: ReactiveMongoRepositoryFactory) :
        SimpleReactiveMongoRepository<Article, String>(f.getEntityInformation(Article::class), db) {

    fun initData() {
        val objectMapper: ObjectMapper = Jackson2ObjectMapperBuilder.json().build()

        deleteAll().block()

        val articleResource = ClassPathResource("data/article_mixit.json")
        val articles: List<ArticleDataDto> = objectMapper.readValue(articleResource.file)
        articles
                .map { article -> article.toArticle() }
                .forEach { article -> save(article).block() }
    }

}
