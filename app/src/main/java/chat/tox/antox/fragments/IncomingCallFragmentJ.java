package chat.tox.antox.fragments;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import chat.tox.antox.R;
import chat.tox.antox.activities.CallReplyDialogJ;
import chat.tox.antox.av.Call;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.wrapper.ContactKey;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class IncomingCallFragmentJ extends CommonCallFragmentJ {

    public static IncomingCallFragment newInstance(Call call, ContactKey activeKey) {
        IncomingCallFragment incomingCallFragment = new IncomingCallFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(CommonCallFragmentJ.EXTRA_CALL_NUMBER, call.callNumber());
        bundle.putString(CommonCallFragmentJ.EXTRA_ACTIVE_KEY, activeKey.toString());
        bundle.putInt(CommonCallFragmentJ.EXTRA_FRAGMENT_LAYOUT, R.layout.fragment_incoming_call);
        incomingCallFragment.setArguments(bundle);
        return incomingCallFragment;
    }

    private Vibrator vibrator;
    private long[] vibrationPattern = new long[]{0, 1000, 1000}; //wait 0ms vibrate 1000ms off 1000ms
    private View answerCallButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(vibrationPattern, 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        rootView.findViewById(R.id.call_duration).setVisibility(View.GONE);

        /* Set up the answer and av buttons */
        answerCallButton = rootView.findViewById(R.id.answer_call_circle);

        answerCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call.answerCall();
            }
        });

        LinearLayout replyButton = (LinearLayout) rootView.findViewById(R.id.incoming_call_reply);
        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String callReplyDialogTag = "call_reply_dialog";
                new CallReplyDialogJ().show(getFragmentManager(), callReplyDialogTag);
            }
        });

        // vibrate and ring on incoming call
        AntoxLog.debug("Audio stream volume " + audioManager.getStreamVolume(AudioManager.STREAM_RING), AntoxLog.DEFAULT_TAG());
        callStateView.setVisibility(View.VISIBLE);
        if (call.selfState().receivingVideo()) {
            callStateView.setText(R.string.call_incoming_video);
        } else {
            callStateView.setText(R.string.call_incoming_voice);
        }

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        vibrator.cancel();
    }

}
