package ch.tarsier.tarsier.storage;

import android.provider.BaseColumns;

/**
 * Created by McMoudi on 23/10/14.
 */
public final class ChatsContract {
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";

    public ChatsContract() {

    }

    /**
     * The discussion table fields
     */
    public static abstract class Discussion implements BaseColumns {

        public static final String TABLE_NAME = "discussion";
        public static final String COLUMN_NAME_CHAT_ID = "chatId";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_HOST = "host";
        public static final String COLUMN_NAME_TYPE = "type";

    }

    /**
     * The message table fields
     */
    public static abstract class Message implements BaseColumns {
        public static final String TABLE_NAME = "message";
        public static final String COLUMN_NAME_MSG = "msg";
        public static final String COLUMN_NAME_DATETIME = "datetime";
        public static final String COLUMN_NAME_SENDER_ID = "senderId";
        public static final String COLUMN_NAME_CHAT_ID = "chatId";

    }
}