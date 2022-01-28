package mixit.security.handler

enum class LoginError(val i18n: String) {
    // Email is invalid
    INVALID_EMAIL("login.error.creation.mail"),
    INVALID_TOKEN("login.error.badtoken.text"),
    TOKEN_SENT("login.error.sendtoken.text"),
    DUPLICATE_LOGIN("login.error.uniquelogin.text"),
    DUPLICATE_EMAIL("login.error.uniqueemail.text"),
    SIGN_UP_ERROR("login.error.field.text"),
    REQUIRED_CREDENTIALS("login.error.required.text")
}