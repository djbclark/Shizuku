package moe.shizuku.manager.core.ui;

import android.content.Context;
import android.os.Build;
import android.util.TypedValue;

import androidx.annotation.StyleRes;

import com.google.android.material.snackbar.Snackbar;

import java.util.Objects;

import moe.shizuku.manager.R;
import moe.shizuku.manager.ShizukuSettings;
import moe.shizuku.manager.core.utils.EnvironmentUtils;
import rikka.core.util.ResourceUtils;

public class ThemeHelper {

    private static final String THEME_DEFAULT = "DEFAULT";
    private static final String THEME_BLACK = "BLACK";

    public static boolean isBlackNightTheme(Context context) {
        return ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.Keys.KEY_AMOLED_BLACK, EnvironmentUtils.isWatch());
    }

    public static boolean isUsingSystemColor() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && ShizukuSettings.getPreferences().getBoolean(ShizukuSettings.Keys.KEY_DYNAMIC_COLOR, true);
    }

    public static String getTheme(Context context) {
        if (isBlackNightTheme(context)
                && ResourceUtils.isNightMode(context.getResources().getConfiguration()))
            return THEME_BLACK;

        return THEME_DEFAULT;
    }

    @StyleRes
    public static int getThemeStyleRes(Context context) {
        if (Objects.equals(getTheme(context), THEME_BLACK)) {
            return R.style.ThemeOverlay_Black;
        } else {
            return R.style.ThemeOverlay;
        }
    }

    public static void applySnackbarTheme(Context context, Snackbar snackbar) {
        snackbar.setBackgroundTint(resolveColor(context, R.attr.colorPrimaryContainer))
                .setTextColor(resolveColor(context, R.attr.colorOnSurface))
                .setActionTextColor(resolveColor(context, R.attr.colorPrimary));
    }

    private static int resolveColor(Context context, int color) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(color, typedValue, true);
        return typedValue.data;
    }
}
