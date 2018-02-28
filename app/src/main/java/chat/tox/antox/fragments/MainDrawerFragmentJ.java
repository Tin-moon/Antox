package chat.tox.antox.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import chat.tox.antox.R;
import chat.tox.antox.activities.AboutActivityJ;
import chat.tox.antox.activities.ProfileSettingsActivityJ;
import chat.tox.antox.activities.SettingsActivityJ;
import chat.tox.antox.callbacks.AntoxOnSelfConnectionStatusCallback;
import chat.tox.antox.data.State;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.IconColor;
import chat.tox.antox.wrapper.FileKind;
import chat.tox.antox.wrapper.UserInfo;
import chat.tox.antox.wrapper.UserStatus;
import de.hdodenhof.circleimageview.CircleImageView;
import im.tox.tox4j.core.enums.ToxConnection;
import im.tox.tox4j.core.enums.ToxUserStatus;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func2;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import scala.Option;
import scala.Tuple2;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class MainDrawerFragmentJ extends Fragment {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private SharedPreferences preferences;
    private Subscription userDetailsSubscription;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main_drawer, container, false);

        // Set up the navigation drawer
        mDrawerLayout = (DrawerLayout) rootView.findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) rootView.findViewById(R.id.left_drawer);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                selectItem(item);
                return true;
            }

        });

        View drawerHeader = rootView.findViewById(R.id.drawer_header);

        // zoff //
        if (drawerHeader != null) {
            drawerHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), ProfileSettingsActivityJ.class);
                    startActivity(intent);
                }
            });
        }

        // zoff //
        if (drawerHeader != null) {
            drawerHeader.setBackgroundColor(ThemeManagerJ.primaryColorDark());
        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        View drawerHeader = getView().findViewById(R.id.drawer_header);
        if (drawerHeader != null) {
            drawerHeader.setBackgroundColor(ThemeManagerJ.primaryColorDark());
        }

        Observable dataObservable = State.userDb(getActivity()).activeUserDetailsObservable().asJavaObservable();
        Observable conObservable = AntoxOnSelfConnectionStatusCallback.connectionStatusSubject().asJavaObservable();
        Observable observable = rx.Observable.combineLatest(dataObservable, conObservable, new Func2<UserInfo, ToxConnection, Tuple2>() {
            @Override
            public Tuple2 call(UserInfo userInfo, ToxConnection toxConnection) {
                return new Tuple2(userInfo, toxConnection);
            }
        });
        observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        userDetailsSubscription = observable.subscribe(o -> {
            Tuple2<UserInfo, ToxConnection> tuple = (Tuple2<UserInfo, ToxConnection>) o;
            refreshDrawerHeader(tuple._1(), tuple._2());
        });
    }

    public void refreshDrawerHeader(UserInfo userInfo, ToxConnection connectionStatus) {
        CircleImageView avatarView = (CircleImageView) getView().findViewById(R.id.drawer_avatar);

        Option<File> mAvatar = FileKind.AVATAR$.MODULE$.getAvatarFile(userInfo.avatarName(), getActivity());

        // zoff //
        if (avatarView != null && mAvatar.isDefined()) {
            if (mAvatar.isDefined()) {
                Observable observable = BitmapManager.load(mAvatar.get(), true).asJavaObservable();
                observable.subscribe(o -> {
                    avatarView.setImageBitmap((Bitmap)o);
                });
            } else {
                avatarView.setImageResource(R.drawable.default_avatar);
            }
        }

        TextView nameView = (TextView) getView().findViewById(R.id.name);

        // zoff //
        if (nameView != null) {
            nameView.setText(new String(userInfo.nickname()));
        }
        TextView statusMessageView = (TextView) getView().findViewById(R.id.status_message);
        // zoff //
        if (statusMessageView != null) {
            statusMessageView.setText(new String(userInfo.statusMessage()));
        }

        updateNavigationHeaderStatus(connectionStatus);
    }

    public void  updateNavigationHeaderStatus(ToxConnection toxConnection) {
        View statusView = getView().findViewById(R.id.status);

        ToxUserStatus status = UserStatus.getToxUserStatusFromString(State.userDb(getActivity()).getActiveUserDetails().status());
        boolean online = toxConnection != ToxConnection.NONE;
        Drawable drawable = getResources().getDrawable(IconColor.iconDrawable(online, status));

        // zoff //
        if (statusView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                statusView.setBackground(drawable);
            } else {
                statusView.setBackgroundDrawable(drawable);
            }
        }
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void openDrawer() {
        mDrawerLayout.openDrawer(GravityCompat.START);
    }

    public void closeDrawer() {
        mDrawerLayout.closeDrawer(GravityCompat.START);
    }

    private void selectItem(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_profile_options: {
                Intent intent = new Intent(getActivity(), ProfileSettingsActivityJ.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_settings: {
                Intent intent = new Intent(getActivity(), SettingsActivityJ.class);
                startActivity(intent);
                break;
            }

            case R.id.nav_create_group: {
                //TODO: uncomment for the future
                /* val dialog = new CreateGroupDialog(this)
                dialog.addCreateGroupListener(new CreateGroupListener {
                  override def groupCreationConfimed(name: String): Unit = {
                    val groupNumber = ToxSingleton.tox.newGroup(name)
                    val groupId = ToxSingleton.tox.getGroupChatId(groupNumber)
                    val db = State.db

                    db.addGroup(groupId, name, "")
                    ToxSingleton.updateGroupList(getApplicationContext)
                  }
                })
                dialog.showDialog()
                */
                Toast.makeText(getActivity(), getString(R.string.main_group_coming_soon), Toast.LENGTH_LONG).show();
                break;
            }

            case R.id.nav_about: {
                Intent intent = new Intent(getActivity(), AboutActivityJ.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_logout: {
                if (State.loggedIn(getActivity())) {
                    State.logout(getActivity());
                }
                break;
            }
        }
        menuItem.setChecked(false);
        mDrawerLayout.closeDrawer(mNavigationView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(userDetailsSubscription != null) {
            userDetailsSubscription.unsubscribe();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
