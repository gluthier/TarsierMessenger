package ch.tarsier.tarsier.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * @author McMoudi
 * @author romac
 */
public class Database {

    private volatile DatabaseHelper mDatabaseHelper;
    private volatile SQLiteDatabase mReadable;
    private volatile SQLiteDatabase mWritable;

    private volatile boolean mIsReady = false;

    public Database(Context context) {
        this(new DatabaseHelper(context));
    }

    public Database(DatabaseHelper databaseHelper) {
        mDatabaseHelper = databaseHelper;

        mReadable = mDatabaseHelper.getReadableDatabase();
        mWritable = mDatabaseHelper.getWritableDatabase();

        mIsReady = true;
    }

    public SQLiteDatabase getReadable() {
        return mReadable;
    }

    public SQLiteDatabase getWritable() {
        return mWritable;
    }

    public boolean isReady() {
        return mIsReady;
    }
}
