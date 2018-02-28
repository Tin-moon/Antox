package chat.tox.antox.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import chat.tox.antox.R;
import chat.tox.antox.activities.SettingsActivityJ;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.ConnectionManager;
import chat.tox.antox.utils.ConnectionTypeChangeListener;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class WifiWarningFragmentJ extends Fragment {

    private Button wifiWarningBar;
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesListener;
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wifi_warning, container, false);
        wifiWarningBar = (Button) rootView.findViewById(R.id.wifi_only_warning);
        wifiWarningBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickWifiOnlyWarning(v);
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        updateWifiWarning();

        ConnectionManager.addConnectionTypeChangeListener(new ConnectionTypeChangeListener() {
            @Override
            public void connectionTypeChange(int connectionType) {
                updateWifiWarning();
            }
        });

        preferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                switch (key) {
                    case "wifi_only":
                        updateWifiWarning();
                        break;
                    default:
                        break;
                }
            }
        };

        preferences.registerOnSharedPreferenceChangeListener(preferencesListener);
    }

    private void updateWifiWarning() {
        if (getActivity() != null) {
            if (!ToxSingleton.isToxConnected(preferences, getActivity())) {
                showWifiWarning();
            } else {
                hideWifiWarning();
            }
        }
    }

    public void onClickWifiOnlyWarning(View view) {
        Intent intent = new Intent(getActivity(), SettingsActivityJ.class);
        startActivity(intent);
    }

    public void  showWifiWarning() {
        getView().setVisibility(View.VISIBLE);
    }

    public void  hideWifiWarning() {
        getView().setVisibility(View.GONE);
    }

}
