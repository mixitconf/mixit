package mixit.feedback.model

import mixit.talk.model.TalkFormat

enum class Feedback(val sort: Int, val formats: List<TalkFormat>) {
    COULD_BE_MORE_EXPLORED(10, TalkFormat.entries.toList()),
    FUN(11, TalkFormat.entries.toList()),
    LACKS_DEMO(3, listOf(TalkFormat.TALK, TalkFormat.LIGHTNING_TALK, TalkFormat.WORKSHOP)),
    LEARNT_NOTHING(4, TalkFormat.entries.toList()),
    LEARNT_SOMETHING(0, TalkFormat.entries.toList()),
    LOVED_LIVE_CODING(7, listOf(TalkFormat.TALK, TalkFormat.LIGHTNING_TALK)),
    SURPRISED_BY_SUBJECT(13, listOf(TalkFormat.KEYNOTE)),
    TOO_COMPLICATED(1, TalkFormat.entries.toList()),
    TOO_FAST(2, TalkFormat.entries.toList()),
    TOO_SLOW(5, TalkFormat.entries.toList()),
    UNCLEAR(8, TalkFormat.entries.toList()),
    UNDERSTOOD_NOTHING(12, TalkFormat.entries.toList()),
    VERY_GOOD_TALK(6, TalkFormat.entries.toList()),
    VERY_INTERESTING(9, TalkFormat.entries.toList()),
}
