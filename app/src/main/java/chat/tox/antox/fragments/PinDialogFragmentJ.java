package chat.tox.antox.fragments;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import chat.tox.antox.R;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class PinDialogFragmentJ extends DialogFragment {

    public interface PinDialogListener {
        void onDialogPositiveClick(DialogFragment dialog, String pin);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    private PinDialogListener listener;
    private EditText pin;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            listener = (PinDialogListener)getActivity();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString() + " must implement PinDialogListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_pin, null);
        builder.setView(view);
        pin = (EditText) view.findViewById(R.id.pin);
        builder.setMessage(getString(R.string.dialog_pin));
        builder.setPositiveButton(getString(R.string.button_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onDialogPositiveClick(PinDialogFragmentJ.this, pin.getText().toString());
            }
        });
        builder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onDialogNegativeClick(PinDialogFragmentJ.this);
            }
        });
        return builder.create();
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if(listener != null) {
            listener.onDialogNegativeClick(this);
        }
    }

}
