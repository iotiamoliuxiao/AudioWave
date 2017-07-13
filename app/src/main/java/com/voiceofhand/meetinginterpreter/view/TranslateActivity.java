package com.voiceofhand.meetinginterpreter.view;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.voiceofhand.meetinginterpreter.R;
import com.voiceofhand.meetinginterpreter.presenter.TranslatePresenter;
import com.voiceofhand.meetinginterpreter.view.intef.TranslateActivityInterface;
import com.voiceofhand.meetinginterpreter.view.voice.VoiceLineView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by iotiamo on 2017/7/12.
 */

public class TranslateActivity extends AppCompatActivity implements TranslateActivityInterface {
    private final static int PERMISSION_REQUEST_CODE = 101;

    @BindView(R.id.titlebar_back)
    ImageView mTitleTabBack;

    @BindView(R.id.titlebar_title)
    TextView mTitleTabTextView;

    @BindView(R.id.voiceBar)
    FrameLayout mVoiceActionBar;

    @BindView(R.id.iv_speech_search_mik)
    ImageView mRecordButtonBackgroud;
    @BindView(R.id.voiceTv)
    TextView mRecordButtonText;

    @BindView(R.id.translate_content)
    TextView mTranslateContentView;

    @BindView(R.id.wave_speech)
    VoiceLineView mVoiceLineView;

    private TranslatePresenter mPresenter = null;

    private boolean mIsRecording = false;

    private int mMinVolumeNumb = 999;
    private int mMaxVolumeNumb = 0;

    private TranslatePresenter.iRecordingListener mRecordListener = new TranslatePresenter.iRecordingListener() {
        @Override
        public void onStart() {
            toStartRecordView();
        }

        @Override
        public void onStop() {
            toFinishRecordView();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translate);
        ButterKnife.bind(this);

        mTitleTabBack.setVisibility(View.INVISIBLE);
        mTitleTabTextView.setText(R.string.translate_title);
        mVoiceActionBar.setVisibility(View.VISIBLE);

        mPresenter = new TranslatePresenter();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            mPresenter.start(this, mRecordListener);
        }

        mIsRecording = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPresenter.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions.length == 0 || grantResults.length == 0 || permissions.length != grantResults.length) {
            return;
        }
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isGranted = true;
            for (int ii = 0; ii < grantResults.length; ii++) {
                if (grantResults[ii] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                }
            }

            if (isGranted) {
                mPresenter.start(this, mRecordListener);
            }
        }
    }

    public void toStartRecordView() {
        mRecordButtonBackgroud.setImageResource(R.mipmap.voice_circular);
        mRecordButtonText.setTextColor(this.getResources().getColor(R.color.voice_green));
        mRecordButtonText.setVisibility(View.VISIBLE);
    }

    public void toFinishRecordView() {
        mRecordButtonBackgroud.setImageResource(R.mipmap.voice_bt_mic);
        mRecordButtonText.setVisibility(View.INVISIBLE);
    }

    @Override
    public void reportVoiceVolume(int volume) {
        mVoiceLineView.putVolumeValue(volume);

        mMaxVolumeNumb = Math.max(mMaxVolumeNumb, volume);
        mMinVolumeNumb = Math.min(mMinVolumeNumb, volume);

        //mVolumeDisplayView.setText(mMinVolumeNumb + " - " + mMaxVolumeNumb);
    }

    @Override
    public void reportRecognResult(String res) {
        if (res.isEmpty() == false) {
            mTranslateContentView.setText(res);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @OnClick(R.id.iv_speech_search_mik)
    public void onChangeRecordState(View v) {
        if (mIsRecording == true) {
            mIsRecording = false;
            mPresenter.stopAudioRecording();
        } else {
            mIsRecording = true;
            mPresenter.startAudioRecording();
        }
    }

}
