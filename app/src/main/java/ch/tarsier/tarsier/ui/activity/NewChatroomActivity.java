package ch.tarsier.tarsier.ui.activity;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import android.app.ActionBar;
import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.widget.EditText;
import android.widget.Toast;

import ch.tarsier.tarsier.R;
import ch.tarsier.tarsier.Tarsier;
import ch.tarsier.tarsier.domain.model.Chat;
import ch.tarsier.tarsier.domain.repository.ChatRepository;
import ch.tarsier.tarsier.domain.repository.UserRepository;
import ch.tarsier.tarsier.event.ConnectedEvent;
import ch.tarsier.tarsier.event.CreateGroupEvent;
import ch.tarsier.tarsier.exception.InsertException;
import ch.tarsier.tarsier.exception.InvalidModelException;
import ch.tarsier.tarsier.validation.ChatroomNameValidator;

/**
 * @author gluthier
 */
public class NewChatroomActivity extends Activity {

    private static final String TAG = "NewChatroomActivity";
    private EditText mChatroomName;
    private Bus mEventBus;
    private Chat mNewChatroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_chatroom);

        getEventBus().register(this);

        mChatroomName = (EditText) findViewById(R.id.chatroom_name);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getEventBus().register(this);
    }

    @Override
    public void onPause() {
        getEventBus().unregister(this);
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_chatroom, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_chatroom:
                createChatroom();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void createChatroom() {

        if (!validateChatroomName()) {
            return;
        }

        ChatRepository chatRepository = Tarsier.app().getChatRepository();
        UserRepository userRepository = Tarsier.app().getUserRepository();

        mNewChatroom = new Chat();
        mNewChatroom.setPrivate(false);
        mNewChatroom.setTitle(mChatroomName.getText().toString());
        mNewChatroom.setHost(userRepository.getUser());

        try {
            chatRepository.insert(mNewChatroom);
        } catch (InvalidModelException | InsertException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Chatroom name saved, waiting for connection...", Toast.LENGTH_SHORT).show();

        mEventBus.post(new CreateGroupEvent(mNewChatroom));
    }

    private boolean validateChatroomName() {
        return new ChatroomNameValidator().validate(mChatroomName);
    }

    private Bus getEventBus() {
        if (mEventBus == null) {
            mEventBus = Tarsier.app().getEventBus();
        }

        return mEventBus;
    }

    @Subscribe
    public void onConnectedEvent(ConnectedEvent event) {
        Log.d(TAG, "Got ConnectedEvent");

        Intent newChatroomIntent = new Intent(this, ChatActivity.class);
        newChatroomIntent.putExtra(ChatActivity.EXTRA_CHAT_MESSAGE_KEY, mNewChatroom);
        startActivity(newChatroomIntent);
    }
}
