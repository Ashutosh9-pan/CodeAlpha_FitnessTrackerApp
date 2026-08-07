package utils;

import android.content.Context;
import android.content.SharedPreferences;
import utils.ThemeManager;
import androidx.appcompat.app.AppCompatDelegate;

public class ThemeManager {

    private static final String PREF_NAME = "vitafit_settings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ThemeManager() {
    }

    public static void applySavedTheme(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        boolean darkMode =
                preferences.getBoolean(
                        KEY_DARK_MODE,
                        false
                );

        AppCompatDelegate.setDefaultNightMode(
                darkMode
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDarkModeEnabled(Context context) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        return preferences.getBoolean(
                KEY_DARK_MODE,
                false
        );
    }

    public static void setDarkMode(
            Context context,
            boolean enabled
    ) {

        SharedPreferences preferences =
                context.getSharedPreferences(
                        PREF_NAME,
                        Context.MODE_PRIVATE
                );

        preferences.edit()
                .putBoolean(KEY_DARK_MODE, enabled)
                .apply();

        AppCompatDelegate.setDefaultNightMode(
                enabled
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
}