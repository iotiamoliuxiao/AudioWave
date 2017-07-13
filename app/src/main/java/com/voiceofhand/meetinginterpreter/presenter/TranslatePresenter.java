package com.voiceofhand.meetinginterpreter.presenter;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.NlsListener;
import com.alibaba.idst.nls.StageListener;
import com.alibaba.idst.nls.internal.protocol.NlsRequest;
import com.alibaba.idst.nls.internal.protocol.NlsRequestASR;
import com.alibaba.idst.nls.internal.protocol.NlsRequestProto;
import com.google.gson.Gson;
import com.voiceofhand.meetinginterpreter.pojo.TranslatePojo;
import com.voiceofhand.meetinginterpreter.view.intef.TranslateActivityInterface;

/**
 * Created by iotiamo on 2017/7/12.
 */

public class TranslatePresenter {
    private final static String TAG = TranslatePresenter.class.getSimpleName();
    private final static String APP_KEY = "nls-service-streaming";
    private final static String APP_SC_ID = "LTAINV7Hs8XUP4US";
    private final static String APP_SC_PWD = "kKJyuQky1hDBmip5CFIBpAQNvC0WOk";

    private final static int MSG_RECORD_START = 102;

    private TranslateActivityInterface mView = null;

    private NlsClient mNlsClient = null;
    private NlsRequest mNlsRequest  = null;

    private boolean mIsRecordContorlRun = false;

    public interface iRecordingListener {
        void onStart();
        void onStop();
    }

    private Handler recordHandler = new Handler() {
        @Override
        public void handleMessage (Message msg) {
            if (msg.what == MSG_RECORD_START) {
                TranslatePresenter.this.doStartAudioRecord();
            }
        }
    };

    private iRecordingListener mRecordListener = null;
    public void start(TranslateActivityInterface activityInterface, iRecordingListener listener) {
        mView = activityInterface;

        mRecordListener = listener;

        startAudioRecording();
    }

    public void finish() {
        mRecordListener = null;
        stopAudioRecording();
    }

    public void doStartAudioRecord() {
        mNlsRequest = initNlsRequest();
        mNlsRequest.setApp_key(APP_KEY);
        mNlsRequest.setAsr_sc("opu");
        mNlsRequest.setAsrResposeMode(NlsRequestASR.mode.STREAMING);

        NlsClient.openLog(true);
        NlsClient.configure(mView.getContext().getApplicationContext());

        mNlsClient = NlsClient.newInstance(mView.getContext(), mRecognizeListener, mStageListener, mNlsRequest);
        mNlsClient.setMaxRecordTime(60000);  //设置最长语音
        mNlsClient.setMaxStallTime(5000);    //设置最短语音
        mNlsClient.setMinRecordTime(1000);    //设置最大录音中断时间
        mNlsClient.setRecordAutoStop(false);  //设置VAD
        mNlsClient.setMinVoiceValueInterval(200); //设置音量回调时长

        mNlsRequest.authorize(APP_SC_ID, APP_SC_PWD); //请替换为用户申请到的数加认证key和密钥
        mNlsClient.start();
    }

    public void startAudioRecording() {
        if (mIsRecordContorlRun == true) {
            return;
        }

        Log.i("asr", "call startAudioRecording()");

        mIsRecordContorlRun = true;

        doStartAudioRecord();
    }

    public void stopAudioRecording() {
        if (mIsRecordContorlRun == false) {
            return;
        }

        Log.i("asr", "call stopAudioRecording()");
        mIsRecordContorlRun = false;
        mNlsClient.stop();
    }

    private NlsRequest initNlsRequest(){
        NlsRequestProto proto = new NlsRequestProto(mView.getContext());
        return new NlsRequest(proto);
    }

    private NlsListener mRecognizeListener = new NlsListener() {

        @Override
        public void onRecognizingResult(int status, RecognizedResult result) {
            switch (status) {
                case NlsClient.ErrorCode.SUCCESS:
                    Log.i("asr", "callback onRecognizResult " + result.asr_out);
                    Gson gson = new Gson();
                    final TranslatePojo translatePojo = gson.fromJson(result.asr_out, TranslatePojo.class);
                    mView.reportRecognResult(translatePojo.result);

                    break;
                case NlsClient.ErrorCode.RECOGNIZE_ERROR:
                    //Toast.makeText(mView.getContext(), "recognizer error", Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.RECORDING_ERROR:
                    //Toast.makeText(mView.getContext(),"recording error",Toast.LENGTH_LONG).show();
                    break;
                case NlsClient.ErrorCode.NOTHING:
                    //Toast.makeText(mView.getContext(),"nothing",Toast.LENGTH_LONG).show();
                    break;
            }
        }


    } ;

    private StageListener mStageListener = new StageListener() {
        @Override
        public void onStartRecognizing(NlsClient recognizer) {
            super.onStartRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStopRecognizing(NlsClient recognizer) {
            super.onStopRecognizing(recognizer);    //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public void onStartRecording(NlsClient recognizer) {
            Log.i("asr", "onStartRecording()");

            //mView.toStartRecordView();
            if (mRecordListener != null) {
                mRecordListener.onStart();
            }
        }

        @Override
        public void onStopRecording(NlsClient recognizer) {
            Log.i("asr", "onStopRecording()");

            //mView.toFinishRecordView();
            if (mIsRecordContorlRun == true) {
                recordHandler.sendEmptyMessageDelayed(MSG_RECORD_START, 500);
                return;
            }

            if (mRecordListener != null) {
                mRecordListener.onStop();
            }
        }

        @Override
        public void onVoiceVolume(int volume) {
            super.onVoiceVolume(volume);
            mView.reportVoiceVolume(volume);
        }

    };
}
