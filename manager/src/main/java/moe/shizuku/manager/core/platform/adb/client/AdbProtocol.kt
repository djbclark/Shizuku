package moe.shizuku.manager.core.platform.adb.client

const val A_SYNC: Int = 0x434e5953
const val A_CNXN: Int = 0x4e584e43
const val A_AUTH: Int = 0x48545541
const val A_OPEN: Int = 0x4e45504f
const val A_OKAY: Int = 0x59414b4f
const val A_CLSE: Int = 0x45534c43
const val A_WRTE: Int = 0x45545257
const val A_STLS: Int = 0x534C5453

const val A_VERSION: Int = 0x01000000
const val A_MAXDATA: Int = 4096

const val A_STLS_VERSION: Int = 0x01000000

const val ADB_AUTH_TOKEN: Int = 1
const val ADB_AUTH_SIGNATURE: Int = 2
const val ADB_AUTH_RSAPUBLICKEY: Int = 3
