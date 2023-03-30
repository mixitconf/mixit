package mixit.routes

object Routes {
    const val GOOGLE_DRIVE_URI = "https://drive.google.com/open"

    val securedAdminUrl: List<String> = listOf("/admin", "/api/admin")
    val securedVolunteerUrl: List<String> = listOf("/volunteer")
    val securedUrl: List<String> = listOf("/me", "/api/favorites")
    const val mixetteQRCode: String = "/volunteer/mixette-donation/create/"
}
