package fr.mixit.support

interface HttpServer {
    fun start()
    fun stop()
    val isRunning: Boolean
}