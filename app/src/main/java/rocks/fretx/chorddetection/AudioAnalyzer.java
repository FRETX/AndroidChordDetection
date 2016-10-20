package rocks.fretx.chorddetection;

import android.support.annotation.Nullable;

import java.util.Arrays;

/**
 * Created by Onur Babacan on 9/23/16.
 */

abstract public class AudioAnalyzer {

    protected int samplingFrequency;
    protected int frameShift;
    protected int frameLength;
    protected int head;
    protected int atFrame;
    protected int maxFrames;
    protected AudioData audioData;

    public abstract void process(AudioData audioData);
    public abstract void processingFinished();

    public static float[] shortToFloat(short[] audio) {
        float[] output = new float[audio.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = audio[i] / 32768f;
        }
        return output;
    }

    public static double[] shortToDouble(short[] audio) {
        double[] output = new double[audio.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = audio[i] / 32768;
        }
        return output;
    }

    @Nullable
    protected short[] getNextFrame(){
        short[] outputBuffer;
        if(atFrame <= maxFrames){
            atFrame ++;
            if(head + frameLength > audioData.length()){
                //zero pad the end
                outputBuffer = (Arrays.copyOf(Arrays.copyOfRange(audioData.audioBuffer,head,audioData.length()-1), frameLength)).clone();
                head = audioData.length()-1;
                return outputBuffer;
            } else {
                //get regular frame
                outputBuffer = Arrays.copyOfRange(audioData.audioBuffer,head,head+frameLength);
                head = head+frameShift-1;
                return outputBuffer;
            }

        } else {
            //return null to signal that the end is reached
            return null;
        }
    }
}
