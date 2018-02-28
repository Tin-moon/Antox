package chat.tox.antox.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.view.MenuItem;
import android.widget.Toast;

import org.scaloid.common.LoggerTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.fragments.ColorPickerDialogJ;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.tox.ToxService;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.AntoxLocalization;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.Options;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class SettingsActivityJ extends BetterPreferenceActivityJ implements Preference.OnPreferenceClickListener {

    private ColorPickerDialogJ themeDialog;

    private Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();
            if ((preference instanceof ListPreference)) {
                ListPreference localListPreference = (ListPreference) preference;
                int index = localListPreference.findIndexOfValue(stringValue);
                preference.setSummary(index >= 0 ? localListPreference.getEntries()[index] : null);
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.title_activity_settings)); // fix locale changes
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ThemeManagerJ.applyTheme(this, getSupportActionBar());

        themeDialog = new ColorPickerDialogJ(this, new ColorPickerDialogJ.Callback() {
            @Override
            public void onColorSelection(int index, int color, int darker) {
                ThemeManagerJ.setPrimaryColor(color);
                ThemeManagerJ.setPrimaryColorDark(darker);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    // it's a shame this can't be
                    // used to recreate this activity and still change the theme
                    Intent i = new Intent(getApplicationContext(), MainActivityJ.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    finish();
                    startActivity(i);
                }
            }
        });

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("showing_theme_dialog", false)) {
                showThemeDialog();
            }
        }

        addPreferencesFromResource(R.xml.settings_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findPreference("theme_color").setOnPreferenceClickListener(this);
        findPreference("call_replies").setOnPreferenceClickListener(this);
        bindPreferenceSummaryToValue(findPreference("locale"));

        bindPreferenceSummaryToValue(findPreference("proxy_type"));
        bindPreferenceSummaryToValue(findPreference("proxy_address"));
        bindPreferenceSummaryToValue(findPreference("proxy_port"));

        bindPreferenceSummaryToValue(findPreference("custom_node_address"));
        bindPreferenceSummaryToValue(findPreference("custom_node_port"));
        bindPreferenceSummaryToValue(findPreference("custom_node_key"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        themeDialog.close();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // this is needed to keep the theme dialog open on rotation
        // the hack is required because PreferenceActivity doesn't allow for dialog fragments
        outState.putBoolean("showing_theme_dialog", themeDialog.isShowing());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean networkSettingsChanged = false;
        List<String> proxySettings = new ArrayList<String>(Arrays.asList("enable_proxy", "proxy_type", "proxy_address", "proxy_port"));

        if (key.equals("enable_udp")) {
            Options.udpEnabled_$eq(sharedPreferences.getBoolean("enable_udp", false));
            networkSettingsChanged = true;
        }

        if (proxySettings.contains(key)) {
            networkSettingsChanged = true;
        }

        if (sharedPreferences.getBoolean("autoacceptft", false)) {
            State.setAutoAcceptFt(true);
        } else {
            State.setAutoAcceptFt(false);
        }

        if (sharedPreferences.getBoolean("batterysavingmode", false)) {
            State.setBatterySavingMode(true);
        } else {
            State.setBatterySavingMode(false);
        }


        if (sharedPreferences.getBoolean("videocallstartwithnovideo", false)) {
            Options.videoCallStartWithNoVideo_$eq(true);
        } else {
            Options.videoCallStartWithNoVideo_$eq(false);
        }

        if (key.equals("proxy_address")) {
            String address = sharedPreferences.getString("proxy_address", "127.0.0.1");
            if (!address.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_ip_address), Toast.LENGTH_SHORT).show();
                EditTextPreference preference = (EditTextPreference) findPreference("proxy_address");
                preference.setText("127.0.0.1");
                preference.setSummary("127.0.0.1");
            } else {
                networkSettingsChanged = true;
            }
        }

        if (key.equals("proxy_port")) {
            int port = Integer.parseInt(sharedPreferences.getString("proxy_port", "9050"));
            if (!(port > 0 && port < 65535)) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_port), Toast.LENGTH_SHORT).show();
                EditTextPreference preference = (EditTextPreference) findPreference("proxy_port");
                preference.setText("9050");
                preference.setSummary("9050");
                networkSettingsChanged = false;
            }
            else{
                networkSettingsChanged = true;
            }
        }

        if (key.equals("custom_node_address")) {
            String address = sharedPreferences.getString("custom_node_address", "127.0.0.1");
            if (!address.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_ip_address), Toast.LENGTH_SHORT).show();
                EditTextPreference preference = (EditTextPreference) findPreference("custom_node_address");
                preference.setText("127.0.0.1");
                preference.setSummary("127.0.0.1");
            }
        }

        if (key.equals("custom_node_port")) {
            int port = Integer.parseInt(sharedPreferences.getString("custom_node_port", "33445"));
            if (!(port > 0 && port < 65535)) {
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_port), Toast.LENGTH_SHORT).show();
                EditTextPreference preference = (EditTextPreference) findPreference("custom_node_port");
                preference.setText("33445");
                preference.setSummary("33445");
            }
        }

        if (key.equals("custom_node_key")) {
            String address = sharedPreferences.getString("custom_node_key", "");
            if (address.length() != 64 || !address.matches("^[0-9A-F]+$")) {
                AntoxLog.error("Malformed tox public key", new LoggerTag("SettingsActivityJ"));
                Toast.makeText(getApplicationContext(), getString(R.string.error_invalid_tox_id), Toast.LENGTH_SHORT).show();
                EditTextPreference preference = (EditTextPreference) findPreference("custom_node_key");
                preference.setText("");
                preference.setSummary("");
            }
        }

        if (key.equals("wifi_only")) {
            if (!ToxSingleton.isToxConnected(sharedPreferences, this)) {
                AntoxDB antoxDb = State.db();
                antoxDb.setAllOffline();
            }
        }

        if (key.equals("locale")) {
            AntoxLog.debug("Locale changed", AntoxLog.DEFAULT_TAG());
            AntoxLocalization.setLanguage(getApplicationContext());
            Intent intent = new Intent(getApplicationContext(), MainActivityJ.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        if (key.equals("notifications_persistent")) {
            if (sharedPreferences.getBoolean("notifications_persistent", false) && sharedPreferences.getBoolean("notifications_enable_notifications", true)) {
                AntoxNotificationManager.createPersistentNotification(getApplicationContext());
            } else {
                AntoxNotificationManager.removePersistentNotification();
            }
        }

        if (key.equals("notifications_enable_notifications")) {
            if (sharedPreferences.getBoolean("notifications_persistent", false) && sharedPreferences.getBoolean("notifications_enable_notifications", true)) {
                AntoxNotificationManager.createPersistentNotification(getApplicationContext());
            } else {
                AntoxNotificationManager.removePersistentNotification();
            }
        }

        if (networkSettingsChanged) {
            AntoxLog.debug("One or more network settings changed. Restarting Tox service", AntoxLog.DEFAULT_TAG());
            Intent service = new Intent(this, ToxService.class);
            this.stopService(service);
            this.startService(service);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case "theme_color":
                showThemeDialog();
                break;
            case "call_replies":
                launchCallRepliesActivity();
                break;
            default: //do nothing
                break;
        }
        return true;
    }

    private void showThemeDialog() {
        int currentColor = ThemeManagerJ.primaryColor();
        themeDialog.show(currentColor);
    }

    private void launchCallRepliesActivity() {
        Intent intent = new Intent(this, EditCallRepliesActivityJ.class);
        startActivity(intent);
    }

}
