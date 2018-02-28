package chat.tox.antox.viewholders;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.utils.TimestampUtils;
import chat.tox.antox.wrapper.Message;

/**
 * Created by Nechypurenko on 20.02.2018.
 */

public abstract class GenericMessageHolderJ extends RecyclerView.ViewHolder {

    private final View v;

    private final LinearLayout row;
    private final LinearLayout bubble;
    private final LinearLayout background;
    private final TextView messageText;
    private final TextView time;
    private final View sentTriangle;
    private final View receivedTriangle;
    private Message msg;
    private Message lastMsg;
    private Message nextMsg;
    private final Context context;
    private final List<View> backgroundViews;
    private final int density;

    public GenericMessageHolderJ(View itemView) {
        super(itemView);
        this.v = itemView;
        this.row = (LinearLayout) v.findViewById(R.id.message_row_layout);
        this.bubble = (LinearLayout) v.findViewById(R.id.message_bubble);
        this.background = (LinearLayout) v.findViewById(R.id.message_text_background);
        this.messageText = (TextView) v.findViewById(R.id.message_text);
        this.time = (TextView) v.findViewById(R.id.message_text_date);
        this.sentTriangle = v.findViewById(R.id.sent_triangle);
        this.receivedTriangle = v.findViewById(R.id.received_triangle);
        this.context = v.getContext();
        this.backgroundViews = Arrays.asList(background, receivedTriangle, sentTriangle);
        this.density = (int)v.getContext().getResources().getDisplayMetrics().density;
    }

    public void setMessage(Message message, Message lastMsg, Message nextMsg) {
        this.msg = message;
        this.lastMsg = lastMsg;
        this.nextMsg = nextMsg;
    }

    public Message getMessage() {
        return this.msg;
    }

    public void setTimestamp() {
        int messageTimeSeparation = 60; // the amount of time between messages needed for them to show a timestamp
        boolean delayedMessage = false;
        boolean differentSender = false;
        if(nextMsg != null) {
            long time = (nextMsg.timestamp().getTime() - msg.timestamp().getTime()) / 1000;
            delayedMessage = time > (long)(messageTimeSeparation + 1);
            differentSender = !nextMsg.senderName().equals(msg.senderName());
        }
        if (nextMsg != null || differentSender || delayedMessage) {
            time.setText(TimestampUtils.prettyTimestamp(msg.timestamp(), true));
            time.setVisibility(View.VISIBLE);
        } else {
            time.setVisibility(View.GONE);
        }
    }

    public void ownMessage() {
        sentTriangle.setVisibility(View.VISIBLE);
        receivedTriangle.setVisibility(View.GONE);
        row.setGravity(Gravity.RIGHT);
        bubble.setPadding(48 * this.density, 0, 0, 0);
        toggleReceived();
        LayerDrawable drawable = (LayerDrawable)context.getResources().getDrawable(R.drawable.conversation_item_sent_shape);
        GradientDrawable shape = (GradientDrawable)drawable.findDrawableByLayerId(R.id.sent_shape);
        shape.setColor(ThemeManagerJ.primaryColor());
        LayerDrawable drawableTriangle = (LayerDrawable)context.getResources().getDrawable(R.drawable.conversation_item_sent_triangle_shape);
        RotateDrawable shapeRotateTriangle = (RotateDrawable)drawableTriangle.findDrawableByLayerId(R.id.sent_triangle_shape);
        GradientDrawable shapeTriangle = (GradientDrawable)shapeRotateTriangle.getDrawable();
        shapeTriangle.setColor(ThemeManagerJ.primaryColor());
        background.setBackgroundDrawable(drawable);
        sentTriangle.setBackgroundDrawable(shapeRotateTriangle);
    }

    public void toggleReceived() {
        if(msg.received()) {
            setAlphaCompat(bubble, 1.0F);
        } else {
            setAlphaCompat(bubble, 0.5F);
        }
    }

    public void setAlphaCompat(View view, float value) {
        if(Build.VERSION.SDK_INT >= 11) {
            view.setAlpha(value);
        }
    }

    public void contactMessage() {
        receivedTriangle.setVisibility(View.VISIBLE);
        sentTriangle.setVisibility(View.GONE);
        row.setGravity(3);
        bubble.setPadding(0, 0, 48 * density, 0);
        toggleReceived();
        background.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.conversation_item_received_shape));
    }

    public View v() {
        return v;
    }

    public LinearLayout row() {
        return row;
    }

    public LinearLayout bubble() {
        return bubble;
    }

    public LinearLayout background() {
        return background;
    }

    public TextView messageText() {
        return messageText;
    }

    public TextView time() {
        return time;
    }

    public View sentTriangle() {
        return sentTriangle;
    }

    public View receivedTriangle() {
        return receivedTriangle;
    }

    public Message msg() {
        return msg;
    }

    public Message lastMsg() {
        return lastMsg;
    }

    public Message nextMsg() {
        return nextMsg;
    }

    public Context context() {
        return context;
    }

    public List<View> backgroundViews() {
        return backgroundViews;
    }

    public int density() {
        return density;
    }

}
