package chat.tox.antox.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class CreateGroupDialogJ {

    public interface CreateGroupListener {
        void groupCreationConfimed(String name);
    }

    private List<CreateGroupListener> createGroupListenerList = new ArrayList<CreateGroupListener>();
    private boolean wrapInScrollView = true;
    private EditText nameInput = null;
    private final Context context;
    private final AlertDialog dialog;

    public CreateGroupDialogJ(Context context) {
        this.context = context;

        dialog = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle)
                .setTitle(R.string.create_group_dialog_message)
                .setView(R.layout.fragment_create_group)
                .setPositiveButton(R.string.create_group_dialog_create_group, null)
                .setNegativeButton(R.string.create_group_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        triggerCreateGroupEvent(nameInput.getText().toString());
                    }
                }).create();

        nameInput = (EditText) dialog.findViewById(R.id.group_name);
        Button positiveAction = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveAction.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        positiveAction.setEnabled(false);
    }

    public void showDialog() {
        dialog.show();
    }

    public void addCreateGroupListener(CreateGroupListener listener) {
        createGroupListenerList.add(listener);
    }

    public void triggerCreateGroupEvent(String groupName) {
        for (CreateGroupListener listener : createGroupListenerList) {
            listener.groupCreationConfimed(groupName);
        }
    }

}
