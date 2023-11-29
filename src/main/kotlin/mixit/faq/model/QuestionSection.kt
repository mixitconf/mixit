package mixit.faq.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

data class Text(
    val descriptionFr: String,
    val descriptionEn: String
)

data class Question(
    val title: Text,
    val answer: Text,
    val order: Int,
    val id: String
)

@Document
data class QuestionSection(
    val title: Text,
    val questions: List<Question>,
    val order: Int,
    @Id val id: String? = null,
)
