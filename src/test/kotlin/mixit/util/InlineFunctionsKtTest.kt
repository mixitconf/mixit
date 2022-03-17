package mixit.util

import mixit.user.model.Role
import mixit.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InlineFunctionsKtTest {
    @Test
    fun enumMatcher() {
        val expected = listOf(
            Pair(Role.STAFF, false),
            Pair(Role.STAFF_IN_PAUSE, false),
            Pair(Role.USER, true),
            Pair(Role.VOLUNTEER, false)
        )
        val user = User().copy(role = Role.USER)
        assertEquals(enumMatcher(user) { it.role }, expected)
    }
}
