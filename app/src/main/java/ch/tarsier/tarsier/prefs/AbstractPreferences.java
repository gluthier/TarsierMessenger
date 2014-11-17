package ch.tarsier.tarsier.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import ch.tarsier.tarsier.Tarsier;

/**
 * @author romac
 *
 * TODO: Have `getString()` take an Enum rather than an int.
 */
public abstract class AbstractPreferences {

    private Tarsier mApp;
    private SharedPreferences mShared;

    protected abstract String getPreferencesFile();

    public AbstractPreferences() {
        mApp = Tarsier.app();
        mShared = mApp.getSharedPreferences(getPreferencesFile(), Context.MODE_PRIVATE);
    }

    protected String getString(int key) {
        String keyString = mApp.getString(key);
        return mShared.getString(keyString, "");
    }

    protected String getString(int key, String defaultValue) {
        return mShared.getString(Tarsier.app().getString(key), defaultValue);
    }

    protected void setString(int key, String data) {
        SharedPreferences.Editor editor = mShared.edit();
        editor.putString(mApp.getString(key), data);
        editor.apply();
    }

    protected long getLong(int key, long defaultValue) {
        String keyString = mApp.getString(key);
        return mShared.getLong(keyString, defaultValue);
    }

}