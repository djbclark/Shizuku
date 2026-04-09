package moe.shizuku.manager.intents.data

import java.security.SecureRandom

private const val DEFAULT_TOKEN_LENGTH = 24
private val CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')
private val secureRandom = SecureRandom()

fun generateToken(length: Int = DEFAULT_TOKEN_LENGTH): String =
    buildString(length) {
        repeat(length) {
            append(CHARS[secureRandom.nextInt(CHARS.size)])
        }
    }
