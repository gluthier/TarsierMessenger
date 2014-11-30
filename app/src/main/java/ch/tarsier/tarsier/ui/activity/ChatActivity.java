package ch.tarsier.tarsier.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import ch.tarsier.tarsier.Tarsier;
import ch.tarsier.tarsier.database.FillDatabaseWithFictionalData;
import ch.tarsier.tarsier.exception.InsertException;
import ch.tarsier.tarsier.exception.InvalidCursorException;
import ch.tarsier.tarsier.exception.InvalidModelException;
import ch.tarsier.tarsier.exception.NoSuchModelException;
import ch.tarsier.tarsier.ui.adapter.BubbleAdapter;
import ch.tarsier.tarsier.util.DateUtil;
import ch.tarsier.tarsier.ui.view.EndlessListView;
import ch.tarsier.tarsier.ui.view.EndlessListener;
import ch.tarsier.tarsier.R;
import ch.tarsier.tarsier.domain.model.Message;
import ch.tarsier.tarsier.validation.EditTextMessageValidator;

/**
 * @author marinnicolini and xawill (extreme programming)
 *
 * This activity is responsible to display the messages of the current chat.
 * This chat can either be a private one, or a chatroom (multiple peers).
 *
 * Bubble's layout is inspired from https://github.com/AdilSoomro/Android-Speech-Bubble
 */
public class ChatActivity extends Activity implements EndlessListener {

    private static final int NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE = 10;

    // TODO: Store those IDs in their own class, so that they can be shared between classes
    //       while reducing the coupling a little.
    private static final String EXTRA_CHAT_ID = "chatId";

    private static Point windowSize;
    private long mChatId;
    private BubbleAdapter mListViewAdapter;
    private EndlessListView mListView;
    private EditText mMessageToBeSend;

    public void onClickSendMessage(View view) {
        String messageText = ((TextView) findViewById(R.id.message_to_send)).getText().toString();
        Message sentMessage = new Message(mChatId, messageText, DateUtil.getNowTimestamp());


        //Add the message to the ListView
        try {
            mListView.addNewData(sentMessage);

            //Add the message to the database
            Tarsier.app().getMessageRepository().insert(sentMessage);
        } catch (InsertException e) {
            e.printStackTrace();
        } catch (InvalidModelException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadData() {
        DatabaseLoader dbl = new DatabaseLoader();
        dbl.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        enableSendMessageImageButton(false);

        mListView = (EndlessListView) findViewById(R.id.list);
        mListView.setLoadingView(R.layout.loading_layout);

        mListViewAdapter = new BubbleAdapter(this, R.layout.message_row);
        mListView.setBubbleAdapter(mListViewAdapter);
        mListView.setEndlessListener(this);

        /*Intent startingIntent = getIntent();
        mChatId = startingIntent.getIntExtra(EXTRA_CHAT_ID, -1);*/

        DatabaseLoader dbl = new DatabaseLoader();
        dbl.execute();

        mMessageToBeSend = (EditText) findViewById(R.id.message_to_send);

        mMessageToBeSend.addTextChangedListener(new EditTextWatcher());

        /** Todo if we have time... Possibility to retrieve one message not yet sent but already
         *  Todo typed
         */
    }

    /**
     * Async Task for the loading of the messages from the database on another thread.
     */
    private class DatabaseLoader extends AsyncTask<Void, Void, List<Message>> {

        @Override
        protected List<Message> doInBackground(Void... params) {
            while (!Tarsier.app().getDatabase().isReady()) { }

            mChatId = FillDatabaseWithFictionalData.getChat10Id();

            if (mChatId == -1) {
                // FIXME: Handle this
            }

            long lastMessageTimestamp = mListViewAdapter.getLastMessageTimestamp();

            List<Message> newMessages = null;
            try {
                try {
                    newMessages = Tarsier.app().getMessageRepository().findByChat(
                            Tarsier.app().getChatRepository().findById(mChatId),
                            lastMessageTimestamp,
                            NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE);
                } catch (InvalidCursorException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchModelException e) {
                e.printStackTrace();
            }

            //Encapsulate messages into messageViewModels
            ArrayList<Message> newMessageViewModels = new ArrayList<Message>();
            for (Message message: newMessages) {
                newMessageViewModels.add(message);
            }

            return newMessageViewModels;
        }

        @Override
        protected void onPostExecute(List<Message> result) {
            super.onPostExecute(result);
            mListView.addNewData(result);

            // Tell the ListView to stop retrieving messages since there all loaded in it.
            if (result.size() < NUMBER_OF_MESSAGES_TO_FETCH_AT_ONCE) {
                mListView.setAllMessagesLoaded(true);
            }
        }
    }

    /**
     * Toggle the clickable property of the lets_chat Button
     * @param enable true makes the Button clickable.
     */
    private void enableSendMessageImageButton(boolean enable) {
        ImageButton send = (ImageButton) findViewById(R.id.sendImageButton);
        send.setClickable(enable);
    }

    /**
     * Verify that we can enable the Button that can send the message to the listView and to the
     * Storage by checking the EditText of the Activity
     */
    private final class EditTextWatcher implements TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
            enableSendMessageImageButton(sendMessageImageButtonCanBeEnabled());
        }

        @Override
        public void afterTextChanged(Editable editable) {

        }
    }

    private boolean sendMessageImageButtonCanBeEnabled() {
        return validateSendMessage();
    }

    private boolean validateSendMessage() {
        return new EditTextMessageValidator("No message to send!").validate(mMessageToBeSend);
    }

}