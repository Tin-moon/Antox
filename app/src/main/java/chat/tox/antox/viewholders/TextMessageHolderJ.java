package chat.tox.antox.viewholders;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.support.v7.app.AlertDialog;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.MessageType;
import rx.Observable;
import rx.Subscriber;
import rx.lang.scala.schedulers.IOScheduler;

/**
 * Created by Nechypurenko on 20.02.2018.
 */

public class TextMessageHolderJ extends GenericMessageHolderJ implements View.OnLongClickListener, View.OnTouchListener {

    protected TextView messageTitle;
    private boolean isLongClick = false;

    public TextMessageHolderJ(View view) {
        super(view);
        messageTitle = (TextView) view.findViewById(R.id.message_title);
    }

    @Override
    public boolean onLongClick(View v) {
        String[] items = new String[]{context().getResources().getString(R.string.message_copy), context().getResources().getString(R.string.message_delete)};
        isLongClick = true;
        new AlertDialog.Builder(context()).setCancelable(true).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        ClipboardManager clipboard = (ClipboardManager) context().getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText(null, msg().message()));
                        break;
                    case 1:
                        Observable.create(new Observable.OnSubscribe<Object>() {
                            @Override
                            public void call(Subscriber<? super Object> subscriber) {
                                AntoxDB db = State.db();
                                db.deleteMessage(msg().id());
                                subscriber.onCompleted();
                            }
                        }).subscribeOn(IOScheduler.apply().asJavaScheduler()).subscribe();
                        break;
                    default:
                        break;
                }
            }
        }).create().show();
        return true;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                for (View view : backgroundViews()) {
                    view.getBackground().setColorFilter(0x55000000, PorterDuff.Mode.SRC_ATOP);
                    view.invalidate();
                }
                break;
            case MotionEvent.ACTION_CANCEL | MotionEvent.ACTION_UP:
                for (View view : backgroundViews()) {
                    view.getBackground().clearColorFilter();
                    view.invalidate();
                }
                break;
            default: //do nothing
                break;
        }

        //bizarre workaround to prevent long pressing on a link causing the link to open on release
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isLongClick = false;
                return v.onTouchEvent(event);
            case MotionEvent.ACTION_UP:
                if (isLongClick) {
                    isLongClick = false;
                    return true; // if we're in a long click ignore the release action
                }
            default: //do nothing
                return v.onTouchEvent(event);
        }
    }

    public void setText(String s) {
        messageText().setText(s);
        messageText().setOnLongClickListener(this);
        messageText().setOnTouchListener(this);

        // Reset the visibility for non-group messages
        messageTitle.setVisibility(View.GONE);
        if (msg().isMine()) {
            if (shouldGreentext(s)) {
                messageText().setTextColor(context().getResources().getColor(R.color.green_light));
            } else {
                messageText().setTextColor(context().getResources().getColor(R.color.white));
            }
        } else {
            if (shouldGreentext(s)) {
                messageText().setTextColor(context().getResources().getColor(R.color.green));
            } else {
                messageText().setTextColor(context().getResources().getColor(R.color.black));
            }
            if (msg().type().equals(MessageType.GROUP_MESSAGE())) {
                groupMessage();
            }
        }
    }

    public void groupMessage() {
        messageText().setText(msg().message());
        messageTitle.setText(msg().senderName());
        toggleReceived();
        // generate name colour from hash to ensure names have consistent colours
        UiUtilsJ.generateColor(msg().senderName().hashCode());
        if (lastMsg() != null || msg().senderName().equals(lastMsg().senderName())) {
            messageTitle.setVisibility(View.VISIBLE);
        }
        messageTitle.setTextColor(UiUtilsJ.generateColor(msg().senderName().hashCode()));
        contactMessage();
    }

    private boolean shouldGreentext(String message) {
        return message.startsWith(">");
    }

}
