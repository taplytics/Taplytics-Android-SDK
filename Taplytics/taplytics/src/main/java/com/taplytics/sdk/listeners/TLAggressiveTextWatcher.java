/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.listeners;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.taplytics.sdk.utils.TLLog;

/**
 * Created by vicvu on 2017-02-17.
 * <p>
 * When the enforcedText is changed to something else other than what Taplytics says, don't let it!
 */

public class TLAggressiveTextWatcher implements TextWatcher {

    private TextView v;
    private String enforcedText;

    public TextWatcher getTextWatcher(TextView v, String text) {
        this.v = v;
        this.enforcedText = text;
        return this;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        try {
            if (!s.toString().equals(enforcedText)) {
                v.setText(enforcedText);
            }
        } catch (Throwable t) {
            TLLog.error("error switching enforcedText", t);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    /**
     * Update the text to be enforced for this view.
     */
    public void updateEnforcedText(String formatted) {
        this.enforcedText = formatted;
    }

    public String getEnforcedText() {
        return enforcedText;
    }
}
