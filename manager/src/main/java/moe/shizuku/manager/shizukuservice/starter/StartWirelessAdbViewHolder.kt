package moe.shizuku.manager.shizukuservice.starter

// package moe.shizuku.manager.home

// import android.Manifest.permission.WRITE_SECURE_SETTINGS
// import android.content.Context
// import android.content.Intent
// import android.content.pm.PackageManager
// import android.os.Build
// import android.provider.Settings
// import android.text.method.LinkMovementMethod
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import androidx.annotation.RequiresApi
// import androidx.core.view.isVisible
// import androidx.fragment.app.FragmentActivity
// import androidx.work.WorkManager
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.CoroutineScope
// import kotlinx.coroutines.Dispatchers
// import moe.shizuku.manager.ShizukuSettings
// import moe.shizuku.manager.R
// import moe.shizuku.manager.adb.AdbStarter
// import moe.shizuku.manager.databinding.HomeItemContainerBinding
// import moe.shizuku.manager.databinding.HomeStartWirelessAdbBinding
// import moe.shizuku.manager.service.ui.showAccessibilityDialog
// import moe.shizuku.manager.receiver.NotifCancelReceiver
// import moe.shizuku.manager.service.ui.StartFragment
// import moe.shizuku.manager.utils.EnvironmentUtils
// import moe.shizuku.manager.utils.ShizukuStateMachine
// import rikka.core.content.asActivity
// import rikka.recyclerview.BaseViewHolder
// import rikka.recyclerview.BaseViewHolder.Creator

//         fun start (context: Context, scope: CoroutineScope) {
//             context.sendBroadcast(Intent(context, NotifCancelReceiver::class.java))

//             val cr = context.contentResolver
//             if (context.checkSelfPermission(WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
//                 Settings.Global.putInt(cr, Settings.Global.ADB_ENABLED, 1)
//                 Settings.Global.putLong(cr, "adb_allowed_connection_time", 0L)
//             }

//             val adbEnabled = Settings.Global.getInt(cr, Settings.Global.ADB_ENABLED, 0)
//             if (adbEnabled == 0) {
//                 WadbEnableUsbDebuggingDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//                 return
//             }

//             val tcpPort = EnvironmentUtils.getAdbTcpPort()
//             val tcpMode = ShizukuSettings.getTcpMode()

//             // If ADB is NOT listening to a TCP port and the device doesn't support TLS, inform the user
//             if (tcpPort <= 0 && !EnvironmentUtils.isTlsSupported()) {
//                 WadbNotEnabledDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // If ADB IS NOT listening to a TCP port but the device supports TLS, start mDns discovery
//             } else if (tcpPort <= 0) {
//                 AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // If ADB IS listening to a TCP port but the user wants to close it and use TLS instead, close the TCP port and start mDns discovery
//             } else if (!tcpMode) {
//                 scope.launch {
//                     AdbStarter.stopTcp(context, tcpPort)
//                 }
//                 AdbDialogFragment().show(context.asActivity<FragmentActivity>().supportFragmentManager)
//             // Otherwise ADB IS listening to a TCP port and the user wants to keep it open. Start Shizuku via TCP
//             } else {
//                 val intent = Intent(context, StartFragment::class.java).apply {
//                     putExtra(StartFragment.EXTRA_PORT, tcpPort)
//                 }
//                 context.startActivity(intent)
//             }
//         }

//fun showWadbEnableUsbDebuggingDialog(context: Context) {
//    MaterialAlertDialogBuilder(context)
//        .setMessage(R.string.start_error_usb_debugging_disabled)
//        .setPositiveButton(R.string.developer_options) { _, _ ->
//            SettingsPage.Developer.HighlightUsbDebugging.launch(context)
//        }
//        .setNegativeButton(android.R.string.cancel, null)
//        .show()
//}
//
//fun showWadbNotEnabledDialog(context: Context) {
//    MaterialAlertDialogBuilder(context)
//        .setMessage(R.string.start_error_wireless_debugging_disabled)
//        .setPositiveButton(android.R.string.ok, null)
//        .show()
//}

//     }

//     init {
//         binding.button1.setOnClickListener { v: View ->
//             start(v.context, scope)
//         }

//         if (EnvironmentUtils.isTlsSupported()) {
//             binding.button2.setOnClickListener { v: View ->
//                 onPairClicked(v.context)
//             }
//             binding.text1.movementMethod = LinkMovementMethod.getInstance()
//             binding.text1.text = context.getString(R.string.home_wireless_adb_description)
//         } else {
//             binding.text1.text = context.getString(R.string.home_wireless_adb_description_pre_11)
//             binding.button2.isVisible = false
//             binding.button3.isVisible = false
//         }
//     }


// }
