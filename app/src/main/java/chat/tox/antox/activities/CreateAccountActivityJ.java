package chat.tox.antox.activities;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.data.UserDB;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.tox.ToxDataFile;
import chat.tox.antox.tox.ToxService;
import chat.tox.antox.toxme.ToxData;
import chat.tox.antox.toxme.ToxMe;
import chat.tox.antox.toxme.ToxMeError;
import chat.tox.antox.toxme.ToxMeName;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.ConnectionManager;
import chat.tox.antox.utils.DatabaseConstants;
import chat.tox.antox.utils.FileUtils;
import chat.tox.antox.utils.Options;
import chat.tox.antox.utils.ProxyUtils;
import chat.tox.antox.wrapper.ToxAddress;
import im.tox.tox4j.core.ToxCoreConstants;
import im.tox.tox4j.core.exceptions.ToxNewException;
import im.tox.tox4j.core.options.ProxyOptions;
import im.tox.tox4j.core.options.SaveDataOptions;
import im.tox.tox4j.core.options.ToxOptions;
import im.tox.tox4j.impl.jni.ToxCoreImpl;
import rx.Observable;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import scala.Enumeration;
import scala.MatchError;
import scala.Option;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;

/**
 * Created by Nechypurenko on 12.02.2018.
 */

public class CreateAccountActivityJ extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.hide();
        }
        ThemeManagerJ.applyTheme(this, bar);
        setContentView(R.layout.activity_create_account);

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getColor(R.color.material_blue_grey_950));
        }

        CheckBox toxmeCheckBox = (CheckBox) findViewById(R.id.toxme);
        TextView toxmeText = (TextView) findViewById(R.id.toxme_text);
        toxmeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toxmeCheckBox.toggle();
                toggleRegisterText();
            }
        });

        toxmeCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRegisterText();
            }
        });


        ImageView toxmeHelpButton = (ImageView) findViewById(R.id.toxme_help_button);
        toxmeHelpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccountActivityJ.this, ToxMeInfoActivityJ.class);
                startActivity(intent);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.create_account, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    private void toggleRegisterText() {
        Button registerButton = (Button) findViewById(R.id.create_account);
        CheckBox toxmeCheckBox = (CheckBox) findViewById(R.id.toxme);
        ObjectAnimator fadeOut = ObjectAnimator.ofInt(registerButton, "textColor", getColor(R.color.white), Color.TRANSPARENT);
        fadeOut.setDuration(300);
        fadeOut.setEvaluator(new ArgbEvaluator());
        fadeOut.start();
        if (toxmeCheckBox.isChecked()) {
            registerButton.setText(R.string.create_register_with_toxme);
        } else {
            registerButton.setText(R.string.create_register);
        }
        ObjectAnimator fadeIn = ObjectAnimator.ofInt(registerButton, "textColor", Color.TRANSPARENT, getColor(R.color.white));
        fadeIn.setDuration(300);
        fadeIn.setEvaluator(new ArgbEvaluator());
        fadeIn.start();
    }

    private boolean validAccountName(String account) {
        Pattern pattern = Pattern.compile("\\s");
        Pattern pattern2 = Pattern.compile(File.separator);
        Matcher matcher = pattern.matcher(account);
        boolean containsSpaces = matcher.find();
        matcher = pattern2.matcher(account);
        boolean containsFileSeparator = matcher.find();
        return !(account.equals("") || containsSpaces || containsFileSeparator);
    }

    private void showBadAccountNameError() {
        Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.create_bad_profile_name), Toast.LENGTH_SHORT);
        toast.show();
    }

    private void loginAndStartMain(String accountName, String password) {
        UserDB userDb = State.userDb(this);
        State.login(accountName, this);
        userDb.updateActiveUserDetail(DatabaseConstants.COLUMN_NAME_PASSWORD(), password);

        // Start the activity
        Intent startTox = new Intent(getApplicationContext(), ToxService.class);
        getApplicationContext().startService(startTox);
        Intent main = new Intent(getApplicationContext(), MainActivityJ.class);
        startActivity(main);
        setResult(Activity.RESULT_OK);
        finish();
    }

    private ToxData createToxData(String accountName) {

//        boolean x$1 = Options..MODULE$.ipv6Enabled();
//        boolean x$2 = Options..MODULE$.udpEnabled();
//        SaveDataOptions.ToxSave x$3 = new SaveDataOptions.ToxSave(toxDataFile.loadFile());
//        boolean x$4 = ToxOptions..MODULE$.$lessinit$greater$default$3();
//        ProxyOptions x$5 = ToxOptions..MODULE$.$lessinit$greater$default$4();
//        int x$6 = ToxOptions..MODULE$.$lessinit$greater$default$5();
//        int x$7 = ToxOptions..MODULE$.$lessinit$greater$default$6();
//        int x$8 = ToxOptions..MODULE$.$lessinit$greater$default$7();
//        boolean x$9 = ToxOptions..MODULE$.$lessinit$greater$default$9();
//        ToxOptions toxOptions = new ToxOptions(x$1, x$2, x$4, x$5, x$6, x$7, x$8, x$3, x$9);

        final boolean localDiscoveryEnabled = ToxOptions.$lessinit$greater$default$3();
        final ProxyOptions proxy = ToxOptions.$lessinit$greater$default$4();
        final int startPort = ToxCoreConstants.DefaultStartPort();
        final int endPort = ToxCoreConstants.DefaultEndPort();
        final int tcpPort = ToxCoreConstants.DefaultTcpPort();
        final SaveDataOptions saveData = ToxOptions.$lessinit$greater$default$8();
        final boolean fatalErrors = ToxOptions.$lessinit$greater$default$9();

        ToxData toxData = new ToxData();
        ToxOptions toxOptions = new ToxOptions(Options.ipv6Enabled(), Options.udpEnabled(), localDiscoveryEnabled, proxy, startPort, endPort, tcpPort, saveData, fatalErrors);
        ToxCoreImpl tox = new ToxCoreImpl(toxOptions);
        ToxDataFile toxDataFile = new ToxDataFile(this, accountName);
        toxDataFile.saveFile(tox.getSavedata());
        toxData.address_$eq(new ToxAddress(tox.getAddress()));
        toxData.fileBytes_$eq(toxDataFile.loadFile());
        return toxData;
    }

    private Option<ToxData> loadToxData(String fileName) {
        ToxData toxData = new ToxData();
        ToxDataFile toxDataFile = new ToxDataFile(this, fileName);

        ToxOptions toxOptions = new ToxOptions(
                Options.ipv6Enabled(),
                Options.udpEnabled(),
                ToxOptions.$lessinit$greater$default$3(),
                ToxOptions.$lessinit$greater$default$4(),
                ToxCoreConstants.DefaultStartPort(),
                ToxCoreConstants.DefaultEndPort(),
                ToxCoreConstants.DefaultTcpPort(),
                new SaveDataOptions.ToxSave(toxDataFile.loadFile()),
                ToxOptions.$lessinit$greater$default$9());

        try {
            ToxCoreImpl tox = new ToxCoreImpl(toxOptions);
            toxData.address_$eq(new ToxAddress(tox.getAddress()));
            toxData.fileBytes_$eq(toxDataFile.loadFile());
            return Option.apply(toxData);
        }catch (Exception e) {
            if(e instanceof ToxNewException) {
                if(((ToxNewException) e).code().equals(ToxNewException.Code.LOAD_ENCRYPTED)) {
                    Toast.makeText(this, R.string.create_account_encrypted_profile_error, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.create_account_load_profile_unknown, Toast.LENGTH_SHORT).show();
                }
            }
        }
        return Option.empty();
    }

    private void disableRegisterButton() {
        Button importProfileButton = (Button) findViewById(R.id.create_account_import);
        importProfileButton.setEnabled(false);

        Button registerButton = (Button) findViewById(R.id.create_account);
        registerButton.setText(getText(R.string.create_registering));
        registerButton.setEnabled(false);

        //only animate on 2.3+ because animation was added in 3.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            int colorFrom = getColor(R.color.brand_secondary);
            int colorTo = getColor(R.color.brand_secondary_darker);
            ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
            colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    registerButton.setBackgroundColor((Integer) animation.getAnimatedValue());
                }
            });
            colorAnimation.start();
        }

        ProgressBar progressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void enableRegisterButton() {
        Button importProfileButton = (Button) findViewById(R.id.create_account_import);
        importProfileButton.setEnabled(true);

        Button registerButton = (Button) findViewById(R.id.create_account);
        registerButton.setEnabled(true);
        CheckBox toxmeCheckBox = (CheckBox) findViewById(R.id.toxme);
        if (toxmeCheckBox.isChecked()) {
            registerButton.setText(R.string.create_register_with_toxme);
        } else {
            registerButton.setText(R.string.create_register);
        }
        registerButton.setBackgroundColor(getColor(R.color.brand_secondary));
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.login_progress_bar);
        progressBar.setVisibility(View.GONE);
    }

    private void createAccount(String rawAccountName, UserDB userDb, boolean shouldCreateDataFile, boolean shouldRegister) {
        ToxMeName toxMeName = ToxMeName.fromString(rawAccountName, shouldRegister);
        if (!validAccountName(toxMeName.username())) {
            showBadAccountNameError();
        } else if (userDb.doesUserExist(toxMeName.username())) {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.create_profile_exists), Toast.LENGTH_LONG);
            toast.show();
        } else {
            disableRegisterButton();

            ToxData toxData = null;
            if (shouldCreateDataFile) {
                // Create tox data save file
                try {
                    toxData = createToxData(toxMeName.username());
                } catch (Exception e) {
                    AntoxLog.debug("Failed creating tox data save file", AntoxLog.DEFAULT_TAG());
                }
            } else {
                loadToxData(toxMeName.username());
            }

            if (toxData != null) {
                Observable observable = null;
                if (shouldRegister) {
                    // Register acccount
                    if (ConnectionManager.isNetworkAvailable(this)) {
                        Option<java.net.Proxy> proxy = ProxyUtils.netProxyFromPreferences(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
                        observable = ToxMe.registerAccount(toxMeName, ToxMe.PrivacyLevel$.MODULE$.PUBLIC()/*ToxMe.PrivacyLevel.PUBLIC()*/, toxData, proxy).asJavaObservable();
                    } else {
                        // fail if there is no connection
                        observable = Observable.just(new Left(ToxMeError.CONNECTION_ERROR()));
                    }
                } else {
                    //succeed with empty password
                    observable = Observable.just(new Right(""));
                }

                final ToxData data = toxData;
                observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler()).subscribe(result -> {
                    onRegistrationResult(toxMeName, data, (Either<ToxMeError, String>) result);
                },
                error -> {
                    AntoxLog.debug("Unexpected error registering account.", AntoxLog.DEFAULT_TAG());
                    ((Throwable) error).printStackTrace();
                });
            } else {
                enableRegisterButton();
            }
        }
    }

    private void onRegistrationResult(ToxMeName toxMeName, ToxData toxData, Either<ToxMeError, String> result) {
        boolean successful = true;
        String accountPassword = "";
        String toastMessage = null;

        if (result instanceof Left) {
            Left left = (Left) result;
            successful = false;
            Enumeration.Value error = (Enumeration.Value) left.a();
            if (error != null) {
                if (error.equals(ToxMeError.NAME_TAKEN())) {
                    toastMessage = getString(R.string.create_account_exists);
                } else if (error.equals(ToxMeError.INTERNAL())) {
                    toastMessage = getString(R.string.create_account_internal_error);
                } else if (error.equals(ToxMeError.RATE_LIMIT())) {
                    toastMessage = getString(R.string.create_account_reached_registration_limit);
                } else if (error.equals( ToxMeError.KALIUM_LINK_ERROR())) {
                    toastMessage = getString(R.string.create_account_kalium_link_error);
                } else if (error.equals( ToxMeError.INVALID_DOMAIN())) {
                    toastMessage = getString(R.string.create_account_invalid_domain);
                } else if (error.equals(ToxMeError.CONNECTION_ERROR())) {
                    toastMessage = getString(R.string.create_account_connection_error);
                } else {
                    toastMessage = getString(R.string.create_account_unknown_error);
                }
            } else {
                toastMessage = getString(R.string.create_account_unknown_error);
            }
        } else {
            if (!(result instanceof Right)) {
                throw new MatchError(result);
            }
            Right right = (Right) result;
            successful = true;
            accountPassword = (String) right.b();
            toastMessage = null;
        }

        if (successful) {
            State.userDb(this).addUser(toxMeName, toxData.address(), "");
            loginAndStartMain(toxMeName.username(), accountPassword);
        } else {
            if (toastMessage != null) {
                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
            }
        }
        enableRegisterButton();
    }

    public void onClickImportProfile(View view) {
        EditText accountField = (EditText) findViewById(R.id.create_account_name);

        File path = Environment.getExternalStorageDirectory();

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = path;
        properties.error_dir = path;
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(this, properties);
        dialog.setTitle(R.string.select_file);

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                // files is the array of the paths of files selected by the Application User.
                // since we only want single file selection, use the first entry
                if (files != null) {
                    if (files.length > 0) {
                        if (files[0] != null) {
                            if (files[0].length() > 0) {
                                File filePath = new File(files[0]);
                                onImportFileSelected(filePath, accountField.getText().toString());
                            }
                        }
                    }
                }
            }
        });
        dialog.show();
    }

    private void onImportFileSelected(File file, String accountFieldName) {
        if (file != null) {
            if (!file.getName().contains(".tox")) {
                Toast.makeText(getApplicationContext(), getString(R.string.import_profile_invalid_file), Toast.LENGTH_SHORT).show();
                return;
            }

            final String accountName;
            if (accountFieldName.isEmpty()) {
                accountName = file.getName().replace(".tox", "");
            } else {
                accountName = accountFieldName;
            }

            if (validAccountName(accountName)) {
                File importedFile = new File(getFilesDir().getAbsolutePath() + "/" + accountName);
                FileUtils.copy(file, importedFile);
                ToxDataFile toxDataFile = new ToxDataFile(this, accountName);
                if (toxDataFile.isEncrypted()) {
                    AntoxLog.debug("Profile is encrypted", AntoxLog.DEFAULT_TAG());
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(getString(R.string.login_profile_encrypted));
                    EditText input = new EditText(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(input);
                    builder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            try {
                                toxDataFile.decrypt(input.getText().toString());
                                createAccount(accountName, State.userDb(getApplicationContext()), false, false);
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), getString(R.string.login_passphrase_incorrect), Toast.LENGTH_SHORT).show();
                                e.printStackTrace();
                            }
                        }
                    });
                    builder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //throw new Exception("No password specified.");
                            Toast.makeText(getApplicationContext(), "No password specified.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    builder.show();
                } else {
                    createAccount(accountName, State.userDb(this), false, false);
                }
            } else {
                showBadAccountNameError();
            }
        } else {
            //throw new Exception("Could not load data file.");
            Toast.makeText(getApplicationContext(), "Could not load data file.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onClickRegisterAccount(View view) {
        EditText accountField = (EditText) findViewById(R.id.create_account_name);
        String account = accountField.getText().toString();

        UserDB userDb = State.userDb(this);

        boolean shouldRegister = ((CheckBox) findViewById(R.id.toxme)).isChecked();
        createAccount(account, userDb, true, shouldRegister);
    }

}