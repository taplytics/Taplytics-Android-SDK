/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.analytics.external;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by VicV on 12/8/14.
 */
public class TLSQLiteOpenHelper extends SQLiteOpenHelper {

    private AbsExternalAnalyticsManager manager;

    public TLSQLiteOpenHelper(Context context, String dbName, int dbVersion, AbsExternalAnalyticsManager manager) {
        super(context, dbName, null, dbVersion);
        this.manager = manager;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        manager.setDb(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        manager.setDb(db);
    }
}
