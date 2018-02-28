package chat.tox.antox.activities;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.wrapper.CallReply;
import scala.Option;
import scala.collection.JavaConversions;

/**
 * Created by Nechypurenko on 14.02.2018.
 */

public class CallReplyDialogJ extends DialogFragment {

    private CallReplySelectedListenerJ listener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (CallReplySelectedListenerJ) getActivity();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString() + " must implement CallReplySelectedListenerJ");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //return super.onCreateDialog(savedInstanceState);

        List<CallReply> replies = JavaConversions.bufferAsJavaList(State.userDb(getActivity()).getActiveUserCallReplies());
        String customReply = getString(R.string.call_incoming_reply_dialog_custom);
        int size = replies.size() + 1;
        String[] replyStrings = new String[size];

        for(int i = 0; i < replies.size(); i++) {
            replyStrings[i] = replies.get(i).reply();
        }
        replyStrings[size-1] = customReply;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.call_incoming_reply_dialog_title);
        builder.setItems(replyStrings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedReply = replyStrings[which];
                listener.onCallReplySelected(Option.apply(selectedReply));
            }
        });
        return builder.create();
    }

}
