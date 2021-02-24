/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.taplytics.sdk.managers.TLManager;
import com.taplytics.sdk.utils.SecurityUtils;
import com.taplytics.sdk.utils.TLLog;

import java.util.ArrayList;

/**
 *
 */
public abstract class AbsExternalAnalyticsManager {

    public abstract boolean appHasSource();

    private static final String SELECT_FROM = SecurityUtils.decodeBase64String("U0VMRUNUICogRlJPTSA=");

    /**
     * An instance of our Mixpanel Manager *
     */
    private static AbsExternalAnalyticsManager instance;

    /**
     * @return {@link #db}
     */
    private SQLiteDatabase getDb() {
        return db;
    }

    /**
     * Set the Mixpanel Database
     *
     * @param Db {@link #db}
     */
    void setDb(SQLiteDatabase Db) {
        this.db = Db;
    }

    /**
     * This is the reference to the actual SQLite database where the Mixpanel data is stored *
     */
    private SQLiteDatabase db;

    /**
     * A tiny little SQLite helper to use with the {@link #db} *
     */
    private SQLiteOpenHelper DbHelper;

    /**
     * Grab all of the events waiting to be flushed. *
     */
    ArrayList<String> getTableResults(String table, String column) {
        ArrayList<String> arr = new ArrayList<>();

        try {
            // Open the database if its closed.
            if (db == null || !db.isOpen()) {
                db = DbHelper.getReadableDatabase();
            }

            // The cursor we use to grab the events.
            Cursor c = getDb().rawQuery(SELECT_FROM + table, null);
            while (c.moveToNext()) {
                String data = c.getString(c.getColumnIndex(column));
                arr.add(data);
//                TLLog.debug(data);
            }

            c.close();
        } catch (Exception e) {
            TLLog.debug("Error getting data from db: " + e.getMessage());
            db.close();
        }

        // TLLog.debug(arr.toString());
        return arr;
    }

    /**
     * Instantiate all the necessary things for this class *
     */
    Boolean setupExternalManager(String databaseName, int databaseVersion, AbsExternalAnalyticsManager manager) {
        try {
            // Start up the GADb helper so we can open the database.
            DbHelper = new TLSQLiteOpenHelper(TLManager.getInstance().getAppContext(), databaseName, databaseVersion, manager);
            return true;
        } catch (Exception e) {
            TLLog.error("External Analytics", e);
            return false;
        }
    }

    /**
     * Return the current number of rows in the selected table *
     */
    int getCursorCount(String table) {
        try {
            if (db == null || !db.isOpen()) {
                db = DbHelper.getReadableDatabase();
            }
            final Cursor c = getDb().rawQuery(SELECT_FROM + table, null);
            int count = c.getCount();
            c.close();
            return count;
        } catch (Exception e) {
            TLLog.debug("count error: " + e.getMessage());
            return 0;
        }
    }

    public abstract void flush();

    public abstract void trackToTaplyticsAndFlush();

}
