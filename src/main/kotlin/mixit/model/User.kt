package mixit.model

import io.requery.Entity
import io.requery.Generated
import io.requery.Key
import io.requery.Persistable

@Entity(model = "kt")
interface User : Persistable {
    @get:Key
    @get:Generated
    var id: Long
    var name: String

    data class Instance(override var id: Long, override var name: String) : User
}

