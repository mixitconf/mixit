package mixit.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

// TODO Switch to val + noarg compiler plugin when it will be stable in IDEA
@Document
data class User(@Id var id: String = "", var firstname: String = "", var lastname: String = "")
