package chat.tox.antox.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;
import android.widget.TextView;

import chat.tox.antox.R;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.Group;
import chat.tox.antox.wrapper.GroupKey;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class GroupProfileActivityJ extends AppCompatActivity {

    private String groupName;

    private GroupKey groupKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_group_profile);
        groupKey = new GroupKey(getIntent().getStringExtra("key"));

        ThemeManagerJ.applyTheme(this, getSupportActionBar());

        Group group = ToxSingleton.getGroup(groupKey);
        setTitle(getString(R.string.title_activity_group_profile));

        ((TextView)findViewById(R.id.group_name)).setText(group.name() != null ? group.name() : UiUtilsJ.trimId(group.key()));

        ((EditText)findViewById(R.id.group_status_message)).setText(group.topic());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            getSupportActionBar().setIcon(R.drawable.ic_actionbar);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

}
