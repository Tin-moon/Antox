package chat.tox.antox.activities;

import android.animation.Animator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;

import chat.tox.antox.R;
import chat.tox.antox.av.Call;
import chat.tox.antox.av.CallEndReason;
import chat.tox.antox.data.State;
import chat.tox.antox.fragments.ActiveCallFragment;
import chat.tox.antox.fragments.ActiveCallFragmentJ;
import chat.tox.antox.fragments.IncomingCallFragmentJ;
import chat.tox.antox.tox.MessageHelper;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.LocationJ;
import chat.tox.antox.wrapper.CallNumber;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.FriendKey;
import im.tox.tox4j.core.enums.ToxMessageType;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.subscriptions.CompositeSubscription;
import scala.Option;
import scala.Tuple2;

/**
 * Created by Nechypurenko on 13.02.2018.
 */

public class CallActivityJ extends FragmentActivity implements CallReplySelectedListenerJ {

    private Call call;
    private ContactKey activeKey;

    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private FrameLayout rootLayout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        int windowFlags =
                // set this flag so this activity will stay in front of the keyguard
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        // Have the WindowManager filter out touch events that are "too fat".
                        WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.JELLY_BEAN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            windowFlags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
        }
        getWindow().addFlags(windowFlags);
        setContentView(R.layout.activity_call);

        activeKey = new FriendKey(getIntent().getStringExtra("key"));
        CallNumber callNumber = new CallNumber(getIntent().getIntExtra("call_number", -1));

        call = State.callManager().get(callNumber.value()).get();
        if(call == null) {
            AntoxLog.debug("Ending call which has an invalid call number $callNumber", AntoxLog.DEFAULT_TAG());
            finish();
            return;
        }

        if (getIntent().getAction() != null && getIntent().getAction().equals(ConstantsJ.END_CALL)) {
            call.end(CallEndReason.Normal());
            finish();
        }

        compositeSubscription.add(
                call.videoEnabledObservable().combineLatest(call.ringingObservable()).asJavaObservable().subscribe(o -> {
                    Tuple2<Object, Object> data = o;
                    Boolean videoEnabled = (Boolean) data._1();
                    Boolean ringing = (Boolean) data._2();
                    if (!ringing && videoEnabled) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    } else {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    }
                })
        );

        LocationJ clickLocation = (LocationJ) getIntent().getExtras().get("click_location");

        rootLayout = (FrameLayout) findViewById(R.id.call_fragment_container);
        if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            rootLayout.setVisibility(View.INVISIBLE);
            ViewTreeObserver viewTreeObserver = rootLayout.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        circularRevealActivity(clickLocation);
                        rootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        }

        setVolumeControlStream(AudioManager.STREAM_RING);

        registerSubscriptions();
    }

    public void circularRevealActivity(LocationJ maybeClickLocation) {
        int x = maybeClickLocation == null ? 0 : maybeClickLocation.x();
        int y = maybeClickLocation == null ? 0 : maybeClickLocation.y();
        int cx = x != 0 ? x : (rootLayout.getWidth() / 2);
        int cy = y != 0 ? y : (rootLayout.getHeight() / 2);

        int finalRadius = Math.max(rootLayout.getWidth(), rootLayout.getHeight());

        // create the animator for this view (the start radius is zero)
        Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, cx, cy, 0, finalRadius);
        circularReveal.setDuration(300);

        // make the view visible and start the animation
        rootLayout.setVisibility(View.VISIBLE);
        circularReveal.start();
    }

    private void registerSubscriptions() {
        compositeSubscription.add(
                call.endedObservable().asJavaObservable().observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler()).subscribe(o -> {onCallEnded();})
        );
        compositeSubscription.add(
            call.ringingObservable().asJavaObservable().distinctUntilChanged().subscribe(ringing -> {
                if ((Boolean) ringing && call.incoming()) {
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.add(R.id.call_fragment_container, IncomingCallFragmentJ.newInstance(call, activeKey));
                    fragmentTransaction.commit();
                } else {
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.replace(R.id.call_fragment_container, ActiveCallFragmentJ.newInstance(call, activeKey));
                    //fragmentTransaction.replace(R.id.call_fragment_container, ActiveCallFragment.newInstance(call, activeKey));
                    fragmentTransaction.commit();
                }
            })
        );
    }

    // Called when the call ends (both by the user and by friend)
    public void onCallEnded() {
        AntoxLog.debug("${this.getClass.getSimpleName} on call ended called", AntoxLog.DEFAULT_TAG());
        finish();
    }

    public void setupOnHold() {
        //N/A TODO
    }

    public void hideViewOnHold() {
        //N/A TODO
    }

    @Override
    public void onCallReplySelected(Option<String> maybeReply) {
        String reply = maybeReply.get();
        if(reply != null) {
            //FIXME when group calls are implemented
            MessageHelper.sendMessage(this, (FriendKey) activeKey, reply, ToxMessageType.NORMAL, Option.empty());
        } else {
            Intent intent = AntoxNotificationManager.createChatIntent(this, ConstantsJ.SWITCH_TO_FRIEND, ChatActivityJ.class, activeKey);
            startActivity(intent);
        }
        call.end(CallEndReason.Normal());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(ConstantsJ.END_CALL)) {
            call.end(CallEndReason.Normal());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(compositeSubscription != null) {
            compositeSubscription.unsubscribe();
        }
    }

    @Override
    public void onBackPressed() {
        if (call.active() && call.ringing()) {
            call.end(CallEndReason.Normal());
        }
        super.onBackPressed();
    }

}
