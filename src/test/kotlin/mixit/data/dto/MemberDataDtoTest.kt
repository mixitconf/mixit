package mixit.data.dto

import mixit.model.Role
import org.junit.Assert.assertTrue
import org.junit.Test

class MemberDataDtoTest {

    private val SPONSOR_LOGO = "logo"
    private val GRAVATAR_HASH = "hash"

    @Test
    fun `Should use Gravatar hash for logoUrl when converting to user`() {
        val member = MemberDataDto(1L, "login", "firstname", "lastname", "email", "company", SPONSOR_LOGO, GRAVATAR_HASH,
                "sessionType", "shortDescription", "shortDescriptionEn", "longDescription", ArrayList(), ArrayList(), ArrayList(), ArrayList())

        assertTrue(GRAVATAR_HASH.equals(member.toUser(ArrayList(), Role.STAFF).logoUrl))
    }


    @Test
    fun `Should use logo for logoUrl when converting to sponsor`() {
        val sponsor = MemberDataDto(1L, "login", "firstname", "lastname", "email", "company", SPONSOR_LOGO, GRAVATAR_HASH,
                "sessionType", "shortDescription", "shortDescriptionEn", "longDescription", ArrayList(), ArrayList(), ArrayList(), ArrayList())

        assertTrue(SPONSOR_LOGO.equals(sponsor.toUser(ArrayList(), Role.SPONSOR).logoUrl))
    }

}