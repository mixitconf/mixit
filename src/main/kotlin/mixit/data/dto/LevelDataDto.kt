package mixit.data.dto

import mixit.model.SponsorshipLevel
import java.time.LocalDate

data class LevelDataDto(
        val key: SponsorshipLevel,
        val value: LocalDate?
)
