package com.voiceofhand.meetinginterpreter.view.intef;

import android.content.Context;

/**
 * Created by iotiamo on 2017/7/12.
 */

public interface TranslateActivityInterface {

    void reportVoiceVolume(int volume);
    void reportRecognResult(String res);

    Context getContext();
}
