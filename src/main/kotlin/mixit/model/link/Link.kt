package mixit.model.link

import org.springframework.data.annotation.Id
import java.time.Instant

/**
 * @author Dev-Mind <guillaume@dev-mind.fr>
 * @since 20/12/16.
 */
// TODO Switch to val + noarg compiler plugin when it will be stable in IDEA
data class Link(
        var name: String = "",
        var url: String = ""
)