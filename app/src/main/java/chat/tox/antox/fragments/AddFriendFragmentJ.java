package chat.tox.antox.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.Proxy;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.toxme.ToxMe;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.ProxyUtils;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.FriendKey;
import chat.tox.antox.wrapper.ToxAddress;
import im.tox.tox4j.core.ToxCoreConstants;
import im.tox.tox4j.core.data.ToxFriendRequestMessage;
import rx.Observable;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Option;

/**
 * Created by Nechypurenko on 08.02.2018.
 */

public class AddFriendFragmentJ extends Fragment implements InputableIDJ {

    private String _friendID = "";
    private String _friendCHECK = "";
    private String _originalUsername = "";
    private Context context = null;
    private CharSequence text = null;
    private int duration = Toast.LENGTH_SHORT;
    private Toast toast = null;
    private EditText friendID = null;
    private EditText friendMessage = null;
    private EditText friendAlias = null;
    private Subscription lookupSubscription;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //return super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_add_friend, container, false);
        getActivity().overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out);

        context = getActivity().getApplicationContext();

        text = getString(R.string.addfriend_friend_added);
        friendID = (EditText) rootView.findViewById(R.id.addfriend_key);
        friendMessage = (EditText) rootView.findViewById(R.id.addfriend_message);
        friendAlias = (EditText) rootView.findViewById(R.id.addfriend_friendAlias);

        friendMessage.setFilters(new InputFilter.LengthFilter[]{new InputFilter.LengthFilter(ToxCoreConstants.MaxFriendRequestLength())});

        ((Button)rootView.findViewById(R.id.add_friend_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFriend(view);
            }
        });
        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(lookupSubscription != null) {
            lookupSubscription.unsubscribe();
        }
    }

    @Override
    public void inputID(String input) {
        EditText addFriendKey = (EditText) getView().findViewById(R.id.addfriend_key);
        String friendAddress = UiUtilsJ.sanitizeAddress(ToxAddress.removePrefix(input));
        if (ToxAddress.isAddressValid(friendAddress)) {
            addFriendKey.setText(friendAddress);
        } else {
            Context context = getActivity().getApplicationContext();
            Toast toast = Toast.makeText(context, getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    private boolean isAddressOwn(ToxAddress address) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String ownAddress = ToxAddress.removePrefix(preferences.getString("tox_id", ""));
        return new ToxAddress(ownAddress).equals(address);
    }

    private boolean checkAndSend(String rawAddress, String originalUsername) {
        if (ToxAddress.isAddressValid(rawAddress)) {
            ToxAddress address = new ToxAddress(rawAddress);

            if (!isAddressOwn(address)) {
                FriendKey key = address.key();
                String message = friendMessage.getText().toString();
                String alias = friendAlias.getText().toString();

                if (message.equals("")) {
                    message = getString(R.string.addfriend_default_message);
                }
//                if(alias.equals("")) {
//                    Toast.makeText(context, getString(R.string.addfriend_nick_hint), Toast.LENGTH_SHORT).show();
//                    return false;
//                }

                AntoxDB db = State.db();
                if (!db.doesContactExist(key)) {
                    try {
                        ToxSingleton.tox().addFriend(address, ToxFriendRequestMessage.unsafeFromValue(message.getBytes()));
                        ToxSingleton.save();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //}
                    db.addFriend(key, originalUsername, alias, "Friend Request Sent");

                    //prevent already-added friend from having an existing friend request
                    db.deleteFriendRequest(key);
                    AntoxNotificationManager.clearRequestNotification(key);
                } else {
                    toast = Toast.makeText(context, getString(R.string.addfriend_friend_exists), Toast.LENGTH_SHORT);
                    toast.show();
                }
                toast = Toast.makeText(context, text, duration);
                toast.show();
                return true;
            } else {
                toast = Toast.makeText(context, getString(R.string.addfriend_own_key), Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
        } else {
            showToastInvalidID();
            return false;
        }
    }

    public void addFriend(View view) {
        if (friendID.length() == 76) {
            // Attempt to use ID as a Tox ID
            boolean result = checkAndSend(friendID.getText().toString(), _originalUsername);
            if (result) {
                Intent update = new Intent(ConstantsJ.BROADCAST_ACTION);
                update.putExtra("action", ConstantsJ.UPDATE);
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(update);
                Intent i = new Intent();
                getActivity().setResult(Activity.RESULT_OK, i);
                getActivity().finish();
            }
        } else {
            // Attempt to use ID as a toxme account name
            _originalUsername = friendID.getText().toString();
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                Option<Proxy> proxy = ProxyUtils.netProxyFromPreferences(preferences);

                Observable observable = ToxMe.lookup(_originalUsername, proxy).asJavaObservable();
                observable = observable.subscribeOn(IOScheduler.apply().asJavaScheduler());
                observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
                lookupSubscription = observable.subscribe(o -> {
                    Option<String> data = (Option<String>) o;
                    if(!data.isEmpty()) {
                        String key = data.get();
                        boolean result = checkAndSend(key, _originalUsername);
                        if (result) {
                            Intent update = new Intent(ConstantsJ.BROADCAST_ACTION);
                            update.putExtra("action", ConstantsJ.UPDATE);
                            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(update);
                            Intent i = new Intent();
                            getActivity().setResult(Activity.RESULT_OK, i);
                            getActivity().finish();
                        }
                    } else {
                        showToastInvalidID();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showToastInvalidID() {
        toast = Toast.makeText(context, getString(R.string.invalid_friend_ID), Toast.LENGTH_SHORT);
        toast.show();
    }

}
