package chat.tox.antox.adapters;

import android.content.Context;
import android.os.Environment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.utils.ConstantsJ;
import chat.tox.antox.utils.FileUtils;
import chat.tox.antox.utils.TimestampUtils;
import chat.tox.antox.viewholders.ActionMessageHolderJ;
import chat.tox.antox.viewholders.CallEventMessageHolderJ;
import chat.tox.antox.viewholders.FileMessageHolderJ;
import chat.tox.antox.viewholders.GenericMessageHolderJ;
import chat.tox.antox.viewholders.TextMessageHolderJ;
import chat.tox.antox.wrapper.Message;
import chat.tox.antox.wrapper.MessageType;
import scala.Enumeration;

/**
 * Created by Nechypurenko on 14.02.2018.
 */

public class ChatMessagesAdapterJ extends RecyclerView.Adapter<GenericMessageHolderJ> {

    private final int TEXT = 1;
    private final int ACTION = 2;
    private final int FILE = 3;
    private final int CALL_INFO = 4;

    private boolean scrolling = false;

    private Context context;
    private List<Message> data;

    public ChatMessagesAdapterJ(Context context, ArrayList<Message> data) {
        this.context = context;
        this.data = data;
    }

    public void add(Message msg) {
        data.add(msg);
        notifyDataSetChanged();
    }

    public void addAll(List<Message> list) {
        data.addAll(list);
        notifyDataSetChanged();
    }

    public void remove(Message msg) {
        data.remove(msg);
        notifyDataSetChanged();
    }

    public void removeAll() {
        data.clear();
        notifyDataSetChanged();
    }

    public void setScrolling(boolean scrolling) {
        this.scrolling = scrolling;
    }

    @Override
    public GenericMessageHolderJ onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch (viewType) {
            case TEXT: {
                View v = inflater.inflate(R.layout.chat_message_row_text, viewGroup, false);
                return new TextMessageHolderJ(v);
            }
            case ACTION: {
                View v = inflater.inflate(R.layout.chat_message_row_action, viewGroup, false);
                return new ActionMessageHolderJ(v);
            }
            case FILE:{
                View v = inflater.inflate(R.layout.chat_message_row_file, viewGroup, false);
                return new FileMessageHolderJ(v);
            }
            case CALL_INFO: {
                View v = inflater.inflate(R.layout.chat_message_row_call_event, viewGroup, false);
                return new CallEventMessageHolderJ(v);
            }
        }
        return null;
    }

    @Override
    public void onBindViewHolder(GenericMessageHolderJ holder, int pos) {
        Message msg = data.get(pos);
//        Option<Message> lastMsg = data.lift(pos - 1);
//        Option<Message> nextMsg = data.lift(pos + 1);

        Message lastMsg = null;
        Message nextMsg = null;
        try { lastMsg = data.get(pos - 1); } catch (IndexOutOfBoundsException e) { }
        try { nextMsg = data.get(pos + 1); } catch (IndexOutOfBoundsException e) { }

        holder.setMessage(msg, lastMsg, nextMsg);
        holder.setTimestamp();

        int viewType = getItemViewType(pos);
        switch (viewType) {
            case TEXT:
                if (holder.getMessage().isMine()) {
                    holder.ownMessage();
                } else {
                    holder.contactMessage();
                }
                TextMessageHolderJ textHolder = (TextMessageHolderJ) holder;
                textHolder.setText(msg.message());
                break;
            case ACTION:
                ActionMessageHolderJ actionHolder = (ActionMessageHolderJ) holder;
                actionHolder.setText(msg.senderName(), msg.message());
                break;
            case CALL_INFO:
                CallEventMessageHolderJ callEventHolder = (CallEventMessageHolderJ) holder;
                callEventHolder.setText(msg.message());
                callEventHolder.setPrefixedIcon(msg.callEventKind().imageRes());
                break;
            case FILE:
                FileMessageHolderJ fileHolder = (FileMessageHolderJ) holder;
                fileHolder.render();

                if (holder.getMessage().isMine()) {
                    holder.ownMessage();
                    // show only filename of file (remove path)
                    String[] split = msg.message().split("/");
                    fileHolder.setFileText(split[split.length - 1]);
                } else {
                    holder.contactMessage();
                    // when receiving file there is only filename, no path
                    fileHolder.setFileText(msg.message());
                }

                if (msg.sent()) {
                    if (msg.messageId() != -1) {
                        fileHolder.showProgressBar();
                    } else {
                        //FIXME this should be "Failed" - fix the DB bug
                        fileHolder.setProgressText(R.string.file_finished);
                    }
                } else {
                    if (msg.messageId() != -1) {
                        if (msg.isMine()) {
                            // fileHolder.setProgressText(R.string.file_request_sent) // this removes the progress bar!!
                        } else {
                            fileHolder.showFileButtons();
                        }
                    } else {
                        fileHolder.setProgressText(R.string.file_rejected);
                    }
                }

                if (msg.received() || msg.isMine()) {
                    File file = null;
                    if (msg.message().contains("/")) {
                        file = new File(msg.message());
                    } else {
                        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), ConstantsJ.DOWNLOAD_DIRECTORY);
                        file = new File(f.getAbsolutePath() + "/" + msg.message());
                    }

                    // val extension = getFileExtensionFromUrl(file.getAbsolutePath())
                    boolean isImage = FileUtils.hasImageFilename(file.getName());

                    // println("FILE LENGTH is " + file.length())
                    if (file.exists() && isImage && file.length() > 0) {
                        fileHolder.setImage(file);
                    }
                }
                break;
        }
    }

    @Override
    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    private String getHeaderString(int position) {
        try {
            return TimestampUtils.prettyTimestampLong(data.get(position).timestamp());
        } catch (Exception e) {
            e.printStackTrace();
            return " ";
        }
    }

    private CharSequence getBubbleText(int position) {
        return getHeaderString(position);
    }

    @Override
    public int getItemViewType(int pos) {
        Enumeration.Value messageType = data.get(pos).type();
        if(messageType.equals(MessageType.MESSAGE()) | messageType.equals(MessageType.GROUP_MESSAGE())) {
            return TEXT;
        } else if(messageType.equals(MessageType.ACTION()) | messageType.equals(MessageType.GROUP_ACTION())) {
            return ACTION;
        } else if(messageType.equals(MessageType.FILE_TRANSFER())) {
            return FILE;
        } else if(messageType.equals(MessageType.CALL_EVENT()) ) {
            return CALL_INFO;
        }
        return 0;
    }

}
