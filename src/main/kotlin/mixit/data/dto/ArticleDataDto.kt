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
        val headline: Map<Language, String> = mapOf(
                Pair(Language.FRENCH, resume.replace("/member/member", "/user")),
                Pair(Language.ENGLISH, headline.replace("/member/member", "/user"))
        )
        val content: Map<Language, String> = mapOf(
                Pair(Language.FRENCH, contenu.replace("/member/member", "/user")),
                Pair(Language.ENGLISH, content.replace("/member/member", "/user"))
        )
        return Article(author.toUser(), postedAt, title, headline, content, "$id")
    }
}