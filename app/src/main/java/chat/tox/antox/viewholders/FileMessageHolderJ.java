package chat.tox.antox.viewholders;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.text.format.Formatter;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.scaloid.common.LoggerTag;

import java.io.File;
import java.util.concurrent.TimeUnit;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.wrapper.FriendKey;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Option;
import scala.Some;
import scala.Tuple2;


/**
 * Created by Nechypurenko on 20.02.2018.
 */

public class FileMessageHolderJ extends GenericMessageHolderJ implements View.OnClickListener, View.OnLongClickListener {

    private final LoggerTag TAG = new LoggerTag(this.getClass().getSimpleName());
    private final ImageView imageMessage;
    private final LinearLayout fileButtons;
    private final LinearLayout progressLayout;
    private final TextView messageTitle;
    private final TextView fileSize;
    private final TextView fileProgressText;
    private final ProgressBar fileProgressBar;
    private final ProgressBar imageLoading;
    private File file;
    private Subscription progressSub;
    private Subscription imageLoadingSub;

    public FileMessageHolderJ(View view) {
        super(view);

        this.imageMessage = (ImageView) view.findViewById(R.id.message_sent_photo);
        this.fileButtons = (LinearLayout) view.findViewById(R.id.file_buttons);
        this.progressLayout = (LinearLayout) view.findViewById(R.id.progress_layout);
        this.messageTitle = (TextView) view.findViewById(R.id.message_title);
        this.fileSize = (TextView) view.findViewById(R.id.file_size);
        this.fileProgressText = (TextView) view.findViewById(R.id.file_transfer_progress_text);
        this.fileProgressBar = (ProgressBar) view.findViewById(R.id.file_transfer_progress);
        this.imageLoading = (ProgressBar) view.findViewById(R.id.image_loading);
        this.imageLoadingSub = null;
    }

    public void render() {
        imageLoading.setVisibility(View.GONE);
    }

    public void setImage(File file) {
        this.file = file;
        // Start a loading indicator in case the bitmap needs to be loaded from disk
        imageMessage.setImageBitmap(null);
        imageLoading.setVisibility(View.VISIBLE);

        if (imageLoadingSub != null) {
            imageLoadingSub.unsubscribe();
        }

        Observable observable = BitmapManager.load(file, false).asJavaObservable();
        imageLoadingSub = observable.subscribe(image -> {
            imageLoading.setVisibility(View.GONE);
            imageMessage.setImageBitmap((Bitmap) image);
        });

        imageMessage.setOnClickListener(this);
        imageMessage.setOnLongClickListener(this);
        imageMessage.setVisibility(View.VISIBLE);

        //TODO would be better to find a way where we didn't have to toggle all these
        messageText().setVisibility(View.GONE);
        fileSize.setVisibility(View.GONE);
        progressLayout.setVisibility(View.GONE);
        fileButtons.setVisibility(View.GONE);
        messageTitle.setVisibility(View.GONE);
        messageText().setVisibility(View.GONE);
    }

    public void showFileButtons() {
        View accept = fileButtons.findViewById(R.id.file_accept_button);
        View reject = fileButtons.findViewById(R.id.file_reject_button);

        FriendKey key = (FriendKey) msg().key();
        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                State.transfers().acceptFile(key, msg().messageId(), context());
            }
        });
        reject.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                State.transfers().rejectFile(key, msg().messageId(), context());
            }
        });
        fileButtons.setVisibility(View.VISIBLE);
        fileSize.setText(Formatter.formatFileSize(context(), msg().size()));
        fileSize.setVisibility(View.VISIBLE);

        progressLayout.setVisibility(View.GONE);
        imageMessage.setVisibility(View.GONE);
    }

    public void showProgressBar() {
        fileProgressBar.setMax(msg().size());
        fileProgressBar.setVisibility(View.VISIBLE);
        progressLayout.setVisibility(View.VISIBLE);

        if (progressSub == null || progressSub.isUnsubscribed()) {
            AntoxLog.debug("observer subscribing", TAG);
            progressSub = Observable.interval(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler())
                    .subscribe(aLong -> {
                        updateProgressBar();
                        State.setLastFileTransferAction();
                    });
        }

        imageMessage.setVisibility(View.GONE);
        fileButtons.setVisibility(View.GONE);
    }

    public void updateProgressBar() {
        int updateRate = 500;
        Option mProgress = State.transfers().getProgressSinceXAgo(msg().id(), updateRate);
        int bytesPerSecond = 0;
        if (mProgress instanceof Some) {
            Some some = (Some) mProgress;
            Tuple2 p = (Tuple2) some.x();
            bytesPerSecond = (int) (((long) p._1()) * 1000L / ((long) p._2()));
        } else {
            bytesPerSecond = 0;
        }

        if (bytesPerSecond != 0) {
            int secondsToComplete = msg().size() / bytesPerSecond;
            String res = String.format(context().getResources().getString(R.string.file_time_remaining), Integer.toString(secondsToComplete));
            fileProgressText.setText("" + (bytesPerSecond / 1024) + " KiB/s, " + res);
        } else {
            fileProgressText.setText("" + (bytesPerSecond / 1024) + " KiB/s");
        }
        fileProgressBar.setProgress((int) State.transfers().getProgress(msg().id()));
        if (fileProgressBar.getProgress() >= msg().size()) {
            progressSub.unsubscribe();
            AntoxLog.debug("observer unsubscribed", TAG);
        }
    }

    public void setProgressText(int resID) {
        fileProgressText.setText(context().getResources().getString(resID));
        fileProgressText.setVisibility(View.VISIBLE);

        bubble().setOnLongClickListener(this);
        progressLayout.setVisibility(View.VISIBLE);

        fileProgressBar.setVisibility(View.GONE);
        fileButtons.setVisibility(View.GONE);

        imageMessage.setVisibility(View.GONE);
    }

    public void setFileText(String text) {
        messageText().setText(text);
        if (msg().isMine()) {
            messageText().setTextColor(context().getResources().getColor(R.color.white));
        } else {
            messageText().setTextColor(context().getResources().getColor(R.color.black));
        }
        messageTitle.setVisibility(View.VISIBLE);
        messageText().setVisibility(View.VISIBLE);
    }

    @Override
    public void toggleReceived() {
        // do nothing
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ImageView) {
            Intent i = new Intent();
            i.setAction(android.content.Intent.ACTION_VIEW);
            i.setDataAndType(Uri.fromFile(file), "image/*");
            context().startActivity(i);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        String[] items = new String[]{
                context().getResources().getString(R.string.message_delete),
                context().getResources().getString(R.string.file_delete)
        };

        new AlertDialog.Builder(context()).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Observable.create(new Observable.OnSubscribe<Object>() {
                            @Override
                            public void call(Subscriber<? super Object> subscriber) {
                                AntoxDB db = State.db();
                                db.deleteMessage(msg().id());
                                subscriber.onCompleted();
                            }
                        }).subscribeOn(IOScheduler.apply().asJavaScheduler()).subscribe();
                        break;
                    case 1:
                        Observable.create(new Observable.OnSubscribe<Object>() {
                            @Override
                            public void call(Subscriber<? super Object> subscriber) {
                                AntoxDB db = State.db();
                                db.deleteMessage(msg().id());

                                File file = null;
                                if (msg().message().contains("/")) {
                                    file = new File(msg().message());
                                } else {
                                    File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ConstantsJ.DOWNLOAD_DIRECTORY);
                                    file = new File(f.getAbsolutePath() + "/" + msg().message());
                                }
                                file.delete();

                                subscriber.onCompleted();
                            }
                        }).subscribeOn(IOScheduler.apply().asJavaScheduler()).subscribe();
                        break;
                }
            }
        }).create().show();
        return true;
    }

}
