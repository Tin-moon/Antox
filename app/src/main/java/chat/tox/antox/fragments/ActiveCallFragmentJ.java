package chat.tox.antox.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import chat.tox.antox.R;
import chat.tox.antox.activities.ChatActivityJ;
import chat.tox.antox.av.AntoxCamera;
import chat.tox.antox.av.Call;
import chat.tox.antox.av.CameraDisplay;
import chat.tox.antox.av.CameraFacing;
import chat.tox.antox.av.CameraUtils;
import chat.tox.antox.av.SelfCallState;
import chat.tox.antox.av.VideoDisplay;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.Options;
import chat.tox.antox.utils.UiUtils;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.ContactKey;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func0;
import rx.functions.Func2;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.NewThreadScheduler;
import scala.Enumeration;
import scala.Option;
import scala.Tuple2;
import scala.Tuple3;
import scala.concurrent.duration.FiniteDuration;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class ActiveCallFragmentJ extends CommonCallFragmentJ {

    public static ActiveCallFragmentJ newInstance(Call call, ContactKey activeKey) {
        ActiveCallFragmentJ activeCallFragment = new ActiveCallFragmentJ();
        Bundle bundle = new Bundle();
        bundle.putInt(CommonCallFragmentJ.EXTRA_CALL_NUMBER, call.callNumber());
        bundle.putString(CommonCallFragmentJ.EXTRA_ACTIVE_KEY, activeKey.toString());
        bundle.putInt(CommonCallFragmentJ.EXTRA_FRAGMENT_LAYOUT, R.layout.fragment_call);
        activeCallFragment.setArguments(bundle);
        return activeCallFragment;
    }

    private View backgroundView;
    private Chronometer durationView;

    private View buttonsView;
    private List<FrameLayout> allButtons;

    private TextureView videoSurface;
    private VideoDisplay videoDisplay;

    private TextureView cameraPreviewSurface;
    private CameraDisplay maybeCameraDisplay;
    private ImageView cameraSwapView;

    private List<View> viewsHiddenOnFade;
    private long lastClickTime = System.currentTimeMillis();


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        backgroundView = rootView.findViewById(R.id.call_background);

        /* Set up the speaker/mic buttons */
        buttonsView = rootView.findViewById(R.id.call_buttons);

        FrameLayout micOn = (FrameLayout) rootView.findViewById(R.id.mic_on);
        FrameLayout micOff = (FrameLayout) rootView.findViewById(R.id.mic_off);
        FrameLayout speakerOn = (FrameLayout) rootView.findViewById(R.id.speaker_on);
        FrameLayout speakerOff = (FrameLayout) rootView.findViewById(R.id.speaker_off);
        FrameLayout loudspeakerOn = (FrameLayout) rootView.findViewById(R.id.speaker_loudspeaker);
        FrameLayout videoOn = (FrameLayout) rootView.findViewById(R.id.video_on);
        FrameLayout videoOff = (FrameLayout) rootView.findViewById(R.id.video_off);
        FrameLayout returnToChat = (FrameLayout) rootView.findViewById(R.id.return_to_chat);

        allButtons = Arrays.asList(micOn, micOff, speakerOn, speakerOff, loudspeakerOn, videoOn, videoOff, returnToChat);
        for(View v : allButtons) {
            v.setClickable(false);
        }

        setupOnClickToggle(micOn, () -> {
            call.muteSelfAudio();
            return null;
        });
        setupOnClickToggle(micOff, () -> {
            call.unmuteSelfAudio();
            return null;
        });
        setupOnClickToggle(loudspeakerOn, () -> {
            call.disableLoudspeaker();
            call.muteFriendAudio();
            return null;
        });
        setupOnClickToggle(speakerOff, () -> {
            call.disableLoudspeaker();
            call.unmuteFriendAudio();
            return null;
        });
        setupOnClickToggle(speakerOn, () -> {
            call.enableLoudspeaker();
            return null;
        });


        Subscription subscription = call.selfStateObservable().asJavaObservable().subscribe(selfState -> {
            if (selfState.audioMuted()) {
                UiUtilsJ.toggleViewVisibility(micOff, micOn);
            } else {
                UiUtilsJ.toggleViewVisibility(micOn, micOff);
            }

            if (selfState.loudspeakerEnabled()) {
                audioManager.setSpeakerphoneOn(true);
            } else {
                audioManager.setSpeakerphoneOn(false);
            }

            if (selfState.loudspeakerEnabled()) {
                List<View> views = Arrays.asList(speakerOn, speakerOff);
                UiUtilsJ.toggleViewVisibility(loudspeakerOn, views);
            } else if (selfState.receivingAudio()) {
                List<View> views = Arrays.asList(speakerOff, loudspeakerOn);
                UiUtilsJ.toggleViewVisibility(speakerOn, views);
            } else {
                List<View> views = Arrays.asList(loudspeakerOn, speakerOn);
                UiUtilsJ.toggleViewVisibility(speakerOff, views);
            }

            if (selfState.sendingVideo()) {
                UiUtilsJ.toggleViewVisibility(videoOn, videoOff);
            } else {
                UiUtilsJ.toggleViewVisibility(videoOff, videoOn);
            }

            if (selfState.videoHidden()) {
                UiUtilsJ.toggleViewVisibility(videoOff, videoOn);
            } else {
                UiUtilsJ.toggleViewVisibility(videoOn, videoOff);
            }
        });
        compositeSubscription.add(subscription);

        // don't let the user enable video if the device doesn't have a camera
        if (CameraUtils.deviceHasCamera(getActivity())) {
            setupOnClickToggle(videoOn, () -> {
                call.hideSelfVideo();
                return null;
            });
            setupOnClickToggle(videoOff, () -> {
                call.showSelfVideo();
                return null;
            });
        }

        setupOnClickToggle(returnToChat, () -> {
            Intent intent = new Intent(getActivity(), ChatActivityJ.class);
            intent.setAction(ConstantsJ.SWITCH_TO_FRIEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY); //stop chats from stacking up
            intent.putExtra("key", activeKey.toString());
            startActivity(intent);
            getActivity().finish();
            return null;
        });


        if (CameraUtils.deviceHasCamera(getActivity())) {
            if (Options.videoCallStartWithNoVideo()) {
                // start with video off!
                call.hideSelfVideo();
            }
        }

        durationView = (Chronometer) rootView.findViewById(R.id.call_duration);
        videoSurface = (TextureView) rootView.findViewById(R.id.video_surface);

        @SuppressLint("ClickableViewAccessibility")
        FrameLayout cameraPreviewWrapper = (FrameLayout) rootView.findViewById(R.id.camera_preview_wrapper);
        cameraPreviewWrapper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
                if (view.getId() != R.id.camera_preview_wrapper) {
                    return false;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE | MotionEvent.ACTION_UP:
                        int newX = (int) (event.getRawX() - (view.getWidth() / 2));
                        int newY = (int) (event.getRawY() - view.getHeight());

                        params.leftMargin = newX;
                        params.topMargin = newY;
                        view.setLayoutParams(params);
                        break;
                    case MotionEvent.ACTION_DOWN:
                        view.setLayoutParams(params);
                        break;
                    default: // do nothing
                        break;
                }
                return true;
            }
        });

        cameraPreviewSurface = (TextureView) rootView.findViewById(R.id.camera_preview_surface);
        scaleSurfaceRelativeToScreen(cameraPreviewSurface, 0.3f);

        videoDisplay = new VideoDisplay(getActivity(), call.videoFrameObservable(), videoSurface, call.videoBufferLength());
        maybeCameraDisplay = new CameraDisplay(getActivity(), cameraPreviewSurface, cameraPreviewWrapper, buttonsView.getHeight());

        cameraSwapView = (ImageView) rootView.findViewById(R.id.swap_camera);
        cameraSwapView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                call.rotateCamera();
            }
        });




        Subscription facingSubscription = call.cameraFacingObservable().asJavaObservable().subscribe(o -> {
            if(o.equals(CameraFacing.Front())) {
                cameraSwapView.setImageResource(R.drawable.ic_camera_front_white_24dp);
            } else if(o.equals(CameraFacing.Back())) {
                cameraSwapView.setImageResource(R.drawable.ic_camera_rear_white_24dp);
            }
        });
        compositeSubscription.add(facingSubscription);



        Observable ringingObservable = call.ringingObservable().distinctUntilChanged().asJavaObservable();
        ringingObservable = ringingObservable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        Subscription ringingSubscription = ringingObservable.subscribe(ringing -> {
            if (call.active()) {
                if (ringing != null && (Boolean) ringing) {
                    setupOutgoing();
                } else {
                    setupActive();
                }
            }
        });
        compositeSubscription.add(ringingSubscription);


        Observable ringingObservable2 = call.ringingObservable().combineLatest(call.selfStateObservable()).asJavaObservable();
        ringingObservable2 = ringingObservable2.map(o -> {
            Tuple2<Object, SelfCallState> t = (Tuple2<Object, SelfCallState>) o; // boolean, SelfCallState
            return new Tuple3<Object, Object, Object>(t._1(), t._2().receivingVideo(), t._2().sendingVideo()); //boolean, boolean, boolean
        });
        ringingObservable2 = ringingObservable2.distinctUntilChanged();
        ringingObservable2 = ringingObservable2.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        Subscription ringingSubscription2 = ringingObservable2.subscribe(o -> {
            Tuple3<Boolean, Boolean, Boolean> data = (Tuple3<Boolean, Boolean, Boolean>) o;
            boolean ringing = data._1();
            boolean receivingVideo = data._2();
            boolean sendingVideo = data._3();
            if (call.active() && !call.ringing()) {
                setupIncomingVideoUi(receivingVideo, sendingVideo);
            }
        });
        compositeSubscription.add(ringingSubscription2);


        Observable ringingObservable3 = call.ringingObservable().combineLatest(call.selfStateObservable()).asJavaObservable();
        ringingObservable3 = ringingObservable3.map(o -> {
            Tuple2<Object, SelfCallState> t = (Tuple2<Object, SelfCallState>) o; // boolean, SelfCallState
            return new Tuple2<Object, Object>(t._1(), t._2().sendingVideo()); // boolean, boolean
        });
        ringingObservable3 = rx.Observable.combineLatest(ringingObservable3, call.cameraFacingObservable().asJavaObservable(), new Func2<Tuple2, Enumeration.Value, Tuple3>() {
            @Override
            public Tuple3 call(Tuple2 t, Enumeration.Value facing) {
                return new Tuple3(t._1(), t._2(), facing);
            }
        });
        ringingObservable3 = ringingObservable3.distinctUntilChanged();
        ringingObservable3 = ringingObservable3.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        Subscription ringingSubscription3 = ringingObservable3.subscribe(o -> {
            Tuple3<Boolean, Boolean, Enumeration.Value> data = (Tuple3<Boolean, Boolean, Enumeration.Value>) o;
            //Tuple3<Object, SelfCallState, Enumeration.Value> data = (Tuple3<Object, SelfCallState, Enumeration.Value>) o;
            boolean ringing = (boolean) data._1();
            //SelfCallState state = data._2();
            //boolean sendingVideo = state.sendingVideo();

            boolean sendingVideo = data._2();
            Enumeration.Value facing = data._3();
            if (call.active() && !call.ringing()) {
                setupOutgoingVideoUi(sendingVideo, facing);
            }
        });
        compositeSubscription.add(ringingSubscription3);

        viewsHiddenOnFade = Arrays.asList(buttonsView, upperCallHalfView);
        return rootView;
    }

    /**
     * Scales a [[TextureView]] to a size relative to that of the screen.
     *
     * @param textureView [[TextureView]] to be scaled
     * @param scale       amount to multiply the width and height of the screen by to get the new size
     */
    private void scaleSurfaceRelativeToScreen(TextureView textureView, Float scale)  {
        ViewGroup.LayoutParams layoutParams = textureView.getLayoutParams();
        layoutParams.width = (int) (UiUtilsJ.getScreenWidth(getActivity()) * scale);
        layoutParams.height = (int) (UiUtilsJ.getScreenHeight(getActivity()) * scale);
        textureView.setLayoutParams(layoutParams);
    }

    private void setupOnClickToggle(View clickView, Func0 action) {
        clickView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (call.active() && !call.ringing()) {
                    action.call();
                }
            }
        });
    }

    public void setupOutgoing() {
        callStateView.setVisibility(View.VISIBLE);
        callStateView.setText(R.string.call_ringing);
        durationView.setVisibility(View.GONE);
    }

    public void setupActive() {
        for (View v : allButtons) {
            v.setClickable(true);
        }
        callStateView.setVisibility(View.GONE);
        durationView.setVisibility(View.VISIBLE);
        durationView.setBase(SystemClock.elapsedRealtime() - call.duration().toMillis());
        durationView.start();
    }

    private void setupIncomingVideoUi(boolean receivingVideo, boolean sendingVideo) {
        boolean videoEnabled = receivingVideo || sendingVideo;
        if (videoEnabled) {
            backgroundView.setBackgroundColor(getResources().getColor(R.color.black_absolute));

            avatarView.setVisibility(View.GONE);
            nameView.setTextColor(getResources().getColor(R.color.white));
            durationView.setTextColor(getResources().getColor(R.color.white));

            // turn on loudspeaker in a video call
            if (!audioManager.isWiredHeadsetOn()){
                call.enableLoudspeaker();
            }
            if(videoDisplay != null) {
                videoDisplay.start();
            }

            // fade out when the video view hasn't been clicked in a while
            startUiFadeTimer();
            videoSurface.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    lastClickTime = System.currentTimeMillis();
                    for (View view : viewsHiddenOnFade) {
                        view.setVisibility(View.VISIBLE);
                        view.clearAnimation();
                        view.animate().cancel();
                    }
                    startUiFadeTimer();
                }
            });
        } else if (!videoEnabled) {
            backgroundView.setBackgroundColor(getResources().getColor(R.color.white));
            avatarView.setVisibility(View.VISIBLE);
            if(videoDisplay != null) {
                videoDisplay.stop();
            }
            nameView.setTextColor(getResources().getColor(R.color.grey_darkest));
            durationView.setTextColor(getResources().getColor(R.color.black));
        }
    }

    private void setupOutgoingVideoUi(boolean sendingVideo, Enumeration.Value cameraFacing/*CameraFacing cameraFacing*/) {
        AntoxLog.debug("stopping some video stuff", AntoxLog.DEFAULT_TAG());

        if (sendingVideo) {
            // get preferred camera or default camera or exit and disable video on failure
            // (there will always be some camera or sendingVideo would be false)
            Option<AntoxCamera> camera = CameraUtils.getCameraInstance(cameraFacing/*.Front()*/);
            AntoxCamera camera1 = null;
            if(camera.isDefined() && camera.get() != null) {
                camera1 = camera.get();
            }
            if(camera1 == null) {
                camera = CameraUtils.getCameraInstance(CameraFacing.Back());
            }
            if(camera.isDefined() && camera.get() != null) {
                camera1 = camera.get();
            }
            if(camera1 == null) {
                call.hideSelfVideo();
                AntoxLog.debug("hiding self video because camera could not be accessed", AntoxLog.DEFAULT_TAG());
                return;
            }


            CameraUtils.setCameraDisplayOrientation(getActivity(), camera.get());
            cameraPreviewSurface.setVisibility(View.VISIBLE);
            if(maybeCameraDisplay != null) {
                call.cameraFrameBuffer_$eq(Option.apply(maybeCameraDisplay.frameBuffer()));
                maybeCameraDisplay.start(camera.get());
            }
        } else {
            AntoxLog.debug("stopping video stuff", AntoxLog.DEFAULT_TAG());
            if(maybeCameraDisplay != null) {
                call.cameraFrameBuffer_$eq(Option.empty());
                maybeCameraDisplay.stop();
            }
            cameraPreviewSurface.setVisibility(View.GONE);
        }
    }

    private void startUiFadeTimer() {
        Observable observable = rx.Observable.timer(4, TimeUnit.SECONDS).observeOn(NewThreadScheduler.apply().asJavaScheduler());
        observable = observable.flatMap(o -> {
            return call.videoEnabledObservable().asJavaObservable();
        });
        Subscription subscription = observable.subscribe(video -> {
            long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
            FiniteDuration fadeDelay = new FiniteDuration(4, TimeUnit.SECONDS);
            if (call.active() && !call.ringing() && (timeSinceLastClick >= fadeDelay.toMillis()) && (boolean) video) {
                AlphaAnimation fadeOut = new AlphaAnimation(1, 0);
                fadeOut.setInterpolator(new AccelerateInterpolator());
                fadeOut.setDuration(2500);
                fadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) { }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        for(View v : viewsHiddenOnFade) {
                            v.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) { }
                });

                for(View view : viewsHiddenOnFade) {
                    view.setAnimation(fadeOut);
                    view.animate();
                }
            }
        });
        compositeSubscription.add(subscription);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(videoDisplay != null) {
            videoDisplay.stop();
        }
        if(maybeCameraDisplay != null) {
            maybeCameraDisplay.stop();
        }

        call.cameraFrameBuffer_$eq(Option.empty());
        durationView.stop();
    }

}
