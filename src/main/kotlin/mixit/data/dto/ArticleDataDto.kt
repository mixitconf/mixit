package mixit.data.dto

import mixit.model.Article
import mixit.model.Content
import mixit.model.Language
import java.time.LocalDateTime

class ArticleDataDto(
        val id: Long,
        val author: MemberDataDto,
        val postedAt: LocalDateTime = LocalDateTime.now(),
        val title: String,
        val headline: String,
        val content: String,
        val titre: String,
        val resume: String,
        val contenu: String
){
    fun toArticle():Article{
        val contents: Map<Language, Content> = mapOf(
                Pair(Language.FRENCH, Content(titre, replaceImagePath(resume), replaceImagePath(contenu))),
                Pair(Language.ENGLISH, Content(title, replaceImagePath(headline), replaceImagePath(content))))
        return Article(author.toUser(), postedAt, contents, "$id")
    }

    fun replaceImagePath(content: String) : String{
        return content
                .replace("https://www.mix-it.fr/img/articles/", "/images/article/")
                .replace("http://www.mix-it.fr/public/images/articles/", "/images/article/")
                .replace("http://www.mix-it.fr/public/images/logo", "/images/sponsor/logo")
                .replace("https://www.mix-it.fr/img/sponsors/", "/images/sponsor/")
    }
}