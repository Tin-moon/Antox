package chat.tox.antox.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.GroupKey;
import chat.tox.antox.wrapper.ToxAddress;
import chat.tox.antox.wrapper.ToxKey$;

/**
 * Created by Nechypurenko on 08.02.2018.
 */

public class AddGroupFragmentJ extends Fragment implements InputableIDJ {

    private GroupKey groupKey;
    private String originalUsername = "";
    private Context context;
    private String text;
    private int duration = Toast.LENGTH_SHORT;
    private Toast toast;
    private EditText groupKeyView;
    private EditText groupAlias;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View rootView = inflater.inflate(R.layout.fragment_add_group, container, false);
        getActivity().overridePendingTransition(R.anim.slide_from_bottom, R.anim.fade_scale_out);

        context = getActivity().getApplicationContext();

        text = getString(R.string.addgroup_group_added);
        groupKeyView = (EditText) rootView.findViewById(R.id.addgroup_key);
        groupAlias = (EditText) rootView.findViewById(R.id.addgroup_groupAlias);

        ((Button)rootView.findViewById(R.id.add_group_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: Uncomment this for the future
                    //addGroup(view)
                    Toast.makeText(getActivity(), getString(R.string.main_group_coming_soon), Toast.LENGTH_LONG).show();
            }
        });

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void inputID(String input) {
        EditText addGroupKey = (EditText) getView().findViewById(R.id.addgroup_key);
        String groupKey = UiUtilsJ.sanitizeAddress(ToxAddress.removePrefix(input));

        if (ToxKey$.MODULE$.isKeyValid(groupKey)) {
            addGroupKey.setText(groupKey);
        } else {
            showToastInvalidID();
        }
    }

    private boolean checkAndSend(String rawGroupKey, String originalUsername) {
        if (ToxKey$.MODULE$.isKeyValid(rawGroupKey)) {
            GroupKey key = new GroupKey(rawGroupKey);
            String alias = groupAlias.getText().toString(); //TODO: group aliases

            AntoxDB db = State.db();
            if (!db.doesContactExist(key)) {
                try {
                    ToxSingleton.tox().joinGroup(key);
                    AntoxLog.debug("joined group : " + groupKeyView, AntoxLog.DEFAULT_TAG());
                    ToxSingleton.save();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                db.addGroup(key, UiUtilsJ.trimId(key),"");

                //prevent already-added group from having an existing group invite
                db.deleteGroupInvite(key);
                AntoxNotificationManager.clearRequestNotification(key);
            } else {
                toast = Toast.makeText(context, getString(R.string.addgroup_group_exists), Toast.LENGTH_SHORT);
                toast.show();
                return false;
            }
            toast = Toast.makeText(context, text, duration);
            toast.show();
            return true;
        } else {
            showToastInvalidID();
            return false;
        }
    }

    public void addGroup(View view) {
        if (groupKeyView.length() == 64) {
            // Attempt to use ID as a Group ID
            boolean result = checkAndSend(groupKeyView.getText().toString(), originalUsername);
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
    }

    public void showToastInvalidID() {
        toast = Toast.makeText(context, getString(R.string.invalid_group_ID), Toast.LENGTH_SHORT);
        toast.show();
    }

}
