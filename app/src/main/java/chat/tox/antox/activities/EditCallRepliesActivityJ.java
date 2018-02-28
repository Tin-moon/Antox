package chat.tox.antox.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import chat.tox.antox.R;
import chat.tox.antox.data.State;
import chat.tox.antox.wrapper.CallReply;
import rx.lang.scala.schedulers.AndroidMainThreadScheduler;
import scala.collection.JavaConversions;

/**
 * Created by Nechypurenko on 12.02.2018.
 */

public class EditCallRepliesActivityJ extends AppCompatActivity {


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_call_replies);

        ActionBar bar = getSupportActionBar();
        if(bar != null) {
            bar.setTitle(R.string.title_activity_edit_call_replies);
            bar.setDisplayHomeAsUpEnabled(true);
        }

        ListView callRepliesListView = (ListView) findViewById(R.id.call_replies_list);

        EditCallRepliesAdapter callRepliesAdapter = new EditCallRepliesAdapter(this, new ArrayList<>());

        State.userDb(this)
                .getActiveUserCallRepliesObservable().asJavaObservable()
                .observeOn(AndroidMainThreadScheduler.apply().asJavaScheduler())
                .subscribe(v1 -> {
                    callRepliesAdapter.setNotifyOnChange(false);
                    callRepliesAdapter.clear();
                    List<CallReply> data = JavaConversions.bufferAsJavaList(v1);
                    callRepliesAdapter.addAll(data);
                    callRepliesAdapter.notifyDataSetChanged();
                });

        callRepliesListView.setAdapter(callRepliesAdapter);

        callRepliesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CallReply callReply = (CallReply) parent.getItemAtPosition(position);
                DialogFragment editCallReplyDialog = EditCallReplyDialogJ.newInstance(callReply);
                editCallReplyDialog.show(getSupportFragmentManager(), "call_reply_dialog");
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class EditCallRepliesAdapter extends ArrayAdapter<CallReply> {

        public EditCallRepliesAdapter(@NonNull Context context, @NonNull List<CallReply> objects) {
            super(context, R.layout.item_call_reply, R.id.call_reply, objects);
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            super.getView(position, convertView, parent);
            CallReply callReply = (CallReply) getItem(position);
            View view = convertView == null ? LayoutInflater.from(getContext()).inflate(R.layout.item_call_reply, parent, false) : convertView;
            TextView callReplyTextView = (TextView) view.findViewById(R.id.call_reply);
            callReplyTextView.setText(callReply.reply());
            return view;
        }

    }

}
