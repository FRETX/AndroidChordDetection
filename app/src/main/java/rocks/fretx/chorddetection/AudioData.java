package rocks.fretx.chorddetection;

import android.util.Log;

/**
 * Created by Onur Babacan on 9/24/16.
 */

public class AudioData {

    protected short[] audioBuffer;
    protected final int samplingFrequency;

    public AudioData(short[] aBuf , int fs){
        audioBuffer = aBuf.clone();
        samplingFrequency = fs;
    }

    public int length(){
        return audioBuffer.length;
    }

    public double getSignalPower(){
        double acc = 0;
	    double normalized;
        for (int i = 0; i < audioBuffer.length; i++) {
	        normalized = ((((double) audioBuffer[i] / 32768)) )*10;//values are really low without the *10! yup. dirty hack.
//	        Log.d("yarro", Double.toString(normalized));
	        acc +=  normalized * normalized;
        }
	    return acc / (double) audioBuffer.length;
    }
}
