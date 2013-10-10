package com.wheelphone.pet.helpers;

import java.util.Locale;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class Talker implements TextToSpeech.OnInitListener{
    private TextToSpeech tts;
	private Context mContext;
    
    public void pause() {
        // Don't forget to shutdown tts!
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
 
    public Talker(Context context){
    	mContext = context;
    }

    public void resume() {
    	tts = new TextToSpeech(mContext, this);
    }
    
	@Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
 
    }
 
    public void say(String text) {
    	tts.stop();
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }


}
