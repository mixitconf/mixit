package mixit.web.routes

object Routes {
    const val GOOGLE_DRIVE_URI = "https://drive.google.com/open"

    val securedAdminUrl: List<String> = listOf("/admin", "/api/admin")
    val securedUrl: List<String> = listOf("/me", "/api/favorites")
}
