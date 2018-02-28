package chat.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.tox.MessageHelper;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.LocationJ;
import chat.tox.antox.wrapper.GroupInfo;
import chat.tox.antox.wrapper.GroupKey;
import im.tox.tox4j.core.enums.ToxMessageType;
import rx.Observable;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Option;

/**
 * Created by Nechypurenko on 09.02.2018.
 */

public class GroupChatActivityJ extends GenericChatActivityJ<GroupKey> {

    private String photoPath = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        findViewById(R.id.info).setVisibility(View.GONE);
        findViewById(R.id.call).setVisibility(View.GONE);
        findViewById(R.id.video).setVisibility(View.GONE);
        statusIconView().setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        AntoxDB db = State.db();
        /*Subscription subscription = */
        Observable observable = db.groupInfoList().asJavaObservable();
        observable = observable.subscribeOn(IOScheduler.apply().asJavaScheduler());
        observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
        Subscription subscription = observable.subscribe(o -> {
            final GroupKey id = (GroupKey) activeKey();
            AntoxLog.error("O Class: " + o.getClass(), AntoxLog.DEFAULT_TAG());
            GroupInfo[] data = ((GroupInfo[])o);
            for(GroupInfo info : data) {
                if(info.key().equals(id)) {
                    String name = info.getDisplayName();
                    setDisplayName(name == null ? "" : name);
                }
            }
        });
        setTitleSub(subscription);
    }

    public void onClickVoiceCallFriend(View v) { }

    public void onClickVideoCallFriend(View v) { }

    public void onClickInfo(View v) {
        Intent profile = new Intent(this, GroupProfileActivityJ.class);
        profile.putExtra("key", activeKey().toString());
        startActivity(profile);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void sendMessage(String message, ToxMessageType messageType, Context context) {
        MessageHelper.sendGroupMessage(context, activeKey(), message, messageType, Option.empty());
    }

    @Override
    public GroupKey getKey(String key) {
        return new GroupKey(key);
    }

    @Override
    public void setTyping(boolean typing) {
        // not yet implemented in toxcore
    }

    @Override
    public void onClickVoiceCall(LocationJ clickLocation) { }

    @Override
    public void onClickVideoCall(LocationJ clickLocation) { }

    @Override
    public void onClickInfo(LocationJ clickLocation) {
        //TODO add a group profile activity
    }

}
