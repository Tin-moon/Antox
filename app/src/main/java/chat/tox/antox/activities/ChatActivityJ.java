package chat.tox.antox.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.av.Call;
import chat.tox.antox.av.CallEndReason;
import chat.tox.antox.av.CameraUtils;
import chat.tox.antox.data.State;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.tox.MessageHelper;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.IconColor;
import chat.tox.antox.utils.LocationJ;
import chat.tox.antox.utils.ViewExtensionsJ;
import chat.tox.antox.wrapper.CallNumber;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.FileKind;
import chat.tox.antox.wrapper.FriendInfo;
import chat.tox.antox.wrapper.FriendKey;
import chat.tox.antox.wrapper.UserStatus;
import de.hdodenhof.circleimageview.CircleImageView;
import im.tox.tox4j.core.data.ToxFileId;
import im.tox.tox4j.core.data.ToxNickname;
import im.tox.tox4j.core.enums.ToxMessageType;
import rx.Observable;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Option;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Created by Nechypurenko on 13.02.2018.
 */

public class ChatActivityJ extends GenericChatActivityJ<FriendKey> {

    private String photoPathSaveKey = "PHOTO_PATH";
    private String photoPath = null;

    private RelativeLayout activeCallBarView;
    private RelativeLayout activeCallBarClickable;
    private Subscription activeCallBarClickSubscription;
    private Subscription activeCallSubscription;
    private Subscription activeGroupInfoListSubscription;

    @Override
    public FriendKey getKey(String key) {
        return new FriendKey(key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
        ThemeManagerJ.applyTheme(this, bar);

        //findViewById(R.id.info).setVisibility(View.GONE)
        /* Set up on click actions for attachment buttons. Could possible just add onClick to the XML?? */
        ImageView attachmentButton = (ImageView) findViewById(R.id.attachment_button);
        ImageView cameraButton = (ImageView) findViewById(R.id.camera_button);
        ImageView imageButton = (ImageView) findViewById(R.id.image_button);

        activeCallBarView = (RelativeLayout) findViewById(R.id.call_bar_wrap);
        activeCallBarView.setVisibility(View.GONE);

        activeCallBarClickable = (RelativeLayout) findViewById(R.id.call_bar_clickable);

        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendInfo friendInfo = State.db().getFriendInfo(activeKey());
                if (!friendInfo.online()) {
                    Toast.makeText(ChatActivityJ.this, getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show();
                    return;
                }

                File path = Environment.getExternalStorageDirectory();
                DialogProperties properties = new DialogProperties();
                properties.selection_mode = DialogConfigs.SINGLE_MODE;
                properties.selection_type = DialogConfigs.FILE_SELECT;
                properties.root = path;
                properties.error_dir = path;
                properties.extensions = null;
                FilePickerDialog dialog = new FilePickerDialog(ChatActivityJ.this, properties);
                dialog.setTitle(R.string.select_file);

                dialog.setDialogSelectionListener(new DialogSelectionListener() {
                    @Override
                    public void onSelectedFilePaths(String[] files) {
                        // files is the array of the paths of files selected by the Application User.
                        // since we only want single file selection, use the first entry
                        if (files != null) {
                            if (files.length > 0) {
                                if (files[0] != null) {
                                    if (files[0].length() > 0) {
                                        String filePath = new File(files[0]).getAbsolutePath();
                                        State.transfers().sendFileSendRequest(filePath, activeKey(), FileKind.DATA$.MODULE$, ToxFileId.empty(), ChatActivityJ.this);
                                    }
                                }
                            }
                        }
                    }
                });

                dialog.show();
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendInfo friendInfo = State.db().getFriendInfo(activeKey());
                if (!friendInfo.online()) {
                    Toast.makeText(ChatActivityJ.this, getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                String image_name = "Antoxpic " + new SimpleDateFormat("hhmm").format(new Date()) + " ";

                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                try {
                    storageDir.mkdirs();
                    File file = File.createTempFile(image_name, ".jpg", storageDir);
                    Uri imageUri = Uri.fromFile(file);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    photoPath = file.getAbsolutePath();
                    startActivityForResult(cameraIntent, ConstantsJ.PHOTO_RESULT);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FriendInfo friendInfo = State.db().getFriendInfo(activeKey());
                if (!friendInfo.online()) {
                    Toast.makeText(ChatActivityJ.this, getString(R.string.chat_ft_failed_friend_offline), Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, ConstantsJ.IMAGE_RESULT);
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the photo path to prevent it being lost on rotation
        outState.putString(photoPathSaveKey, photoPath == null ? "" : photoPath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        photoPath = savedInstanceState.getString(photoPathSaveKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        AntoxNotificationManager.clearMessageNotification(activeKey());

        Observable observable = State.db().friendInfoList().asJavaObservable();
        observable = observable.subscribeOn(IOScheduler.apply().asJavaScheduler());
        observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        activeGroupInfoListSubscription = observable.subscribe(o -> {
            Seq<FriendInfo> data = (Seq<FriendInfo>) o;
            List<FriendInfo> allCalls = JavaConversions.seqAsJavaList(data);
            updateDisplayedState(allCalls);
        });

        Observable observableCall = State.callManager().activeCallObservable().asJavaObservable();
        observableCall = observableCall.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        activeCallSubscription = observableCall.subscribe(o -> {
            Call activeAssociatedCall = null;
            Iterable<Call> activeCalls = JavaConversions.asJavaIterable((scala.collection.Iterable<Call>) o);
            for (Call call : activeCalls) {
                if (call.contactKey().equals(activeKey())) {
                    activeAssociatedCall = call;
                    break;
                }
            }

            if(activeAssociatedCall != null) {
                final Call call = activeAssociatedCall;
                if(!call.ringing()) {
                    activeCallBarView.setVisibility(View.VISIBLE);
                    activeCallBarClickable.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            startCallActivity(call, ViewExtensionsJ.getCenterLocationOnScreen(v));
                        }
                    });
                    Chronometer chronometer = (Chronometer) findViewById(R.id.call_bar_chronometer);
                    chronometer.setBase(SystemClock.elapsedRealtime() - call.duration().toMillis());
                    chronometer.start();
                }
            } else {
                activeCallBarClickable.setOnClickListener(null);
                activeCallBarView.setVisibility(View.GONE);
            }
        });
    }

    private void updateDisplayedState(List<FriendInfo> allCalls) {
        ContactKey key = activeKey();
        FriendInfo mFriend = null;
        for(FriendInfo fi : allCalls) {
            if(fi.key().equals(key)) {
                mFriend = fi;
                break;
            }
        }

        final String displayName;
        if(mFriend != null) {
            displayName = mFriend.getDisplayName();
            Option<File> avatarOption = mFriend.avatar();
            if(!avatarOption.isEmpty()) {
                File avatar = avatarOption.get();
                if(avatar != null) {
                    final CircleImageView avatarHolder = (CircleImageView) findViewById(R.id.chat_avatar);
                    BitmapManager.load(avatar, true).asJavaObservable().forEach(o -> avatarHolder.setImageBitmap(o));
                }
            }
            FriendInfo finalMFriend = mFriend;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        statusIconView().setBackground(getDrawable(IconColor.iconDrawable(finalMFriend.online(), UserStatus.getToxUserStatusFromString(finalMFriend.status()))));
                    } else {
                        statusIconView().setBackgroundDrawable(getDrawable(IconColor.iconDrawable(finalMFriend.online(), UserStatus.getToxUserStatusFromString(finalMFriend.status()))));
                    }
                }
            });
        } else {
            displayName = "";
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setDisplayName(displayName);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ConstantsJ.IMAGE_RESULT) {
                Uri uri = data.getData();
                String[] filePathColumn = new String[]{MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.DISPLAY_NAME};
                CursorLoader loader = new CursorLoader(this, uri, filePathColumn, null, null, null);
                Cursor cursor = loader.loadInBackground();
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        int columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0]);
                        String filePath = cursor.getString(columnIndex);
                        int fileNameIndex = cursor.getColumnIndexOrThrow(filePathColumn[1]);
                        String fileName = cursor.getString(fileNameIndex);
                        try {
                            State.transfers().sendFileSendRequest(filePath, this.activeKey(), FileKind.DATA$.MODULE$, ToxFileId.empty(), this);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            if (requestCode == ConstantsJ.PHOTO_RESULT) {
                if(photoPath != null) {
                    State.transfers().sendFileSendRequest(photoPath, this.activeKey(), FileKind.DATA$.MODULE$, ToxFileId.empty(), this);
                    photoPath = null;
                }
            }
        } else {
            AntoxLog.debug("onActivityResult result code not okay, user cancelled", AntoxLog.DEFAULT_TAG());
        }
    }

    public void startCallActivity(Call call, LocationJ clickLocation) {
        Intent callActivity = new Intent(this, CallActivityJ.class);
        callActivity.putExtra("key", call.contactKey().toString());
        callActivity.putExtra("call_number", call.callNumber());
        callActivity.putExtra("click_location", clickLocation);
        startActivity(callActivity);
    }

    @Override
    public void onClickInfo(LocationJ clickLocation) {
        Intent intent = new Intent(this, FriendProfileActivityJ.class);
        FriendInfo friendInfo = State.db().getFriendInfo(activeKey());

        Option<File> fileOption = friendInfo.avatar();
        File f = null;
        if(!fileOption.isEmpty()) {
            f = fileOption.get();
        }
        intent.putExtra("key", activeKey().key());
        intent.putExtra("avatar", f);


        String name = null;
        Iterator iterator = JavaConversions.asJavaIterator(friendInfo.alias().productIterator());
        if(iterator != null) {
            while (iterator.hasNext()) {
                ToxNickname o = (ToxNickname) iterator.next();
                name = new String(o.value());
            }
        }
//        if(friendInfo.alias().isDefined() && friendInfo.alias().get() != null) { //TODO wrong cast
//            name = new String(friendInfo.alias().get()); //java.lang.ClassCastException: im.tox.tox4j.core.data.ToxNickname cannot be cast to byte[]
//        }
        if(name == null || name.equals("")) {
            name = new String(friendInfo.name());
        }
        intent.putExtra("name", name);
        startActivity(intent);
    }

    public void  onClickCall(boolean video, LocationJ clickLocation) {
        AntoxLog.debug("Calling friend", AntoxLog.DEFAULT_TAG());
        if (!State.db().getFriendInfo(activeKey()).online()) {
            AntoxLog.debug("Friend not online", AntoxLog.DEFAULT_TAG());
            return;
        }

        List<Call> activeCalls = new ArrayList<>();
        List<Call> allCalls = JavaConversions.seqAsJavaList(State.callManager().calls());
        for(Call c : allCalls) {
            if(c.active()) {
                activeCalls.add(c);
            }
        }

        //end all calls that are active that do not belong to this activity
        for(Call c : activeCalls) {
            if(c.contactKey().equals(activeKey())) {
                c.end(CallEndReason.Normal());
            }
        }

        Call associatedActiveCall = null;
        for(Call c : activeCalls) {
            if(c.active()) {
                associatedActiveCall = c;
                break;
            }
        }

        if(associatedActiveCall != null) {
            startCallActivity(associatedActiveCall, clickLocation);
        } else {
            Call call = new Call(CallNumber.fromFriendNumber(ToxSingleton.tox().getFriendNumber(activeKey())), activeKey(), false);
            State.callManager().add(call);
            call.startCall(true, video);
            startCallActivity(call, clickLocation);
        }
    }

    @Override
    public void onClickVoiceCall(LocationJ clickLocation) {
        onClickCall(false, clickLocation);
    }

    @Override
    public void onClickVideoCall(LocationJ clickLocation) {
        // don't send video if the device doesn't have a camera
        boolean sendingVideo = CameraUtils.deviceHasCamera(this);
        onClickCall(sendingVideo, clickLocation);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(activeGroupInfoListSubscription != null) {
            activeGroupInfoListSubscription.unsubscribe();
        }
        if(activeCallSubscription != null) {
            activeCallSubscription.unsubscribe();
        }
    }

    @Override
    public void sendMessage(String message, ToxMessageType messageType, Context context) {
        MessageHelper.sendMessage(this, activeKey(), message, messageType, Option.empty());
    }

    @Override
    public void setTyping(boolean typing) {
        try {
            ToxSingleton.tox().setTyping(activeKey(), typing);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

}
