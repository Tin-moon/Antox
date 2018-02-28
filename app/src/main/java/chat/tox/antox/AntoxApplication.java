package chat.tox.antox;

import android.app.Application;

import chat.tox.antox.theme.ThemeManager;
import chat.tox.antox.theme.ThemeManagerJ;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class AntoxApplication extends Application {


    public AntoxApplication() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        ThemeManagerJ.init(getApplicationContext());
        ThemeManager.init(getApplicationContext()); //TODO remove
    }
}
