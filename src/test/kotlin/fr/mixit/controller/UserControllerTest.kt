package fr.mixit.controller

import fr.mixit.model.Models
import fr.mixit.model.User
import fr.mixit.model.UserEntity
import io.requery.Persistable
import io.requery.kotlin.eq
import io.requery.sql.KotlinConfiguration
import io.requery.sql.KotlinEntityDataStore
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import org.h2.jdbcx.JdbcDataSource
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UserControllerTest {

    var instance : KotlinEntityDataStore<Persistable>? = null
    val data : KotlinEntityDataStore<Persistable> get() = instance!!

    @Before
    fun setup() {
        val dataSource = JdbcDataSource()
        dataSource.setUrl("jdbc:h2:~/testh2")
        dataSource.user = "sa"
        dataSource.password = "sa"

        val configuration = KotlinConfiguration(
                dataSource = dataSource,
                model = Models.KT,
                statementCacheSize = 0,
                useDefaultLogging = true)

        instance = KotlinEntityDataStore(configuration)

        val tables = SchemaModifier(configuration)
        tables.dropTables()

        val mode = TableCreationMode.CREATE
        tables.createTables(mode)
    }

    @After
    fun teardown() {
        data.close()
    }

    @Test
    fun findByIdShouldReturnMatchingUser() {
        val user = UserEntity()
        user.id = 1L
        user.name = "userName"

        data.invoke {
            insert(user)
            assertTrue(user.id > 0)
            val result = select(User::class) where (User::id eq user.id) limit 10
            assertSame(result.get().first(), user)
        }

        assertTrue(true)
    }
}
