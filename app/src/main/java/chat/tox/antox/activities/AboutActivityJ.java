package chat.tox.antox.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.regex.Pattern;

import chat.tox.antox.R;
import chat.tox.antox.theme.ThemeManagerJ;

/**
 * Created by Nechypurenko on 08.02.2018.
 */

public class AboutActivityJ extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about);

        setTitle(R.string.about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ThemeManagerJ.applyTheme(this, getSupportActionBar());

        TextView versionTextView = (TextView) findViewById(R.id.version_text);
        TextView sourceURLTextView = (TextView) findViewById(R.id.source_link);

        // Make the source URL a clickable link
        Pattern pattern = Pattern.compile("https://github.com/Antox/Antox");
        Linkify.addLinks(sourceURLTextView, pattern, "");

        // Fetch the app version and set it in the versionTextView, falling back to a blank version if unable to
        String version_name = "-.-.-";
        int version_code = 0;
        try {
            version_name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            e.printStackTrace();
        }


        try {
            version_code = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        versionTextView.setText(getString(R.string.ver) + " " + version_name + " (" + version_code + ")");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
