package mixit.faq.model

import mixit.talk.model.Language
import mixit.talk.model.Language.ENGLISH
import mixit.talk.model.Language.FRENCH
import mixit.util.hasFoundOccurrences
import mixit.util.markFoundOccurrences
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.TextIndexed
import org.springframework.data.mongodb.core.mapping.Document

data class Text(
    val descriptionFr: String,
    val descriptionEn: String
) {
    fun toIndex(): String = "$descriptionFr $descriptionEn".lowercase()
}

data class Question(
    val title: Text,
    val answer: Text,
    val order: Int,
    val id: String
) {
    fun toIndex(): String = "${title.toIndex()} ${answer.toIndex()}"

    fun hasFoundOccurrences(language: Language, searchTerms: List<String>): Boolean =
        if (language == FRENCH) {
            title.descriptionFr.hasFoundOccurrences(searchTerms) || answer.descriptionFr.hasFoundOccurrences(searchTerms)
        } else {
            title.descriptionEn.hasFoundOccurrences(searchTerms) || answer.descriptionEn.hasFoundOccurrences(searchTerms)
        }
}

@Document
data class QuestionSection(
    val title: Text,
    val questions: List<Question>,
    val order: Int,
    @Id val id: String? = null,
    @TextIndexed(weight = 1F) val index: String =
        (listOf(title.toIndex()) + questions.map { it.toIndex() }).joinToString()
) {
    fun markFoundOccurrences(language: Language, searchTerms: List<String>): QuestionSection? =
        if (language == FRENCH) markFoundOccurrencesFr(searchTerms) else markFoundOccurrencesEn(searchTerms)

    fun hasFoundOccurrences(language: Language, searchTerms: List<String>): Boolean =
        if (language == FRENCH)
            title.descriptionFr.hasFoundOccurrences(searchTerms)
        else
            title.descriptionEn.hasFoundOccurrences(searchTerms)

    /**
     * Mark found occurrences in the French version of the question section and if nothing is found returns null
     */
    private fun markFoundOccurrencesEn(searchTerms: List<String>): QuestionSection? =
        if (searchTerms.isEmpty()) this else {
            if (!hasFoundOccurrences(ENGLISH, searchTerms) || questions.all { !it.hasFoundOccurrences(ENGLISH, searchTerms) }) {
                null
            } else {
                this.copy(
                    title = title.copy(
                        descriptionEn = title.descriptionEn.markFoundOccurrences(searchTerms) ?: title.descriptionEn
                    ),
                    questions = questions.mapNotNull {
                        if (!it.hasFoundOccurrences(ENGLISH, searchTerms)) {
                            null
                        } else {
                            it.copy(
                                title = it.title.copy(
                                    descriptionEn = it.title.descriptionEn.markFoundOccurrences(searchTerms)
                                ),
                                answer = it.answer.copy(
                                    descriptionEn = it.answer.descriptionEn.markFoundOccurrences(searchTerms)
                                )
                            )
                        }
                    }
                )
            }
        }

    private fun markFoundOccurrencesFr(searchTerms: List<String>): QuestionSection? =
        if (searchTerms.isEmpty()) this else {
            if (!hasFoundOccurrences(FRENCH, searchTerms) || questions.all { !it.hasFoundOccurrences(FRENCH, searchTerms) }) {
                null
            } else {
                this.copy(
                    title = title.copy(
                        descriptionFr = title.descriptionFr.markFoundOccurrences(searchTerms)
                    ),
                    questions = questions.mapNotNull {
                        if (!it.hasFoundOccurrences(ENGLISH, searchTerms)) {
                            null
                        } else {
                            it.copy(
                                title = it.title.copy(
                                    descriptionFr = it.title.descriptionFr.markFoundOccurrences(searchTerms)
                                ),
                                answer = it.answer.copy(
                                    descriptionFr = it.answer.descriptionFr.markFoundOccurrences(searchTerms)
                                )
                            )
                        }
                    }
                )
            }
        }
}
