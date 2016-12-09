package fr.mixit.model

import javax.persistence.Entity

@Entity
class User(var id:Long, var name:String) {

    override fun toString(): String {
        return "fr.mixit.User(id='$id', name='$name')"
    }
}