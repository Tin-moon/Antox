package chat.tox.antox.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Chronometer;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.data.AntoxDB;
import chat.tox.antox.data.State;
import chat.tox.antox.fragments.ContactItemType;
import chat.tox.antox.tox.ToxSingleton;
import chat.tox.antox.utils.AntoxLog;
import chat.tox.antox.utils.AntoxNotificationManager;
import chat.tox.antox.utils.BitmapManager;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.IconColor;
import chat.tox.antox.utils.LeftPaneItem;
import chat.tox.antox.utils.TimestampUtils;
import chat.tox.antox.utils.UiUtilsJ;
import chat.tox.antox.wrapper.ContactKey;
import chat.tox.antox.wrapper.FriendKey;
import chat.tox.antox.wrapper.GroupInvite;
import chat.tox.antox.wrapper.GroupKey;
import de.hdodenhof.circleimageview.CircleImageView;
import rx.Subscription;
import scala.Enumeration;
import scala.Option;
import scala.collection.JavaConversions;

/**
 * Created by Nechypurenko on 15.02.2018.
 */

public class ContactListAdapterJ extends BaseAdapter implements Filterable {

    private Context context;

    private List<LeftPaneItem> originalData= new ArrayList<LeftPaneItem>();
    private List<LeftPaneItem> data = new ArrayList<LeftPaneItem>();

    private final LayoutInflater layoutInflater;

    private Filter filter;

    public ContactListAdapterJ(Context context) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
    }

    public void addItem(LeftPaneItem item) {
        data.add(item);
        originalData.add(item);
        notifyDataSetChanged();
    }

    public void insert(int index, LeftPaneItem item) {
        data.add(null);
        data.set(index, item);
        originalData.add(null);
        originalData.set(index, item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        LeftPaneItem item = getItem(position);
        if(item != null) {
            Enumeration.Value type = item.viewType();
            return type.id();
        }
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return ContactItemType.values().iterator().size();
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public LeftPaneItem getItem(int position) {
        return data.get(position);
    }

    public ContactKey getKey(int position){
        return getItem(position).key();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        View newConvertView = convertView;
        Enumeration.Value type = ContactItemType.apply(getItemViewType(position));
        if (newConvertView == null) {
            holder = new ViewHolder();
            if(type.equals(ContactItemType.FRIEND_REQUEST()) | type.equals(ContactItemType.GROUP_INVITE())) {
                newConvertView = layoutInflater.inflate(R.layout.friendrequest_list_item, null);
                holder.firstText = (TextView) newConvertView.findViewById(R.id.request_key);
                holder.secondText = (TextView) newConvertView.findViewById(R.id.request_message);
            } else if(type.equals(ContactItemType.FRIEND()) | type.equals(ContactItemType.GROUP())) {
                newConvertView = layoutInflater.inflate(R.layout.contact_list_item, null);
                holder.firstText = (TextView) newConvertView.findViewById(R.id.contact_name);
                holder.secondText = (TextView) newConvertView.findViewById(R.id.contact_status);
                holder.secondImage = (ImageView) newConvertView.findViewById(R.id.second_image);
                holder.icon = (TextView) newConvertView.findViewById(R.id.icon);
                holder.favorite = (ImageView) newConvertView.findViewById(R.id.star);
                holder.avatar = (CircleImageView) newConvertView.findViewById(R.id.avatar);
                holder.countText = (TextView) newConvertView.findViewById(R.id.unread_messages_count);
                holder.timeText = (TextView) newConvertView.findViewById(R.id.last_message_timestamp);
                holder.chronometer = (Chronometer) newConvertView.findViewById(R.id.contact_item_chronometer);
            }
            newConvertView.setTag(holder);
        } else {
            holder = (ViewHolder) newConvertView.getTag();
        }

        LeftPaneItem item = getItem(position);
        holder.firstText.setText(item.first());
        holder.firstText.setTextColor(context.getColor(R.color.black));

        if (!item.second().equals("")) {
            holder.secondText.setText(item.second());
        } else {
            holder.firstText.setGravity(Gravity.CENTER_VERTICAL);
        }

        if(holder.secondImage != null) {
            if(!item.secondImage().isEmpty()) {
                holder.secondImage.setVisibility(View.VISIBLE);
                holder.secondImage.setImageResource((Integer) item.secondImage().get());
            } else {
                holder.secondImage.setVisibility(View.GONE);
            }
        }

        if(type.equals(ContactItemType.FRIEND()) | type.equals(ContactItemType.GROUP())) {
            if (item.count() > 0) {
                holder.countText.setVisibility(View.VISIBLE);
                //limit unread counter to 99
                holder.countText.setText(java.lang.Integer.toString((item.count() > ConstantsJ.UNREAD_COUNT_LIMIT) ? ConstantsJ.UNREAD_COUNT_LIMIT : item.count()));
            } else {
                holder.countText.setVisibility(View.GONE);
            }

            if (item.activeCall()) {
                holder.timeText.setVisibility(View.GONE);
                holder.chronometer.setVisibility(View.VISIBLE);
                holder.chronometer.setBase(SystemClock.elapsedRealtime() - item.timestamp().getTime());
                holder.chronometer.start();
            } else {
                holder.timeText.setText(TimestampUtils.prettyTimestamp(item.timestamp(), false));
            }

            if(holder.imageLoadingSubscription != null) {
                holder.imageLoadingSubscription.unsubscribe();
            }

            if(!item.image().isEmpty()) {
                File img = item.image().get();
                Option<Bitmap> bitmapOption = BitmapManager.getFromCache(true, img);
                if(!bitmapOption.isEmpty()) {
                    Bitmap bitmap = bitmapOption.get();
                    holder.avatar.setImageBitmap(bitmap);
                    holder.imageLoadingSubscription = null;
                } else {
                    holder.imageLoadingSubscription = BitmapManager.load(img, true).asJavaObservable().subscribe(bitmap -> {
                        holder.avatar.setImageBitmap(bitmap);
                    });
                }
            } else {
                holder.avatar.setImageResource(R.drawable.default_avatar);
                holder.imageLoadingSubscription = null;
            }

            Drawable drawable = context.getDrawable(IconColor.iconDrawable(item.isOnline(), item.status()));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holder.icon.setBackground(drawable);
            } else {
                holder.icon.setBackgroundDrawable(drawable);
            }
            if (item.favorite()) {
                holder.favorite.setVisibility(View.VISIBLE);
            } else {
                holder.favorite.setVisibility(View.GONE);
            }
        }

        if (holder.timeText != null) {
            holder.timeText.setTextColor(context.getColor(R.color.grey_dark));
        }

        ImageView acceptButton = (ImageView) newConvertView.findViewById(R.id.accept);
        ImageView rejectButton = (ImageView) newConvertView.findViewById(R.id.reject);

        if (type.equals(ContactItemType.FRIEND_REQUEST())) {
            createFriendRequestClickHandlers((FriendKey)item.key(), acceptButton, rejectButton);
        } else if (type.equals(ContactItemType.GROUP_INVITE())) {
            createGroupInviteClickHandlers((GroupKey)item.key(), acceptButton, rejectButton);
        }

        return newConvertView;
    }

    private void createFriendRequestClickHandlers(FriendKey key, ImageView acceptButton, ImageView rejectButton) {
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AntoxLog.debug("Accepting Friend: " + key, AntoxLog.CLICK_TAG());
                AntoxDB db = State.db();
                db.addFriend(key, "", "", context.getString(R.string.friend_accepted_default_status));
                db.deleteFriendRequest(key);
                AntoxNotificationManager.clearRequestNotification(key);
                try {
                    ToxSingleton.tox().addFriendNoRequest(key);
                    ToxSingleton.save();
                } catch (Exception e) {
                }
            }
        });

        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AntoxLog.debug("Rejecting Friend: " + key, AntoxLog.CLICK_TAG());
                AntoxDB db = State.db();
                db.deleteFriendRequest(key);
            }
        });
    }

    private void createGroupInviteClickHandlers(GroupKey groupKey, ImageView acceptButton, ImageView rejectButton) {
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AntoxLog.debug("Joining Group: " + groupKey, AntoxLog.CLICK_TAG());
                AntoxDB db = State.db();
                db.groupInvites().first().asJavaObservable().subscribe(invites -> {
                    try {
                        List<GroupInvite> allData = JavaConversions.seqAsJavaList(invites);
                        byte[] inviteData = null;
                        for(GroupInvite gi : allData) {
                            ContactKey key = gi.groupKey();
                            if(key.equals(groupKey)) {
                                inviteData = gi.data();
                                break;
                            }
                        }
                        ToxSingleton.tox().acceptGroupInvite(inviteData);
                        ToxSingleton.save();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                });
                db.addGroup(groupKey, UiUtilsJ.trimId(groupKey), "");
                db.deleteGroupInvite(groupKey);
                AntoxNotificationManager.clearRequestNotification(groupKey);
            }
        });
        rejectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AntoxLog.debug("Joining Group: " + groupKey, AntoxLog.CLICK_TAG());
                AntoxDB db = State.db();
                db.deleteGroupInvite(groupKey);
            }
        });
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (originalData != null) {
                        if (constraint == null || constraint.equals("")) {
                            filterResults.values = originalData;
                            filterResults.count = originalData.size();
                        } else {
                            data = originalData;
                            List<LeftPaneItem> tempList1 = new ArrayList<LeftPaneItem>();
                            List<LeftPaneItem> tempList2 = new ArrayList<LeftPaneItem>();
                            int length = data.size();
                            int i = 0;
                            while (i < length) {
                                LeftPaneItem item = data.get(i);
                                if (item.first().toUpperCase().startsWith(constraint.toString().toUpperCase())){
                                    tempList1.add(item);
                                } else if (item.first().toLowerCase().contains(constraint.toString().toLowerCase())) {
                                    tempList2.add(item);
                                }
                                i += 1;
                            }
                            tempList1.addAll(tempList2);
                            filterResults.values = tempList1;
                            filterResults.count = tempList1.size();
                        }
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    data = (ArrayList<LeftPaneItem>) results.values;
                    if (results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
        }
        return filter;
    }

    private class ViewHolder {
        TextView firstText;
        TextView secondText;
        ImageView secondImage;
        TextView icon;
        ImageView favorite;
        CircleImageView avatar;
        TextView countText;
        TextView timeText;
        Chronometer chronometer;
        Subscription imageLoadingSubscription;
    }

}
