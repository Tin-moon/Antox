package chat.tox.antox.theme;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;

import chat.tox.antox.R;

/**
 * Created by Nechypurenko on 08.02.2018.
 */

public class ThemeManagerJ {

    private static int primaryColor;
    private static int primaryColorDark;

    private static SharedPreferences preferences;

    public static int primaryColor() {
        return primaryColor;
    }

    public static int primaryColorDark() {
        return primaryColorDark;
    }

    public static void init(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        primaryColor = preferences.getInt("theme_color", context.getResources().getColor(R.color.brand_primary));
        primaryColorDark = darkenColor(primaryColor);
    }

    public static int darkenColor(int color) {
        float hsv[] = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.9f;
        return Color.HSVToColor(hsv);
    }

    public static void applyTheme(Activity activity, ActionBar actionBar) {
        if(actionBar == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //ActivityManager.RunningTaskInfo info = new ActivityManager.RunningTaskInfo();
            actionBar.setBackgroundDrawable(new ColorDrawable(ThemeManagerJ.primaryColor));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.setTaskDescription(new ActivityManager.TaskDescription(activity.getString(R.string.app_name), BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_launcher), primaryColor));
            activity.getWindow().setStatusBarColor(ThemeManagerJ.primaryColorDark);
        }
    }

    public static void setPrimaryColor(int color) {
        primaryColor = color;
        preferences.edit().putInt("theme_color", primaryColor).apply();
    }

    public static void setPrimaryColorDark(int color) {
        primaryColorDark = color;
    }
}
