package moe.shizuku.manager.tcpmode.models

data class TcpState(
    val current: Port,
    val target: Port
) {
    sealed class Port {
        object Disabled : Port()
        data class Enabled(val port: Int) : Port()
    }

    val isSynced: Boolean = current == target
}
