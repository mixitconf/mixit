package mixit.util

/**
 * Parse an enum and try to return a list with a boolean to indicate which value an entity used
 */
inline fun <reified E : Enum<E>, reified T> enumMatcher(entity: T?, getter: (T?) -> E?): List<Pair<E, Boolean>> =
    enumValues<E>().sorted().map { Pair(it, it == getter.invoke(entity)) }.toList()

inline fun <reified E : Enum<E>, reified T> enumMatcherWithI18nKey(
    entity: T,
    i18nPrefix: String,
    getter: (T) -> E
): List<Triple<E, String, Boolean>> =
    enumValues<E>()
        .sorted()
        .map { Triple(it, "$i18nPrefix.${it.name.lowercase()}", it == getter.invoke(entity)) }
        .toList()
