package mixit.util

import mixit.model.Role
import mixit.model.User
import org.junit.jupiter.api.Assertions.*
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