/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by clvcooke on 11/21/15.
 */
public class TLDatabaseHelper extends SQLiteOpenHelper {

    private static String EVENT_TABLE_NAME;

    private static String SQL_CREATE_TABLE;

    private static String SQL_DROP_TABLE;

    private static String SQL_DELETE_ENTRIES;

    private static String SQL_ROW_COUNT;

    private static String DATABASE_NAME = "TLDatabases";

    //UPDATE THIS FOR ANY DATABASE CHANGES!
    private static int DATABASE_VERSION = 2;

    public TLDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Used so we can ensure future writes and reads don't have to call onCreate
     */
    public void init() {
        initQueriesIfNecessary();
        getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Create the entries we need
        initQueriesIfNecessary();
        db.execSQL(SQL_CREATE_TABLE);
    }

    private static void initQueriesIfNecessary() {
        //Due to a silly security request, we have to obfuscate our sql queries despite the db being heavily obfuscated anyway.
        if (SecurityUtils.decodeSQLQueries()) {
            EVENT_TABLE_NAME = SecurityUtils.sqlQueries[0];
            SQL_CREATE_TABLE = SecurityUtils.sqlQueries[1] + " " + EVENT_TABLE_NAME + " " + SecurityUtils.sqlQueries[2];
            SQL_DROP_TABLE = SecurityUtils.sqlQueries[3] + " " + EVENT_TABLE_NAME;
            SQL_DELETE_ENTRIES =
                    SecurityUtils.sqlQueries[4] + " " + EVENT_TABLE_NAME + " " + SecurityUtils.sqlQueries[5] + "  " + EVENT_TABLE_NAME + " " + SecurityUtils.sqlQueries[6];
            SQL_ROW_COUNT = SecurityUtils.sqlQueries[7] + " " + EVENT_TABLE_NAME;
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        initQueriesIfNecessary();
        //This is only used to store events temporarily, so if we need to upgrade just wipe the db
        db.execSQL(SQL_DROP_TABLE);
        onCreate(db);
    }

    public void writeEvent(JSONObject event) {
        String string = event.toString();
        if (TLUtils.isJSONValid(string)) {
            if (TLReaderWriter.getWriterCipher() != null) {
                string = SecurityUtils.encrypt(string, TLReaderWriter.getWriterCipher());
            }
            ContentValues cv = new ContentValues();
            cv.put("timestamp", System.currentTimeMillis());
            cv.put("event", string);
            try {
                getWritableDatabase().insert(EVENT_TABLE_NAME, null, cv);
            } catch (Exception e) {
                TLLog.error("Exception writing events", e);
            }
            int eventCount = getEventCount();
            if (eventCount > 1000) {
                deleteEvents(eventCount - 950);
            }
        }
    }


    //starting at the end, get the oldest events (limited by batchsize)
    public ArrayList<JSONObject> getEvents(int maxBatchSize) throws JSONException {
        ArrayList<JSONObject> events = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().query(EVENT_TABLE_NAME, new String[]{"timestamp", "event"}, null, null, null, null, "timestamp ASC", Integer.toString(maxBatchSize));
            while (cursor.moveToNext()) {
                String string = cursor.getString(1);
                JSONObject event;
                try {
                    try {
                        event = new JSONObject(SecurityUtils.decryptValueWithCipher(string, TLReaderWriter.getReaderCipher()));
                    } catch (Throwable e) {
                        event = new JSONObject(string);
                    }
                    events.add(event);
                } catch (Throwable e) {
                    //Move on..
                }
            }
        } catch (Exception e) {
            TLLog.error("Exception while getting events", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return events;
    }

    //delete the oldest count events
    public void deleteEvents(int count) {
        try {
            getWritableDatabase().execSQL(String.format(SQL_DELETE_ENTRIES, count));
        } catch (Exception e) {
            TLLog.error("Exception while deleting events", e);
        }
    }

    public int getEventCount() {
        int count = 0;
        Cursor cursor = null;
        try {
            cursor = getReadableDatabase().rawQuery(SQL_ROW_COUNT, null);
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0);
            }
            cursor.close();
        } catch (Exception e) {
            TLLog.error("Exception while getting event count", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

}
