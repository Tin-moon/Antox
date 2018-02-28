package chat.tox.antox.activities;

import android.app.ActivityManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;

import java.util.regex.Pattern;

import chat.tox.antox.R;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class ToxMeInfoActivityJ extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_toxme_info);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(Color.parseColor("#202020"));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && getSupportActionBar() != null) {
            ActivityManager.RunningTaskInfo info = new ActivityManager.RunningTaskInfo();
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#24221f")));
        }

        TextView toxMeWebsite = (TextView) findViewById(R.id.toxme_info_website);
        toxMeWebsite.setMovementMethod(LinkMovementMethod.getInstance());
        toxMeWebsite.setText(Html.fromHtml(getString(R.string.toxme_website)));

        TextView sourceURLTextView = (TextView) findViewById(R.id.toxme_source);
        Pattern pattern = Pattern.compile("https://github.com/LittleVulpix/toxme");
        Linkify.addLinks(sourceURLTextView, pattern, "");


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

}
