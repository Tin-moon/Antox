package chat.tox.antox.activities;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.fragments.MainDrawerFragmentJ;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.utils.AntoxLocalization;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.Options;
import scala.Option;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class MainActivityJ extends AppCompatActivity {

    private View request;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set the right language
        AntoxLocalization.setLanguage(getApplicationContext());

        setContentView(R.layout.activity_main);

        // Use a toolbar so that the drawer goes above the action bar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setTitle(getString(R.string.app_name));

        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setHomeAsUpIndicator(R.drawable.ic_menu);
            bar.setDisplayHomeAsUpEnabled(true);
            //ThemeManagerJ.applyTheme(this, getSupportActionBar());
        }

        // Fix for Android 4.1.x
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        // Check to see if Internet is potentially available and show a warning if it isn't
        if (!isNetworkConnected()) {
            showAlertDialog(this, getString(R.string.main_no_internet), getString(R.string.main_not_connected));
        }

        // Give ToxSingleton an instance of notification manager for use in displaying notifications from callbacks
        AntoxNotificationManager.mNotificationManager_$eq(Option.apply(((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))));

        if (preferences.getBoolean("notifications_persistent", false)) {
            AntoxNotificationManager.createPersistentNotification(getApplicationContext());
        }

        // Initialise the bitmap manager for storing bitmaps in a cache
        new BitmapManager();

        // Removes the drop shadow from the actionbar as it overlaps the tabs
        getSupportActionBar().setElevation(0);

        // set autoaccept option on startup
        State.setAutoAcceptFt(preferences.getBoolean("autoacceptft", false));

        Options.videoCallStartWithNoVideo_$eq(preferences.getBoolean("videocallstartwithnovideo", false));
        State.setBatterySavingMode(preferences.getBoolean("batterysavingmode", false));

    }

    @Override
    protected void onResume() {
        super.onResume();
        ThemeManagerJ.applyTheme(this, getSupportActionBar());
    }

    public void onClickAdd(View v) {
        Intent intent = new Intent(this, AddActivityJ.class);
        startActivityForResult(intent, ConstantsJ.ADD_FRIEND_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        MainDrawerFragmentJ drawerFragment = (MainDrawerFragmentJ) getSupportFragmentManager().findFragmentById(R.id.drawer);
        if (drawerFragment.isDrawerOpen()) {
            drawerFragment.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Displays a generic dialog using the strings passed in.
     * TODO: Should maybe be refactored into separate class and used for other dialogs?
     */
    private void showAlertDialog(Context context, String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        alertDialog.show();
    }

    /**
     * Checks to see if Wifi or Mobile have a network connection
     */
    private boolean isNetworkConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
        for (NetworkInfo info : networkInfo) {
            if ("WIFI".equalsIgnoreCase(info.getTypeName()) && info.isConnected() || "MOBILE".equalsIgnoreCase(info.getTypeName()) && info.isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            MainDrawerFragmentJ drawer = (MainDrawerFragmentJ) getSupportFragmentManager().findFragmentById(R.id.drawer);
            drawer.openDrawer();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
