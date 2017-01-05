package mixit.support

class MixitApi {
    companion object {
        /**
         * The event ids are the year on 2 digits. This function converts an id to a year on 4 digits
         */
        fun idToYear(id: String): Int {
            return "20".plus(id).toInt()
        }
    }
}
