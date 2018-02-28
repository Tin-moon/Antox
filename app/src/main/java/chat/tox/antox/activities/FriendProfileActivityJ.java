package chat.tox.antox.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.wrapper.FriendKey;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by Nechypurenko on 12.02.2018.
 */

public class FriendProfileActivityJ extends AppCompatActivity {

    private FriendKey friendKey = null;
    private boolean nickChanged = false;

    @SuppressLint("StringFormatInvalid")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_profile);

        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                bar.setIcon(R.drawable.ic_actionbar);
            }
        }


        ThemeManagerJ.applyTheme(this, bar);

        friendKey = new FriendKey(getIntent().getStringExtra("key"));
        AntoxDB db = State.db();
        String friendNote = db.getFriendInfo(friendKey).statusMessage();

        setTitle(String.format(getString(R.string.friend_profile_title), getIntent().getStringExtra("name")));

        EditText editFriendAlias = (EditText) findViewById(R.id.friendAlias);
        editFriendAlias.setText(getIntent().getStringExtra("name"));
        editFriendAlias.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                /* Set nick changed to true in order to save change in onPause() */
                nickChanged = true;
                /* Update title to reflect new nick */
                setTitle(String.format(getString(R.string.friend_profile_title), editFriendAlias.getText().toString()));
            }
        });

        // Set cursor to end of edit text field
        editFriendAlias.setSelection(editFriendAlias.length(), editFriendAlias.length());

        TextView editFriendNote = (TextView) findViewById(R.id.friendNoteText);
        editFriendNote.setText("\"" + friendNote + "\"");

//        Option<File> avatar = (Option<File>) getIntent().getSerializableExtra("avatar");
//        avatar.foreach(v1 -> {
//            final CircleImageView avatarHolder = (CircleImageView) findViewById(R.id.profile_avatar);
//            //BitmapManager.load(v1, true).foreach(avatarHolder.setImageBitmap());
//            BitmapManager.load(v1, true).foreach(v11 -> {avatarHolder.setImageBitmap(v11); return null;});
//            return null;
//        });

        File avatar = (File) getIntent().getSerializableExtra("avatar");
        if(avatar != null) {
            final CircleImageView avatarHolder = (CircleImageView) findViewById(R.id.profile_avatar);
            BitmapManager.load(avatar, true).asJavaObservable().forEach(o -> avatarHolder.setImageBitmap(o));
        }

        updateFab(db.getFriendInfo(friendKey).favorite());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Intent intent = new Intent(this, MainActivityJ.class);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        /* Update friend alias after text has been changed */
        if (nickChanged) {
            EditText editFriendAlias = (EditText) findViewById(R.id.friendAlias);
            State.db().updateAlias(editFriendAlias.getText().toString(), friendKey);
        }
    }

    public void onClickFavorite(View view) {
        AntoxDB db = State.db();
        boolean favorite = !db.getFriendInfo(friendKey).favorite();
        db.updateContactFavorite(friendKey, favorite);
        updateFab(favorite);
    }

    public void updateFab(boolean favorite) {
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.favorite_button);
        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(favorite ? R.color.material_red_a700 : R.color.white)));
        if (favorite) {
            Drawable drawable = getDrawable(R.drawable.ic_star_black_24dp);
            drawable.setColorFilter(getResources().getColor(R.color.brand_primary), PorterDuff.Mode.MULTIPLY);
            fab.setImageDrawable(drawable);
        } else {
            fab.setImageDrawable(getDrawable(R.drawable.ic_star_outline_black_24dp));
        }
    }


}
