package mixit.data.dto

import mixit.model.Article
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
    fun toArticle() : Article{
        val title: Map<Language, String> = mapOf(
                Pair(Language.FRENCH, titre),
                Pair(Language.ENGLISH, title)
        )
        val content: Map<Language, String> = mapOf(
                Pair(Language.FRENCH, replaceImagePath(resume + "\n" + contenu)),
                Pair(Language.ENGLISH, replaceImagePath(headline + "\n" + content))
        )
        return Article(author.toUser(), postedAt, title, content, "$id")
    }

    fun replaceImagePath(content: String) : String{
        return content
                .replace("https://www.mix-it.fr/img/articles/", "/images/article/")
                .replace("http://www.mix-it.fr/public/images/articles/", "/images/article/")
                .replace("http://www.mix-it.fr/public/images/logo", "/images/sponsor/logo")
                .replace("https://www.mix-it.fr/img/sponsors/", "/images/sponsor/")
    }
}