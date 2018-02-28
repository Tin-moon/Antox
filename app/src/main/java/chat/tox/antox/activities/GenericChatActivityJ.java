package chat.tox.antox.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.adapters.ChatMessagesAdapterJ;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.theme.ThemeManagerJ;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.KeyboardOptions;
import chat.tox.antox.utils.LocationJ;
import chat.tox.antox.utils.ViewExtensionsJ;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.Message;
import chat.tox.antox.wrapper.MessageType;
import im.tox.tox4j.core.enums.ToxMessageType;
import jp.wasabeef.recyclerview.animators.LandingAnimator;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Option;
import scala.collection.JavaConversions;

/**
 * Created by Nechypurenko on 14.02.2018.
 */

public abstract class GenericChatActivityJ<KeyType extends ContactKey> extends AppCompatActivity {

    //var ARG_CONTACT_NUMBER: String = "contact_number"
    private Toolbar toolbar;
    private ChatMessagesAdapterJ adapter;
    private EditText messageBox;
    private TextView isTypingBox;
    private TextView statusTextBox;
    private RecyclerView chatListView;
    private TextView displayNameView;
    private View statusIconView;
    private View avatarActionView;
    private Subscription messagesSub;
    private Subscription titleSub;
    private KeyType activeKey;
    private boolean scrolling = false;
    private LinearLayoutManager layoutManager = new LinearLayoutManager(this);

    private boolean fromNotifications = false;

    private int MESSAGE_LENGTH_LIMIT = ConstantsJ.MAX_MESSAGE_LENGTH * 64;

    private int defaultMessagePageSize = 50;
    private int numMessagesShown = defaultMessagePageSize;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
        setContentView(R.layout.activity_chat);

        toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        toolbar.inflateMenu(R.menu.chat_menu);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        setSupportActionBar(toolbar);

        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            ThemeManagerJ.applyTheme(this, bar);
            bar.setDisplayShowTitleEnabled(false);
        }

        Bundle extras = getIntent().getExtras();
        activeKey = getKey(extras.getString("key"));
        fromNotifications = extras.getBoolean("notification", false);

        AntoxLog.debug("key = " + activeKey, AntoxLog.DEFAULT_TAG());

        String action = getIntent().getAction();
        if (action != null && action.equals(ConstantsJ.START_CALL)) {
            onClickVoiceCall(LocationJ.Origin());
            finish();
            return;
        }

        AntoxDB db = State.db();
        adapter = new ChatMessagesAdapterJ(this, new ArrayList(getActiveMessageList(numMessagesShown)));

        displayNameView = (TextView) this.findViewById(R.id.displayName);
        statusIconView = this.findViewById(R.id.icon);
        avatarActionView = this.findViewById(R.id.avatarActionView);
        avatarActionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        layoutManager.setStackFromEnd(true);


        chatListView = (RecyclerView) this.findViewById(R.id.chat_messages);
        chatListView.setLayoutManager(layoutManager);
        chatListView.setAdapter(adapter);
        chatListView.setItemAnimator(new LandingAnimator());
        chatListView.setVerticalScrollBarEnabled(true);
        chatListView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(-1)) {
                    onScrolledToTop();
                }
            }

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                adapter.setScrolling(!(newState == RecyclerView.SCROLL_STATE_IDLE));
            }
        });

        ImageView sendMessageButton = (ImageView) this.findViewById(R.id.send_message_button);
        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendMessage();
                setTyping(false);
            }
        });

        messageBox = (EditText) this.findViewById(R.id.your_message);
        messageBox.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MESSAGE_LENGTH_LIMIT)});
        AntoxLog.error("=====================================================================", AntoxLog.DEFAULT_TAG());
        AntoxLog.error("activeKey="+activeKey, AntoxLog.DEFAULT_TAG());
        AntoxLog.error("db"+db, AntoxLog.DEFAULT_TAG());
        AntoxLog.error("=====================================================================", AntoxLog.DEFAULT_TAG());
        if(db == null) {
            AntoxLog.error("Error DB == NULL", AntoxLog.DEFAULT_TAG());
            finish();
            return;
        }
        String message = db.getContactUnsentMessage(activeKey);
        messageBox.setText(message);
        messageBox.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    onSendMessage();
                    setTyping(false);
                    return true;
                }
                return false;
            }
        });

        messageBox.setInputType(KeyboardOptions.getInputType(getApplicationContext()));
        messageBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                boolean isTyping = after > 0;
                setTyping(isTyping);
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                Observable.create(new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        db.updateContactUnsentMessage(activeKey, charSequence.toString());
                        subscriber.onCompleted();
                    }
                }).unsubscribeOn(IOScheduler.apply().asJavaScheduler()).subscribe();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        LocationJ clickLocation = null;
        View maybeItemView = toolbar.findViewById(item.getItemId());
        if (maybeItemView != null) {
            clickLocation = ViewExtensionsJ.getCenterLocationOnScreen(maybeItemView);
        } else {
            clickLocation = LocationJ.Origin();
        }
        switch (item.getItemId()) {
            case R.id.voice_call_button:
                onClickVoiceCall(clickLocation);
                return true;
            case R.id.video_call_button:
                onClickVideoCall(clickLocation);
                return true;
            case R.id.user_info:
                onClickInfo(clickLocation);
                return true;
            default:
                return false;
        }
    }

    public void setDisplayName(final String name) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayNameView.setText(name);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        State.activeKey().onNext(Option.apply(activeKey));
        State.chatActive().onNext(true);

        AntoxDB db = State.db();
        db.markIncomingMessagesRead(activeKey);

        messagesSub = getActiveMessagesUpdatedObservable()
                .observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler())
                .subscribe(o -> {
                    AntoxLog.debug("Messages updated", AntoxLog.DEFAULT_TAG());
                    updateChat(getActiveMessageList(numMessagesShown));
                });
    }

    public void updateChat(List<Message> messageList) {
        //FIXME make this more efficient
        adapter.removeAll();

        adapter.addAll(filterMessageList(messageList));

        // This works like TRANSCRIPT_MODE_NORMAL but for RecyclerView
        if (layoutManager.findLastCompletelyVisibleItemPosition() >= chatListView.getAdapter().getItemCount() - 2) {
            chatListView.smoothScrollToPosition(chatListView.getAdapter().getItemCount());
        }
        AntoxLog.debug("changing chat list cursor", AntoxLog.DEFAULT_TAG());
    }

    public List<Message> filterMessageList(List<Message> messageList) {
        boolean showCallEvents = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("call_event_logging", true);
        if (!showCallEvents) {
            List<Message> data = new ArrayList<>();
            for (Message m : messageList) {
                if (!m.type().equals(MessageType.CALL_EVENT())) {
                    data.add(m);
                }
            }
            return data;
        } else {
            return messageList;
        }
    }

    private String validateMessageBox() {
        return messageBox.getText().toString();
    }

    private void onScrolledToTop() {
        numMessagesShown += defaultMessagePageSize;
        Observable.create(new Observable.OnSubscribe<Object>() {
            @Override
            public void call(Subscriber<? super Object> subscriber) {
                subscriber.onNext(getActiveMessageList(numMessagesShown));
                subscriber.onCompleted();
            }
        }).subscribeOn(IOScheduler.apply().asJavaScheduler())
                .observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler())
                .subscribe(o -> {
                    updateChat((List<Message>) o);
                });
    }

    private void onSendMessage() {
        AntoxLog.debug("sendMessage", AntoxLog.DEFAULT_TAG());
        String mMessage = validateMessageBox();

        messageBox.setText("");
        String meMessagePrefix = "/me ";
        ToxMessageType messageType = mMessage.startsWith(meMessagePrefix) ? ToxMessageType.ACTION : ToxMessageType.NORMAL;

        String message = "";
        if (messageType.equals(ToxMessageType.ACTION)) {
            message = mMessage.replaceFirst(meMessagePrefix, "");
        } else {
            message = mMessage;
        }
        sendMessage(message, messageType, this);
    }


    private Observable getActiveMessagesUpdatedObservable() { //Observable[Int]
        AntoxDB db = State.db();
        return db.messageListUpdatedObservable(Option.apply(activeKey)).asJavaObservable();
    }

    private List<Message> getActiveMessageList(int takeLast) {
        AntoxDB db = State.db();
        List<Message> data;
        if(db != null) {
            data = JavaConversions.bufferAsJavaList(db.getMessageList(Option.apply(activeKey), takeLast));
        } else {
            data = new ArrayList<>();
        }
        return data;
    }

    @Override
    protected void onPause() {
        super.onPause();

        State.chatActive().onNext(false);
        if (isFinishing()) {
            overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_right);
        }
        if (messagesSub != null) {
            messagesSub.unsubscribe();
        }
    }

    @Override
    public void onBackPressed() {
        if (fromNotifications) {
            Intent main = new Intent(this, MainActivityJ.class);
            main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(main);
        } else {
            super.onBackPressed();
        }
    }

    //Abstract Methods
    public abstract KeyType getKey(String key);

    public abstract void sendMessage(String message, ToxMessageType messageType, Context context);

    public abstract void setTyping(boolean typing);

    public abstract void onClickVoiceCall(LocationJ clickLocation);

    public abstract void onClickVideoCall(LocationJ clickLocation);

    public abstract void onClickInfo(LocationJ clickLocation);

    public View statusIconView() {
        return statusIconView;
    }

    public KeyType activeKey() {
        return activeKey;
    }

    public void setTitleSub(Subscription titleSub) {
        this.titleSub = titleSub;
    }

}
