package chat.tox.antox.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.ClipboardManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import chat.tox.QR.Contents;
import chat.tox.QR.QRCodeEncode;
import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.data.UserDB;
import chat.tox.antox.fragments.AvatarDialogJ;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.wrapper.UserStatus;
import im.tox.tox4j.core.data.ToxNickname;
import im.tox.tox4j.core.data.ToxStatusMessage;
import im.tox.tox4j.core.enums.ToxUserStatus;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class ProfileSettingsActivityJ extends BetterPreferenceActivityJ {

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

    private AvatarDialogJ avatarDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getDelegate().installViewFactory();
        getDelegate().onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);

        setTitle(getString(R.string.title_activity_profile_settings));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ThemeManagerJ.applyTheme(this, getSupportActionBar());

        addPreferencesFromResource(R.xml.pref_profile);

        avatarDialog = new AvatarDialogJ(ProfileSettingsActivityJ.this);

        if (savedInstanceState != null) {
            if (savedInstanceState.getBoolean("showing_avatar_dialog", false)) {
                avatarDialog.show();
            }
        }

        bindPreferenceSummaryToValue(findPreference("nickname"));
        bindPreferenceSummaryToValue(findPreference("status"));
        bindPreferenceSummaryToValue(findPreference("status_message"));
        bindPreferenceSummaryToValue(findPreference("tox_id"));
        bindPreferenceSummaryToValue(findPreference("active_account"));

        Preference passwordPreference = findPreference("password");

        passwordPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                createPasswordDialog();
                return true;
            }
        });
        bindPreferenceIfExists(passwordPreference);

        Preference toxMePreference = findPreference("toxme_info");
        toxMePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                createToxMeAddressDialog();
                return true;
            }
        });
        bindPreferenceIfExists(toxMePreference);


        Preference toxIDPreference = findPreference("tox_id");
        toxIDPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                createToxIDDialog();
                return true;
            }
        });

        Preference avatarPreference = findPreference("avatar");
        avatarPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                avatarDialog.show();
                return true;
            }
        });


        Preference exportProfile = findPreference("export");
        exportProfile.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                File path = Environment.getExternalStorageDirectory();

                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.DIR_SELECT;
                properties.root = path;
                properties.error_dir = path;
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(ProfileSettingsActivityJ.this, properties);
                dialog.setTitle(R.string.select_file);

                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        //files is the array of the paths of files selected by the Application User.
                        // since we only want single file selection, use the first entry
                        if (files != null) {
                            if (files.length > 0) {
                                if (files[0] != null) {
                                    if (files[0].length() > 0) {
                                        File directory = new File(files[0]);
                                        onExportDataFileSelected(directory);
                                    }
                                }
                            }
                            else {
                                onExportDataFileSelected(path);
                            }
                        }
                    }
                });
                dialog.show();
                return true;
            }
        });

        Preference deleteAccount = findPreference("delete");
        deleteAccount.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsActivityJ.this);
                builder.setMessage(R.string.delete_account_dialog_message);
                builder.setTitle(R.string.delete_account_dialog_title);
                builder.setPositiveButton(R.string.delete_account_dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        State.deleteActiveAccount(ProfileSettingsActivityJ.this);
                    }
                });
                builder.setNegativeButton(getString(R.string.delete_account_dialog_cancel), null);
                builder.show();
                return true;
            }
        });

        Preference nospamPreference = findPreference("nospam");
        nospamPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsActivityJ.this);
                builder.setMessage(R.string.reset_tox_id_dialog_message);
                builder.setTitle(R.string.reset_tox_id_dialog_title);
                builder.setPositiveButton(getString(R.string.reset_tox_id_dialog_confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Random random = new Random();
                            int maxNospam = 1234567890;
                            int nospam = random.nextInt(maxNospam);
                            ToxSingleton.tox().setNospam(nospam);
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ProfileSettingsActivityJ.this);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("tox_id", ToxSingleton.tox().getAddress().toString());
                            editor.apply();

                            // Display toast to inform user of successful change
                            Toast.makeText(
                                    getApplicationContext(),
                                    getString(R.string.tox_id_reset),
                                    Toast.LENGTH_SHORT
                            ).show();

                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                builder.setNegativeButton(getString(R.string.button_cancel), null);
                builder.show();
                return true;
            }
        });
    }

    private void bindPreferenceIfExists(Preference preference) {
        if (PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), "").isEmpty()) {
            getPreferenceScreen().removePreference(preference);
        } else {
            bindPreferenceSummaryToValue(preference);
        }
    }

    private void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
    }

    private void createToxIDDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_tox_id, null);
        builder.setView(view);
        builder.setPositiveButton(getString(R.string.button_ok), null);
        builder.setNeutralButton(getString(R.string.dialog_tox_id), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                android.text.ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(sharedPreferences.getString("tox_id", ""));
            }
        });

        final String path = Environment.getExternalStorageDirectory().getPath();
        File dir = new File(path + "/Antox/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File noMedia = new File(path + "/Antox/", ".nomedia");
        if (!noMedia.exists()) {
            try {
                noMedia.createNewFile();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        File file = new File(path + "/Antox/userkey_qr.png");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        generateQR(pref.getString("tox_id", ""));
        Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
        ImageButton qrCode = (ImageButton) view.findViewById(R.id.qr_image);
        qrCode.setImageBitmap(bmp);
        qrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(path + "/Antox/userkey_qr.png")));
                shareIntent.setType("image/jpeg");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_with)));
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void createCopyToClipboardDialog(String prefKey, String dialogPositiveString, String dialogNeutralString) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        builder.setTitle(pref.getString(prefKey, ""));
        builder.setPositiveButton(dialogPositiveString, null);
        builder.setNeutralButton(dialogNeutralString, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                android.text.ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(sharedPreferences.getString(prefKey, ""));
            }
        });
        builder.create().show();
    }

    private void createToxMeAddressDialog() {
        createCopyToClipboardDialog("toxme_info", getString(R.string.button_ok), getString(R.string.dialog_toxme));
    }

    private void createPasswordDialog() {
        createCopyToClipboardDialog("password", getString(R.string.button_ok), getString(R.string.dialog_password));
    }

    private void onExportDataFileSelected(File dest) {
        try {
            ToxSingleton.exportDataFile(dest);
            Toast.makeText(getApplicationContext(), getString(R.string.export_success, dest.getPath()), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), R.string.export_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void generateQR(String userKey) {
        String qrData = "tox:" + userKey;
        int qrCodeSize = 400;
        QRCodeEncode qrCodeEncoder = new QRCodeEncode(qrData, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeSize);
        FileOutputStream out = null;

        try {
            Bitmap bitmap = qrCodeEncoder.encodeAsBitmap();
            out = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Antox/userkey_qr.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
        } catch (WriterException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        avatarDialog.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        avatarDialog.onActivityResult(requestCode, resultCode, data);
        avatarDialog.close();
        avatarDialog.show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("showing_avatar_dialog", avatarDialog.isShowing());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        UserDB userDb = State.userDb(this);
        switch (key) {
            case "nickname":
                String name = sharedPreferences.getString(key, "");
                try {
                    ToxSingleton.tox().setName(ToxNickname.unsafeFromValue(name.getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                userDb.updateActiveUserDetail(key, name);
                break;
            case "password":
                String password = sharedPreferences.getString(key, "");
                userDb.updateActiveUserDetail(key, password);
                break;
            case "status":
                String newStatusString = sharedPreferences.getString(key, "");
                ToxUserStatus newStatus = UserStatus.getToxUserStatusFromString(newStatusString);
                try {
                    ToxSingleton.tox().setStatus(newStatus);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                userDb.updateActiveUserDetail(key, newStatusString);
                break;
            case "status_message":
                String statusMessage = sharedPreferences.getString(key, "");
                try {
                    ToxSingleton.tox().setStatusMessage(ToxStatusMessage.unsafeFromValue(sharedPreferences.getString(statusMessage, "").getBytes()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                userDb.updateActiveUserDetail(key, statusMessage);
                break;
            case "logging_enabled":
                Boolean loggingEnabled = sharedPreferences.getBoolean(key, true);
                userDb.updateActiveUserDetail(key, loggingEnabled);
                break;
            case "avatar":
                String avatar = sharedPreferences.getString(key, "");
                userDb.updateActiveUserDetail(key, avatar);
                break;
            default:
                break;
        }
    }

}
