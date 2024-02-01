package mixit.talk.spi

import mixit.talk.model.CachedTalk


interface CfpSynchronizer {

    suspend fun synchronize()
}
