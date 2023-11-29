package mixit.faq.model

import mixit.util.cache.Cached

data class CachedQuestionSection(
    override val id: String,
    val title: Text,
    val questions: List<Question>,
    val order: Int,
) : Cached {
    fun toQuestionSection(): QuestionSection =
        QuestionSection(
            questions = questions,
            title = title,
            order = order,
            id = id
        )
}
