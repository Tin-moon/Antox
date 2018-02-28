package chat.tox.antox.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.av.Call;
import chat.tox.antox.av.CallEndReason;
import chat.tox.antox.data.State;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.wrapper.CallNumber;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.FriendInfo;
import chat.tox.antox.wrapper.FriendKey;
import de.hdodenhof.circleimageview.CircleImageView;
import rx.Observable;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.subscriptions.CompositeSubscription;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public abstract class CommonCallFragmentJ extends Fragment {

    public static final String EXTRA_CALL_NUMBER = "call_number";
    public static final String EXTRA_ACTIVE_KEY = "active_key";
    public static final String EXTRA_FRAGMENT_LAYOUT = "fragment_layout";


    protected Call call;
    protected ContactKey activeKey;
    private int callLayout;

    protected RelativeLayout upperCallHalfView;
    protected TextView callStateView;
    protected TextView nameView;
    protected CircleImageView avatarView;
    private View endCallButton;

    protected CompositeSubscription compositeSubscription = new CompositeSubscription();
    protected AudioManager audioManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock maybeWakeLock;
    private Subscription videoWakeLockSubscription;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Option<Call> callOption = State.callManager().get(new CallNumber(getArguments().getInt(CommonCallFragmentJ.EXTRA_CALL_NUMBER)).value());
        if(callOption.isDefined()) {
            call = callOption.get();
        }

        if (call == null) {
            AntoxLog.debug("Ending call which has an invalid call number", AntoxLog.DEFAULT_TAG());
            getActivity().finish();
            return;
        }

        activeKey = new FriendKey(getArguments().getString(CommonCallFragmentJ.EXTRA_ACTIVE_KEY));
        callLayout = getArguments().getInt(CommonCallFragmentJ.EXTRA_FRAGMENT_LAYOUT);

        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        // power manager used to turn off screen when the proximity sensor is triggered
        powerManager = (PowerManager) getActivity().getSystemService(Context.POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                maybeWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, AntoxLog.DEFAULT_TAG().toString());
            }
        }
    }

    private void updateDisplayedState(List<FriendInfo> friendInfoList) {
        FriendInfo mFriend = null;
        for(FriendInfo fi : friendInfoList) {
            if(fi.key().equals(activeKey)) {
                mFriend = fi;
                break;
            }
        }

        FriendInfo finalMFriend = mFriend;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(finalMFriend != null) {
                    nameView.setText(finalMFriend.getDisplayName());
                    File avatar = finalMFriend.avatar().isDefined() ? finalMFriend.avatar().get() : null;
                    if(avatar != null) {
                        Bitmap bitmap = BitmapManager.loadBlocking(avatar, true);
                        avatarView.setImageBitmap(bitmap);
                    }
                } else {
                    nameView.setText("");
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(callLayout, container, false);

        callStateView = (TextView) rootView.findViewById(R.id.call_state_text);

        upperCallHalfView = (RelativeLayout) rootView.findViewById(R.id.call_upper_half);
        avatarView = (CircleImageView) rootView.findViewById(R.id.call_avatar);
        nameView = (TextView) rootView.findViewById(R.id.friend_name);

        // Set up the end call and av buttons
        endCallButton = rootView.findViewById(R.id.end_call_circle);

        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call.end(CallEndReason.Normal());
                endCallButton.setEnabled(false); //don't let the user press end call more than once
            }
        });

        // update displayed friend info on change
        Observable observable = State.db().friendInfoList().asJavaObservable();
        observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        Subscription subscription = observable.subscribe(o -> {
            updateDisplayedState(JavaConversions.seqAsJavaList((Seq<FriendInfo>)o));
        });
        compositeSubscription.add(subscription);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        Observable observable = call.videoEnabledObservable().asJavaObservable();
        videoWakeLockSubscription = observable.subscribe(video -> {
            if(maybeWakeLock != null) {
                if ((boolean) video) {
                    if (maybeWakeLock.isHeld()) {
                        maybeWakeLock.release();
                    }
                } else {
                    if (!maybeWakeLock.isHeld()) {
                        maybeWakeLock.acquire();
                    }
                }
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(videoWakeLockSubscription != null) {
            videoWakeLockSubscription.unsubscribe();
        }
        if(maybeWakeLock != null) {
            if (maybeWakeLock.isHeld()) {
                maybeWakeLock.release();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

}
