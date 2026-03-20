package moe.shizuku.manager.home.models

import androidx.annotation.StringRes
import moe.shizuku.manager.R

enum class HelpItem(@param:StringRes val labelRes: Int) {
    USER_GUIDE(R.string.help_user_guide),
    BUG_REPORT(R.string.help_bug_report),
    FEATURE_REQUEST(R.string.help_feature_request),
    TRANSLATE(R.string.help_translate),
    PRIVACY(R.string.help_privacy)
}