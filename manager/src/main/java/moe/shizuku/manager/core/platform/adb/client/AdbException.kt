package moe.shizuku.manager.core.platform.adb.client

open class AdbException : Exception {
    constructor(cause: Throwable) : super(cause)
    constructor()
}

class AdbInvalidPairingCodeException : AdbException()

class AdbKeyException(cause: Throwable) : AdbException(cause)
