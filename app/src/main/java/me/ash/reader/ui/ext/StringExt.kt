package me.ash.reader.ui.ext

import android.text.Html
import android.util.Base64
import java.math.BigInteger
import java.security.MessageDigest

object MimeType {

    const val ANY = "*/*"
    const val FONT = "font/ttf"
    const val OPML = "text/x-opml"  // Not supported yet
    const val JSON = "application/json"
}

fun String.formatUrl(): String {
    if (this.startsWith("//")) {
        return "https:$this"
    }
    val regex = Regex("^(https?|ftp|file).*")
    return if (!regex.matches(this)) {
        "https://$this"
    } else {
        this
    }
}

fun String.isUrl(): Boolean {
    val regex = Regex("(https?|ftp|file)://[-A-Za-z0-9+&@#/%?=~_|!:,.;]+[-A-Za-z0-9+&@#/%=~_|]")
    return regex.matches(this)
}

fun String.mask(): String = run {
    "\u2022".repeat(length)
}

fun String.encodeBase64(): String = Base64.encodeToString(toByteArray(), Base64.DEFAULT)

fun String.decodeBase64(): String = String(Base64.decode(this, Base64.DEFAULT))

fun String.md5(): String =
    BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray()))
        .toString(16).padStart(32, '0')

fun String?.decodeHTML(): String? = this?.run { Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString() }

fun String?.orNotEmpty(l: (value: String) -> String): String =
    if (this.isNullOrBlank()) "" else l(this)


/**
 * Whether this text reads right-to-left, decided by which script most of its letters are in and
 * falling back to the first strong letter on a tie. Digits, punctuation and spaces don't count.
 *
 * Not [java.text.Bidi.requiresBidi], which is true for *any* RTL character and so flipped an English
 * headline that quoted one Hebrew word; and not first-strong alone, which calls a Hebrew headline
 * opening with a Latin brand ("CISO יקר…", "Galaxy S26 …") left-to-right.
 */
fun String.isRtl(): Boolean {
    var rtl = 0
    var ltr = 0
    var firstStrongIsRtl: Boolean? = null
    codePoints().forEach {
        val isRtl =
            when (Character.getDirectionality(it)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT,
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC -> true
                Character.DIRECTIONALITY_LEFT_TO_RIGHT -> false
                else -> return@forEach
            }
        if (firstStrongIsRtl == null) firstStrongIsRtl = isRtl
        if (isRtl) rtl++ else ltr++
    }
    return if (rtl != ltr) rtl > ltr else firstStrongIsRtl ?: false
}

fun String?.extractDomain(): String? {
    if (this.isNullOrBlank()) return null
    val urlMatchResult = Regex("(?<=://)([\\w\\d.-]+)").find(this)
    if (urlMatchResult != null) {
        return urlMatchResult.value
    }
    val domainRegex = Regex("[\\w\\d.-]+\\.[\\w\\d.-]+")
    val domainMatchResult = domainRegex.find(this)
    return domainMatchResult?.value
}
