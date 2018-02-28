package chat.tox.antox.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.data.UserDB;
import chat.tox.antox.tox.ToxService;
import chat.tox.antox.utils.Options;
import scala.collection.Iterator;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class LoginActivityJ extends AppCompatActivity implements AdapterView.OnItemSelectedListener {


    private String profileSelected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        UserDB userDb = State.userDb(this);

        State.setAutoAcceptFt(preferences.getBoolean("autoacceptft", false));
        Options.videoCallStartWithNoVideo_$eq(preferences.getBoolean("videocallstartwithnovideo", false));
        State.setBatterySavingMode(preferences.getBoolean("batterysavingmode", false));

        // if the user is starting the app for the first
        // time, go directly to the register account screen
        if (userDb.numUsers() == 0) {
            Intent createAccount = new Intent(getApplicationContext(), CreateAccountActivityJ.class);
            createAccount.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(createAccount);
            finish();
        } else if (userDb.loggedIn()) {
            Intent startTox = new Intent(getApplicationContext(), ToxService.class);
            getApplicationContext().startService(startTox);
            Intent main = new Intent(getApplicationContext(), MainActivityJ.class);
            startActivity(main);
            finish();
        } else {

//            ArrayList<String> profiles = JavaConversions.bufferAsJavaList(userDb.getAllProfiles()); //TODO???
            List<String> profiles = new ArrayList<>();
            Iterator<String> iteratorToList = userDb.getAllProfiles().iterator();
            while (iteratorToList.hasNext()) {
                profiles.add(iteratorToList.next());
            }

            Spinner profileSpinner = (Spinner) findViewById(R.id.login_account_name);
            ArrayAdapter adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, profiles);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            profileSpinner.setAdapter(adapter);
            profileSpinner.setSelection(0);
            profileSpinner.setOnItemSelectedListener(this);
        }

        // this may get the app banned from google play :-(
        // ShowPermissionDialog()
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        profileSelected = parent.getItemAtPosition(position).toString();
        if (parent.getChildAt(0) != null) {
            // getChildAt(pos) returns a view, or null if non-existant
            ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) { }

    public void onClickLogin(View view) {
        String account = profileSelected;
        if (account.equals("")) {
            Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.login_must_fill_in),  Toast.LENGTH_SHORT);
            toast.show();
        } else {
            UserDB userDb = State.userDb(this);
            if (userDb.doesUserExist(account)) {
                //Option<UserInfo> details = userDb.getUserDetails(account);
                State.login(account, this);
                Intent startTox = new Intent(getApplicationContext(), ToxService.class);
                getApplicationContext().startService(startTox);
                Intent main = new Intent(getApplicationContext(), MainActivityJ.class);
                startActivity(main);
                finish();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.login_bad_login), Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    public void  onClickCreateAccount(View view) {
        Intent createAccount = new Intent(getApplicationContext(), CreateAccountActivityJ.class);
        startActivityForResult(createAccount, 1);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                finish();
            }
        }
    }

}
