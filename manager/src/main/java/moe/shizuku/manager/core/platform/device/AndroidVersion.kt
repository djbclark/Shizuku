package moe.shizuku.manager.core.platform.device

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

@Suppress("unused")
object AndroidVersion {
    private val sdkVersion = Build.VERSION.SDK_INT

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.BAKLAVA)
    val isAtLeast16: Boolean = sdkVersion >= Build.VERSION_CODES.BAKLAVA

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    val isAtLeast15: Boolean = sdkVersion >= Build.VERSION_CODES.VANILLA_ICE_CREAM

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val isAtLeast14: Boolean = sdkVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    val isAtLeast13: Boolean = sdkVersion >= Build.VERSION_CODES.TIRAMISU

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isAtLeast12: Boolean = sdkVersion >= Build.VERSION_CODES.S

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.R)
    val isAtLeast11: Boolean = sdkVersion >= Build.VERSION_CODES.R

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    val isAtLeast10: Boolean = sdkVersion >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.P)
    val isAtLeast9: Boolean = sdkVersion >= Build.VERSION_CODES.P

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    val isAtLeast8: Boolean = sdkVersion >= Build.VERSION_CODES.O
}