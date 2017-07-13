package com.voiceofhand.meetinginterpreter.view;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.voiceofhand.meetinginterpreter.R;

public class FlashActivity extends AppCompatActivity {

    private final static int MAX_SPLASH_DURATION_TIME = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash);


        new CountDownTimer(MAX_SPLASH_DURATION_TIME, MAX_SPLASH_DURATION_TIME) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                finish();

                startActivity(new Intent(FlashActivity.this, TranslateActivity.class));
            }
        }.start();
    }
}
