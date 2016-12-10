package mixit

import mixit.controller.Controllers
import mixit.controller.ResourceController
import mixit.controller.UserController
import mixit.model.Models
import mixit.model.UserEntity
import mixit.service.UserService
import mixit.support.HttpServer
import mixit.support.TomcatHttpServer
import mixit.support.register
import mixit.support.getBean
import io.requery.Persistable
import io.requery.sql.*
import org.h2.jdbcx.JdbcDataSource
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.support.GenericApplicationContext

class Application {

    val appContext: GenericApplicationContext
    val server: HttpServer

    constructor() {
        appContext = appContext()
        appContext.refresh()
        server = appContext.getBean(HttpServer::class)
    }

    private fun appContext() : GenericApplicationContext {
        val beanFactory = DefaultListableBeanFactory()
        beanFactory.registerSingleton("dataStore", dataStore())
        beanFactory.register(UserService::class)
        beanFactory.register(UserController::class)
        beanFactory.register(ResourceController::class)
        beanFactory.register(Controllers::class)
        beanFactory.register(TomcatHttpServer::class)
        return GenericApplicationContext(beanFactory)
    }

    private fun dataStore() : KotlinEntityDataStore<Persistable>  {
        val dataSource: JdbcDataSource
        dataSource = JdbcDataSource()
        dataSource.setUrl("jdbc:h2:~/testh2")
        dataSource.user = "sa"
        dataSource.password = "sa"

        val configuration = KotlinConfiguration(
                dataSource = dataSource,
                model = Models.KT,
                statementCacheSize = 0,
                useDefaultLogging = true)
        val dataStore: KotlinEntityDataStore<Persistable> = KotlinEntityDataStore(configuration)
        initDataStore(dataStore, configuration)
        return dataStore
    }

    private fun initDataStore(dataStore: KotlinEntityDataStore<Persistable>, configuration: KotlinConfiguration) {
        val tables = SchemaModifier(configuration)
        tables.dropTables()
        val mode = TableCreationMode.CREATE
        tables.createTables(mode)

        val user1 = UserEntity()
        user1.id = 1L
        user1.name = "Robert"
        dataStore.insert(user1)

        val user2 = UserEntity()
        user2.id = 2L
        user2.name = "Raide"
        dataStore.insert(user2)

        val user3 = UserEntity()
        user3.id = 3L
        user3.name = "Ford"
        dataStore.insert(user3)
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
        appContext.destroy()
    }
}