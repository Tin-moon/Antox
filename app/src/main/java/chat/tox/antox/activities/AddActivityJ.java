package chat.tox.antox.activities;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import chat.tox.QR.IntentIntegrator;
import chat.tox.QR.IntentResult;
import chat.tox.antox.R;
import chat.tox.antox.fragments.AddPaneFragmentJ;
import chat.tox.antox.fragments.InputableIDJ;
import chat.tox.antox.theme.ThemeManagerJ;


/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class AddActivityJ extends AppCompatActivity {


    private Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out);

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED, WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        context = getApplicationContext();

        setTitle(R.string.title_activity_add);
        setContentView(R.layout.activity_add);
        ThemeManagerJ.applyTheme(this, getSupportActionBar());

        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        if (action != null && action.equals(Intent.ACTION_VIEW)) {
            // Handle incoming tox uri links
            Uri uri = intent.getData();
            if (uri != null) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_add_pane);
                if (fragment instanceof AddPaneFragmentJ) {
                    ((AddPaneFragmentJ) fragment).getSelectedFragment();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_bottom);
    }

    private void scanIntent() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_add_pane);
                if (fragment instanceof AddPaneFragmentJ) {
                    Fragment f = ((AddPaneFragmentJ) fragment).getSelectedFragment();
                    if (f instanceof InputableIDJ) {
                        ((InputableIDJ) f).inputID(scanResult.getContents());
                    }
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_friend, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.scanFriend:
                scanIntent();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
