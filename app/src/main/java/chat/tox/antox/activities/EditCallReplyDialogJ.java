package chat.tox.antox.activities;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.wrapper.CallReply;

/**
 * Created by Nechypurenko on 14.02.2018.
 */

public class EditCallReplyDialogJ extends DialogFragment {

    private static final String EXTRA_CALL_REPLY_ID = "call_reply_id";
    private static final String EXTRA_CALL_REPLY_REPLY = "call_reply_reply";

    public static EditCallReplyDialog newInstance(CallReply callReply) {
        EditCallReplyDialog editCallReplyDialog = new EditCallReplyDialog();
        Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_CALL_REPLY_ID, callReply.id());
        bundle.putString(EXTRA_CALL_REPLY_REPLY, callReply.reply());
        editCallReplyDialog.setArguments(bundle);
        return editCallReplyDialog;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        CallReply callReply = new CallReply(getArguments().getInt(EXTRA_CALL_REPLY_ID), getArguments().getString(EXTRA_CALL_REPLY_REPLY));

        View customView = getActivity().getLayoutInflater().inflate(R.layout.dialog_edit_call_replies, null);
        EditText editText = (EditText) customView.findViewById(R.id.call_reply_text);
        editText.setText(callReply.reply());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.edit_call_reply_title)
                .setView(customView);

        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newReplyString = editText.getText().toString();
                //prevent the preset translatable replies from being overwritten
                if (!newReplyString.equals(callReply.reply())) {
                    CallReply newReply = new CallReply(callReply.id(), newReplyString);
                    State.userDb(getActivity()).updateActiveUserCallReply(newReply);
                }
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        return builder.show();
    }
}
