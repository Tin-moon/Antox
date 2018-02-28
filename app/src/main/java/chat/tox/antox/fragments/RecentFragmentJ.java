package chat.tox.antox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.adapters.ContactListAdapterJ;
import chat.tox.antox.av.Call;
import chat.tox.antox.data.CallEventKind;
import chat.tox.antox.utils.LeftPaneItem;
import chat.tox.antox.utils.TimestampUtils;
import chat.tox.antox.wrapper.ContactInfo;
import chat.tox.antox.wrapper.FriendInfo;
import chat.tox.antox.wrapper.FriendRequest;
import chat.tox.antox.wrapper.GroupInfo;
import chat.tox.antox.wrapper.GroupInvite;
import chat.tox.antox.wrapper.Message;
import chat.tox.antox.wrapper.UserStatus;
import scala.Enumeration;
import scala.Option;
import scala.Tuple4;
import scala.collection.Iterable;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class RecentFragmentJ extends AbstractContactsFragmentJ {

    public RecentFragmentJ() {
        super(false, false);
    }

    @Override
    public void updateContacts(Tuple4<Seq<FriendInfo>, Seq<FriendRequest>, Seq<GroupInvite>, Seq<GroupInfo>> contactInfoTuple, Iterable<Call> activeCalls) {

        List<FriendInfo> friendsList = JavaConversions.seqAsJavaList(contactInfoTuple._1());
        //List<FriendRequest> friendRequests = JavaConversions.seqAsJavaList(contactInfoTuple._2());
        //List<GroupInvite> groupInvites = JavaConversions.seqAsJavaList(contactInfoTuple._3());
        List<GroupInfo> groupList = JavaConversions.seqAsJavaList(contactInfoTuple._4());

        List<ContactInfo> contactList = new ArrayList<>(friendsList);
        contactList.addAll(groupList);

        leftPaneAdapter = new ContactListAdapterJ(getActivity());
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateContactsLists(leftPaneAdapter, contactList, activeCalls);
                contactsListView.setAdapter(leftPaneAdapter);
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        rootView.findViewById(R.id.center_text).setVisibility(View.VISIBLE);
        return rootView;
    }

    public void updateContactsLists(ContactListAdapterJ leftPaneAdapter, List<ContactInfo> contactList, Iterable<Call> activeCalls) {
        List<ContactInfo> sortedContactList = new ArrayList<>();
        for(ContactInfo c : contactList) {
            if(c.lastMessage().isDefined()) {
                sortedContactList.add(c);
            }
        }
        Collections.sort(sortedContactList, (o1, o2) -> {
            if(compareNames(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });

        Collections.sort(sortedContactList, (o1, o2) -> {
            if(compareLastMessageTimestamp(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });

        if (!sortedContactList.isEmpty()) {
            getView().findViewById(R.id.center_text).setVisibility(View.GONE);
            for (ContactInfo contact : sortedContactList) {
                Enumeration.Value itemType;
                if(contact instanceof GroupInfo) {
                    itemType = ContactItemType.GROUP();
                } else {
                    itemType = ContactItemType.FRIEND();
                }

                Call activeCall = null;
                Iterator<Call> iterator = JavaConversions.asJavaIterable(activeCalls).iterator();
                while (iterator.hasNext()) {
                    Call call = iterator.next();
                    if(call.contactKey().equals(contact.key())) {
                        if(call.ringing()) {
                            activeCall = call;
                            break;
                        }
                    }
                }

                Timestamp timestamp = null;
                String second = null;
                Option<Object> secondImage = null;
                boolean isDefined;
                if(activeCall != null) {
                    timestamp = new Timestamp(activeCall.duration().toMillis());
                    second = getString(R.string.call_ongoing);
                    secondImage = Option.apply(CallEventKind.Answered$.MODULE$.imageRes());
                    isDefined = true;
                } else {
                    Option<Message> message = contact.lastMessage();
                    timestamp = message.isDefined() ? message.get().timestamp() : TimestampUtils.emptyTimestamp();
                    second = message.isDefined() ? message.get().toNotificationFormat(getActivity()) : "";
                    secondImage = Option.apply(getSecondImage(contact));
                    isDefined = false;
                }

                //Message lastMessage = contact.lastMessage().get();
                LeftPaneItem contactPaneItem = new LeftPaneItem(
                        itemType,
                        contact.key(),
                        contact.avatar(),
                        contact.getDisplayName(),
                        second,
                        secondImage,
                        contact.online(),
                        UserStatus.getToxUserStatusFromString(contact.status()),
                        contact.favorite(),
                        contact.unreadCount(),
                        timestamp,
                        isDefined);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isDefined) {
                            leftPaneAdapter.insert(0, contactPaneItem); // active call goes to the top
                        } else {
                            leftPaneAdapter.addItem(contactPaneItem);
                        }
                    }
                });
            }
        } else {
            getView().findViewById(R.id.center_text).setVisibility(View.VISIBLE);
        }
    }

    public boolean compareLastMessageTimestamp(ContactInfo a, ContactInfo b) {
        return lastMessageTimstamp(a).after(lastMessageTimstamp(b));
    }

    private Timestamp lastMessageTimstamp(ContactInfo info) {
        Option<Message> message = info.lastMessage();
        return message.isDefined() ? message.get().timestamp() : TimestampUtils.emptyTimestamp();
    }

}
