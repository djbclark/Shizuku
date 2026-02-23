package moe.shizuku.manager.shizukuservice.starter

// import android.content.Intent
// import android.view.View
// import com.google.android.material.dialog.MaterialAlertDialogBuilder
// import moe.shizuku.manager.R
// import moe.shizuku.manager.core.extensions.*
// import moe.shizuku.manager.starter.Starter

// binding.button1.setOnClickListener { v: View ->
//     val context = v.context
//     MaterialAlertDialogBuilder(context)
//         .setTitle(R.string.view_command)
//         .setMessage(
//                 context.getString(
//                     R.string.home_adb_dialog_view_command_message,
//                     Starter.adbCommand
//                 )
//         )
//         .setPositiveButton(android.R.string.copy) { _, _ ->
//             context.copyToClipboard(Starter.adbCommand)
//         }
//         .setNegativeButton(android.R.string.cancel, null)
//         .setNeutralButton(R.string.share) { _, _ ->
//             var intent = Intent(Intent.ACTION_SEND)
//             intent.type = "text/plain"
//             intent.putExtra(Intent.EXTRA_TEXT, Starter.adbCommand)
//             intent = Intent.createChooser(
//                 intent,
//                 context.getString(R.string.share)
//             )
//             context.startActivity(intent)
//         }
//         .show()
// }
