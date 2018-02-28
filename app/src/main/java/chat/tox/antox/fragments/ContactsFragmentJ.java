package chat.tox.antox.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.adapters.ContactListAdapterJ;
import chat.tox.antox.av.Call;
import chat.tox.antox.utils.LeftPaneItem;
import chat.tox.antox.utils.TimestampUtils;
import chat.tox.antox.wrapper.FriendInfo;
import chat.tox.antox.wrapper.FriendRequest;
import chat.tox.antox.wrapper.GroupInfo;
import chat.tox.antox.wrapper.GroupInvite;
import im.tox.tox4j.core.enums.ToxUserStatus;
import scala.Option;
import scala.Tuple4;
import scala.collection.Iterable;
import scala.collection.JavaConversions;
import scala.collection.Seq;

/**
 * Created by Nechypurenko on 16.02.2018.
 */

public class ContactsFragmentJ extends AbstractContactsFragmentJ {

    public ContactsFragmentJ() {
        super(true, true);
    }

    @Override
    public void updateContacts(Tuple4<Seq<FriendInfo>, Seq<FriendRequest>, Seq<GroupInvite>, Seq<GroupInfo>> contactInfoTuple, Iterable<Call> activeCalls) {
        if(contactInfoTuple != null) {

            List<FriendInfo> friendsList = JavaConversions.seqAsJavaList(contactInfoTuple._1());
            List<FriendRequest> friendRequests = JavaConversions.seqAsJavaList(contactInfoTuple._2());
            List<GroupInvite> groupInvites = JavaConversions.seqAsJavaList(contactInfoTuple._3());
            List<GroupInfo> groupList = JavaConversions.seqAsJavaList(contactInfoTuple._4());

            leftPaneAdapter = new ContactListAdapterJ(getActivity());
            updateFriendsList(leftPaneAdapter, friendsList);
            updateFriendRequests(leftPaneAdapter, friendRequests);
            updateGroupInvites(leftPaneAdapter, groupInvites);
            updateGroupList(leftPaneAdapter, groupList);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    contactsListView.setAdapter(leftPaneAdapter);
                }
            });
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        rootView.findViewById(R.id.center_text).setVisibility(View.GONE);
        return rootView;
    }


    private void updateFriendsList(ContactListAdapterJ leftPaneAdapter, List<FriendInfo> friendsList) {
        List<FriendInfo> sortedFriendsList = new ArrayList<>(friendsList);
        Collections.sort(sortedFriendsList, new Comparator<FriendInfo>() {
            @Override
            public int compare(FriendInfo o1, FriendInfo o2) {
                if(compareNames(o1, o2)) {
                    return 1;
                } else {
                    return 0;
                }
            }
        });

        Collections.sort(sortedFriendsList, (o1, o2) -> {
            if(compareOnline(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });

        Collections.sort(sortedFriendsList, (o1, o2) -> {
            if(compareFavorite(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });

        if (!sortedFriendsList.isEmpty()) {
            for(FriendInfo friend : sortedFriendsList) {
                java.sql.Timestamp timestamp = friend.lastMessage().isDefined() ? friend.lastMessage().get().timestamp() : TimestampUtils.emptyTimestamp();
                LeftPaneItem friendPane = new LeftPaneItem(
                        ContactItemType.FRIEND(),
                        friend.key(),
                        friend.avatar(),
                        friend.getDisplayName(),
                        friend.statusMessage(),
                        Option.empty(),
                        friend.online(),
                        friend.getFriendStatusAsToxUserStatus(),
                        friend.favorite(),
                        friend.unreadCount(),
                        timestamp,
                        false);
                leftPaneAdapter.addItem(friendPane);
            }
        }
    }

    private void updateFriendRequests(ContactListAdapterJ leftPaneAdapter, List<FriendRequest> friendRequests) {
        if (!friendRequests.isEmpty()) {
            for (FriendRequest r : friendRequests) {
                LeftPaneItem request = new LeftPaneItem(ContactItemType.FRIEND_REQUEST(), r.requestKey(), r.requestMessage());
                leftPaneAdapter.insert(0, request); // insert friend requests at top of contact list
            }
        }
    }

    private void updateGroupInvites(ContactListAdapterJ leftPaneAdapter, List<GroupInvite> groupInvites) {
        if (!groupInvites.isEmpty()) {
            for (GroupInvite invite : groupInvites) {
                LeftPaneItem request = new LeftPaneItem(ContactItemType.GROUP_INVITE(), invite.groupKey(), getString(R.string.invited_by) + " " + invite.inviter());
                leftPaneAdapter.addItem(request);
            }
        }
    }

    private void updateGroupList(ContactListAdapterJ leftPaneAdapter, List<GroupInfo> groups) {
        List<GroupInfo> sortedGroupList = new ArrayList<>(groups);
        Collections.sort(sortedGroupList, (o1, o2) -> {
            if(compareNames(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });
        Collections.sort(sortedGroupList, (o1, o2) -> {
            if(compareFavorite(o1, o2)) {
                return 1;
            } else {
                return 0;
            }
        });
        if (!sortedGroupList.isEmpty()) {
            for (GroupInfo group : sortedGroupList) {
                java.sql.Timestamp timestamp = group.lastMessage().isDefined() ? group.lastMessage().get().timestamp() : TimestampUtils.emptyTimestamp();
                LeftPaneItem groupPane = new LeftPaneItem(
                        ContactItemType.GROUP(),
                        group.key(),
                        Option.empty(), //group.avatar(), //TODO
                        group.getDisplayName(),
                        group.topic(),
                        Option.empty(),
                        group.online(), ToxUserStatus.NONE,
                        group.favorite(),
                        group.unreadCount(),
                        timestamp,
                        false);
                leftPaneAdapter.addItem(groupPane);
            }
        }
    }

}
