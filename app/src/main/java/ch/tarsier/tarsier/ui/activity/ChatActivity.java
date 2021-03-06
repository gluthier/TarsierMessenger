package ch.tarsier.tarsier.ui.activity;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import java.util.ArrayList;
import java.util.List;
import ch.tarsier.tarsier.Tarsier;
import ch.tarsier.tarsier.domain.model.Chat;
import ch.tarsier.tarsier.event.DisplayMessageEvent;
import ch.tarsier.tarsier.event.ErrorConnectionEvent;
import ch.tarsier.tarsier.event.SendMessageEvent;
import ch.tarsier.tarsier.exception.NoSuchModelException;
import ch.tarsier.tarsier.ui.adapter.BubbleAdapter;
import ch.tarsier.tarsier.ui.view.BubbleListViewItem;
import ch.tarsier.tarsier.util.DateUtil;
import ch.tarsier.tarsier.ui.view.EndlessListView;
import ch.tarsier.tarsier.ui.view.EndlessListener;
import ch.tarsier.tarsier.R;
import ch.tarsier.tarsier.domain.model.Message;

/**
 *
 * This activity is responsible to display the messages of the current chat.
 * This chat can either be a private one, or a chatroom (multiple peers).
 *
 * Bubble's layout is inspired from https://github.com/AdilSoomro/Android-Speech-Bubble
 *
 * @author marinnicolini and xawill (extreme programming)
 */
public class ChatActivity extends Activity implements EndlessListener {

    public final static String EXTRA_CHAT_MESSAGE_KEY = "ch.tarsier.tarsier.ui.activity.CHAT";

    private static final int NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE = 20;
    private static final String TAG = "ChatActivity";

    private Chat mChat;
    private BubbleAdapter mListViewAdapter;
    private EndlessListView mListView;
    private Bus mEventBus;
    private boolean mActivityJustStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        getEventBus().register(this);

        mListView = (EndlessListView) findViewById(R.id.list_view);
        mListView.setLoadingView(R.layout.loading_layout);

        mListViewAdapter = new BubbleAdapter(this, new ArrayList<BubbleListViewItem>());
        mListView.setBubbleAdapter(mListViewAdapter);
        mListView.setEndlessListener(this);

        mChat = (Chat) getIntent().getSerializableExtra(EXTRA_CHAT_MESSAGE_KEY);

        if (mChat != null && mChat.getId() > -1) {
            DatabaseLoader dbl = new DatabaseLoader();
            dbl.execute();

            mActivityJustStarted = true;
        } else {
            Toast.makeText(this, "Error : This chat doesn't exists", Toast.LENGTH_LONG).show();
            Intent goBackToChatList = new Intent(this, ChatListActivity.class);
            startActivity(goBackToChatList);
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setTitle(mChat.getTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat, menu);

        if (mChat.isPrivate()) {
            menu.removeItem(R.id.goto_chatroom_peers_activity);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.goto_chatroom_peers_activity:
                openChatroomPeers();
                return true;
            case R.id.goto_profile_activity:
                openProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openChatroomPeers() {
        Intent openChatroomPeersIntent = new Intent(this, ChatroomPeersActivity.class);
        startActivity(openChatroomPeersIntent);
    }

    private void openProfile() {
        Intent openProfileIntent = new Intent(this, ProfileActivity.class);
        startActivity(openProfileIntent);
    }

    /**
     * Should not be called if message is empty (button should be disabled)
     * @param view the view that initiated the action. Here, the button to send the message
     */
    public void onClickSendMessage(View view) {
        TextView messageView = (TextView) findViewById(R.id.message_to_send);
        String messageText = messageView.getText().toString();
        Message sentMessage = new Message(mChat.getId(), messageText, DateUtil.getNowTimestamp());

        if (!messageText.isEmpty()) {
            //Add the message to the ListView
            mListView.addNewMessage(sentMessage);

            getEventBus().post(new SendMessageEvent(mChat, sentMessage));

            mListView.smoothScrollToPosition(mListViewAdapter.getCount() - 1);

            messageView.setText("");

            //Scroll down to most recent message
            mListView.setSelection(mListViewAdapter.getCount() - 1);
        } else {
            messageView.setError("No message to send!");
        }
    }

    private Bus getEventBus() {
        if (mEventBus == null) {
            mEventBus = Tarsier.app().getEventBus();
        }

        return mEventBus;
    }

    @Override
    public void loadData() {
        DatabaseLoader dbl = new DatabaseLoader();
        dbl.execute();
    }

    @Subscribe
    public void onDisplayMessageEvent(DisplayMessageEvent event) {
        Log.d(TAG, "Got DisplayMessageEvent.");
        Log.d(TAG, "Message id: " + event.getMessage().getChatId() + " | Chat id: " + mChat.getId());

        Message message = event.getMessage();
        Chat chat = event.getChat();

        if (message.getChatId() != mChat.getId()) {
            if (chat.isPrivate()) {
                String text = event.getSender().getUserName() + " just sent you a private message.";
                Toast.makeText(this, text, Toast.LENGTH_LONG).show();
            }
            return;
        }

        mListView.addNewMessage(message);
        mListView.setSelection(mListViewAdapter.getCount() - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getEventBus().register(this);
    }

    @Override
    protected void onPause() {
        getEventBus().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void onReceiveConnectionError(ErrorConnectionEvent event) {
        Toast.makeText(this, event.getErrorMessage(), Toast.LENGTH_LONG).show();
    }

    /**
     * Async Task for the loading of the messages from the database on another thread.
     */
    private class DatabaseLoader extends AsyncTask<Void, Void, List<Message>> {

        @Override
        protected List<Message> doInBackground(Void... params) {
            while (!Tarsier.app().getDatabase().isReady()) { }

            List<Message> newMessages = new ArrayList<Message>();
            try {
                newMessages.addAll(Tarsier.app().getMessageRepository().findByChatUntil(
                        mChat,
                        mListViewAdapter.getLastMessageTimestamp(),
                        NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE));
            } catch (NoSuchModelException e) {
                e.printStackTrace();
            }

            return newMessages;
        }

        @Override
        protected void onPostExecute(List<Message> result) {
            super.onPostExecute(result);

            // Tell the ListView to stop retrieving messages since there all loaded in it.
            if (result.size() < NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE) {
                mListView.setAllMessagesLoaded(true);
            }

            if (result.size() > 0) {
                mListView.fetchOldMessages(result);
            }

            if (mActivityJustStarted) {
                mListView.setSelection(mListViewAdapter.getCount() - 1);
            }
            mActivityJustStarted = false;
        }
    }
}
