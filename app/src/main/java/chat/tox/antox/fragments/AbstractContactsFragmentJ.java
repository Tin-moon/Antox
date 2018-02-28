package chat.tox.antox.fragments;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Collator;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.activities.ChatActivityJ;
import chat.tox.antox.activities.FriendProfileActivityJ;
import chat.tox.antox.activities.GroupChatActivityJ;
import chat.tox.antox.adapters.ContactListAdapterJ;
import chat.tox.antox.av.Call;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.CallEventKind;
import chat.tox.antox.data.State;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.LeftPaneItem;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.ContactInfo;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.FriendInfo;
import chat.tox.antox.wrapper.FriendKey;
import chat.tox.antox.wrapper.FriendRequest;
import chat.tox.antox.wrapper.Group;
import chat.tox.antox.wrapper.GroupInfo;
import chat.tox.antox.wrapper.GroupInvite;
import chat.tox.antox.wrapper.GroupKey;
import chat.tox.antox.wrapper.Message;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import rx.lang.scala.schedulers.IOScheduler;
import scala.Enumeration;
import scala.Option;
import scala.Tuple2;
import scala.Tuple4;
import scala.collection.Iterable;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Created by Nechypurenko on 15.02.2018.
 */

public abstract class AbstractContactsFragmentJ extends Fragment implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    protected boolean showSearch;
    protected boolean showFab = true;

    protected ListView contactsListView;

    protected ContactListAdapterJ leftPaneAdapter;

    protected Subscription contactChangeSub;

    protected ContactKey activeKey;

    public AbstractContactsFragmentJ(boolean showSearch, boolean showFab) {
        this.showSearch = showSearch;
        this.showFab = showFab;
    }

    public abstract void updateContacts(Tuple4<Seq<FriendInfo>, Seq<FriendRequest>, Seq<GroupInvite>, Seq<GroupInfo>> var1, Iterable<Call> var2);

    @Override
    public void onResume() {
        super.onResume();
        AntoxDB db = State.db();
        if(db != null) {
            Observable observable = db.contactListElements().combineLatest(State.callManager().activeCallObservable()).asJavaObservable();
            observable = observable.observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler());
            contactChangeSub = observable.subscribe(tuple -> {
                Tuple2<Tuple4<Seq<FriendInfo>, Seq<FriendRequest>, Seq<GroupInvite>, Seq<GroupInfo>>, Iterable<Call>> data = (Tuple2<Tuple4<Seq<FriendInfo>, Seq<FriendRequest>, Seq<GroupInvite>, Seq<GroupInfo>>, Iterable<Call>>) tuple;
                updateContacts(data._1(), data._2());
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (contactChangeSub != null) {
            contactChangeSub.unsubscribe();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        contactsListView = (ListView) rootView.findViewById(R.id.contacts_list);
        contactsListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
        contactsListView.setOnItemClickListener(this);
        contactsListView.setOnItemLongClickListener(this);

        if (showFab) {
            FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
            try {
                @SuppressLint("ResourceType") XmlResourceParser parser = getResources().getXml(R.color.fab_colors_list);
                fab.setBackgroundTintList(ColorStateList.createFromXml(getResources(), parser));
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            rootView.findViewById(R.id.fab).setVisibility(View.VISIBLE);
        } else {
            rootView.findViewById(R.id.fab).setVisibility(View.GONE);
        }

        if (showSearch) {
            EditText search = (EditText) rootView.findViewById(R.id.searchBar);
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                    if (leftPaneAdapter != null) {
                        leftPaneAdapter.getFilter().filter(charSequence);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        } else {
            rootView.findViewById(R.id.contact_search_view).setVisibility(View.GONE);
        }

        rootView.findViewById(R.id.center_text).setVisibility(View.GONE);

        return rootView;
    }

    public int getSecondImage(ContactInfo contact) {
        Option<Message> messageOption = contact.lastMessage();
        if(messageOption.isDefined()) {
            Message m = messageOption.get();
            //int res = m.callEventKind().imageRes();
            if(!m.callEventKind().equals(CallEventKind.Invalid$.MODULE$)) {
                return m.callEventKind().imageRes();
            }
        }
        return 0;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LeftPaneItem item = (LeftPaneItem) ((Adapter) parent.getAdapter()).getItem(position);
        Enumeration.Value type = item.viewType();
        if (!type.equals(ContactItemType.FRIEND_REQUEST()) && (!type.equals(ContactItemType.GROUP_INVITE()))) {
            ContactKey key = item.key();
            ToxSingleton.changeActiveKey(key);
            Intent intent = null;
            if (type.equals(ContactItemType.FRIEND())) {
                intent = new Intent(getActivity(), ChatActivityJ.class);
            } else {
                intent = new Intent(getActivity(), GroupChatActivityJ.class);
            }
            intent.putExtra("key", key.toString());
            startActivity(intent);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        LeftPaneItem item = (LeftPaneItem) ((Adapter) parent.getAdapter()).getItem(position);
        createLeftPanePopup(item);
        return true;
    }

    private void createLeftPanePopup(LeftPaneItem parentItem) {
        final Enumeration.Value type = parentItem.viewType();
        String[] items;
        if (type.equals(ContactItemType.FRIEND())) {
            items = new String[]{
                    getString(R.string.friend_action_profile),
                    getString(R.string.friend_action_delete),
                    getString(R.string.friend_action_export_chat),
                    getString(R.string.friend_action_delete_chat)
            };
        } else if (type.equals(ContactItemType.GROUP())) {
            items = new String[]{
                    getString(R.string.group_action_delete)
            };
        } else if (type.equals(ContactItemType.FRIEND_REQUEST()) | type.equals(ContactItemType.GROUP_INVITE())) {
            items = new String[]{
                    getString(R.string.request_action_copy_id)
            };
        } else {
            items = new String[]{};
        }

        if (parentItem != null && items.length > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle);
            builder.setTitle(getString(R.string.contacts_actions_on) + " " + parentItem.first());
            builder.setCancelable(true);

            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (type.equals(ContactItemType.FRIEND())) {
                        FriendKey key = (FriendKey) parentItem.key();
                        switch (which) {
                            case 0:
                                Intent profile = new Intent(getActivity(), FriendProfileActivityJ.class);
                                profile.putExtra("key", key.toString());
                                profile.putExtra("avatar", parentItem.image().get());
                                profile.putExtra("name", parentItem.first());
                                startActivity(profile);
                                break;
                            case 1:
                                showDeleteFriendDialog(getActivity(), key);
                                break;
                            case 2:
                                exportChat(getActivity(), key);
                                break;
                            case 3:
                                showDeleteChatDialog(getActivity(), key);
                                break;
                        }
                    } else if (type.equals(ContactItemType.GROUP())) {
                        GroupKey key = (GroupKey) parentItem.key();
                        switch (which) {
                            case 0:
                                AntoxDB db = State.db();
                                db.deleteChatLogs(key);
                                db.deleteContact(key);
                                Group group = ToxSingleton.getGroupList().getGroup(key);
                                try {
                                    group.leave(getString(R.string.group_default_part_message));
                                } catch (Exception e) {
                                }
                                ToxSingleton.save();
                                break;
                        }
                    } else if (type.equals(ContactItemType.FRIEND_REQUEST()) | type.equals(ContactItemType.GROUP_INVITE())) {
                        ContactKey key = parentItem.key();
                        ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText(null, key.toString()));
                        Toast toast = Toast.makeText(getActivity(), getString(R.string.request_id_copied), Toast.LENGTH_SHORT);
                        toast.show();
                    } else {
                    }
                    dialog.cancel();
                }
            });
            builder.show();
        }
    }

    public void showDeleteFriendDialog(Context context, FriendKey friendKey) {
        View deleteFriendDialog = View.inflate(context, R.layout.dialog_delete_friend, null);
        CheckBox deleteLogsCheckboxView = (CheckBox) deleteFriendDialog.findViewById(R.id.deleteChatLogsCheckBox);
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setView(deleteFriendDialog).setCancelable(false);
        builder.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Observable.create(new Observable.OnSubscribe<Boolean>() {
                    @Override
                    public void call(Subscriber<? super Boolean> subscriber) {
                        AntoxDB db = State.db();
                        if (deleteLogsCheckboxView.isChecked()) {
                            db.deleteChatLogs(friendKey);
                        }
                        db.deleteContact(friendKey);
                        try {
                            ToxSingleton.tox().deleteFriend(friendKey);
                            ToxSingleton.save();
                        } catch (Exception e) {
                        }

                        Option<NotificationManager> manager = AntoxNotificationManager.mNotificationManager();
                        if (manager.isDefined()) {
                            manager.get().cancel(AntoxNotificationManager.generateNotificationId(friendKey));
                        }
                        subscriber.onCompleted();
                    }
                }).subscribeOn(IOScheduler.apply().asJavaScheduler()).subscribe();
            }
        });
        builder.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }


    public void exportChat(Context context, FriendKey friendKey) {
        File path = Environment.getExternalStorageDirectory();
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = path;
        properties.error_dir = path;
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(getActivity(), properties);
        dialog.setTitle(R.string.select_file);

        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files != null) {
                    if (files.length > 0) {
                        if (files[0] != null) {
                            if (files[0].length() > 0) {
                                File directory = new File(files[0]);
                                try {
                                    AntoxDB db = State.db();
                                    List<Message> messageList = JavaConversions.seqAsJavaList(db.getMessageList(Option.apply(friendKey), -1));
                                    String exportPath = directory.getPath() + "/" + db.getFriendInfo(friendKey).name() + "-" + UiUtilsJ.trimId(friendKey) + "-log.txt";
                                    PrintWriter log = new PrintWriter(new FileOutputStream(exportPath, false));
                                    for (Message message : messageList) {
                                        Option<String> formattedMessage = message.logFormat();
                                        if (formattedMessage.isDefined()) {
                                            log.print(formattedMessage.get() + '\n');
                                        }
                                    }
                                    log.close();
                                    Toast.makeText(context, getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show();
                                } catch (Exception e) {
                                    Toast.makeText(context, getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show();
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    try {
                        AntoxDB db = State.db();
                        List<Message> messageList = JavaConversions.seqAsJavaList(db.getMessageList(Option.apply(friendKey), -1));
                        String exportPath = path.getPath() + "/" + db.getFriendInfo(friendKey).name() + "-" + UiUtilsJ.trimId(friendKey) + "-log.txt";
                        PrintWriter log = new PrintWriter(new FileOutputStream(exportPath, false));
                        for (Message message : messageList) {
                            Option<String> formattedMessage = message.logFormat();
                            if (formattedMessage.isDefined()) {
                                log.print(formattedMessage.get() + '\n');
                            }
                        }
                        log.close();
                        Toast.makeText(context, getString(R.string.friend_action_chat_log_exported, exportPath), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(context, getString(R.string.friend_action_chat_log_export_failed), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                }
            }
        });
        dialog.show();


    }

    public void showDeleteChatDialog(Context context, ContactKey key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
        builder.setMessage(getString(R.string.friend_action_delete_chat_confirmation));
        builder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AntoxDB db = State.db();
                        db.deleteChatLogs(key);
                        Option<NotificationManager> manager = AntoxNotificationManager.mNotificationManager();
                        if (manager.isDefined()) {
                            manager.get().cancel(AntoxNotificationManager.generateNotificationId(key));
                        }
                    }
                });

        builder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    public boolean compareNames(ContactInfo a, ContactInfo b) {
        return Collator.getInstance().compare(a.getDisplayName().toLowerCase(), b.getDisplayName().toLowerCase()) < 0;
    }

    public boolean compareOnline(FriendInfo a, FriendInfo b) {
        return a.online() && !b.online();
    }

    public boolean compareFavorite(ContactInfo a, ContactInfo b) {
        return a.favorite() && !b.favorite();
    }

}
