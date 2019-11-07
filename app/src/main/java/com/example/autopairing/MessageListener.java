package com.example.autopairing;

import android.widget.TextView;

public class MessageListener {

    TextView tv;

    public MessageListener(TextView tv) {
        this.tv = tv;
    }
    public void onMessage(String message) {
        tv.setText(tv.getText() + "\n" + message);
    }
}
