package mixit.support

interface Server {
    fun start()
    fun stop()
    val isRunning: Boolean
}