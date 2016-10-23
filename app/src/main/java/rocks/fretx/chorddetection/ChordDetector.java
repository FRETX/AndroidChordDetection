package rocks.fretx.chorddetection;

import android.support.annotation.Nullable;

import org.jtransforms.fft.DoubleFFT_1D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Onur Babacan on 10/20/16.
 */

public class ChordDetector extends AudioAnalyzer {


    private short[] tempBuffer;
    private List<Chord> targetChords;
    protected Chord detectedChord;

    //Plot
    double[] magnitudeSpectrum;

    public ChordDetector(final int samplingFrequency, final int frameLength, final int frameShift, final List<Chord> targetChords) {
//        int frameLength = Math.round ((float)audioData.samplingFrequency * CHROMAGRAM_FRAME_LENGTH_IN_S);
//        int frameShift = frameLength / 4;
//        final float CHROMAGRAM_FRAME_LENGTH_IN_S = 0.75f;
        this.samplingFrequency = samplingFrequency;
        this.frameLength = frameLength;
        this.frameShift = frameShift;
        this.head = -1;
        this.atFrame = -1;
        this.maxFrames = -1;
        this.targetChords = targetChords;
    }


    private double[] getChromagram(short[] audioBuffer){
        //Make sure the buffer length is even
        short[] tmpAudio;
        if((audioBuffer.length % 2) == 0){
            tmpAudio = audioBuffer;
        } else{
            tmpAudio = new short[audioBuffer.length+1];
            for (int i = 0; i < audioBuffer.length; i++) {
                tmpAudio[i] = audioBuffer[i];
            }
            tmpAudio[audioBuffer.length] = 0;
        }
        double[] buf = shortToDouble(audioBuffer);
        //FFT
        DoubleFFT_1D fft = new DoubleFFT_1D(buf.length);
        fft.realForward(buf);
        //Get Magnitude spectrum from FFT
        magnitudeSpectrum = new double[buf.length/2];
        magnitudeSpectrum[0] = Math.abs(buf[0]);
        magnitudeSpectrum[magnitudeSpectrum.length-1] = Math.abs(buf[1]);
        for (int i = 1; i < magnitudeSpectrum.length-1; i++) {
            magnitudeSpectrum[i] = Math.sqrt((buf[2*i]*buf[2*i]) + (buf[2*i+1]*buf[2*i+1]));
        }

        double A1 = 55; //reference note A1 in Hz
        int peakSearchWidth = 2;
        double[] chromagram = new double[12];

        for (int interval = 0; interval < 11; interval++) {
            for (int phi = 1; phi < 5; phi++) {
                for (int harmonic = 1; harmonic < 2; harmonic++) {
                    int kprime = (int) Math.round( frequencyFromInterval(A1,interval) * (double)phi * (double)harmonic / ((double)samplingFrequency/(double)frameLength) );
                    int k0 = kprime - (peakSearchWidth*harmonic);
                    int k1 = kprime + (peakSearchWidth*harmonic);
                    chromagram[interval] += findMaxValue(magnitudeSpectrum,k0,k1) / harmonic;
                }
            }
        }

        return chromagram;
    }

    private Chord detectChord(double[] chromagram, List<Chord> targetChords){
        //Take the square of chromagram so the peak differences are more pronounced. see paper.
        for (int i = 0; i < chromagram.length; i++) {
            chromagram[i] *= chromagram[i];
        }

        double[] deltas = new double[targetChords.size()];
        Arrays.fill(deltas,0);
        double[] bitMask = new double[12];
        for (int i = 0; i < targetChords.size(); i++) {
            //Generate bit mask for target chord
            Arrays.fill(bitMask,1);
            int[] notes = targetChords.get(i).getNotes();
            for (int j = 0; j < notes.length; j++) {
                bitMask[notes[j]-1] = 0;
            }
            for (int j = 0; j < chromagram.length; j++) {
                deltas[i] += chromagram[j] * bitMask[j];
            }
            deltas[i] /= notes.length;
            deltas[i] = Math.sqrt(deltas[i]);
        }

        int chordIndex = findMinIndex(deltas);
        return targetChords.get(chordIndex);
    }

    private double frequencyFromInterval(double baseNote, int intervalInSemitones){
        return baseNote * Math.pow(2,intervalInSemitones/12);
    }

    private double findMaxValue(double[] arr, int beginIndex, int endIndex){
        //TODO: array safety
        double maxVal = -Double.MAX_VALUE;
        for (int i = beginIndex; i <= endIndex; i++) {
            if(arr[i] > maxVal) maxVal = arr[i];
        }
        return maxVal;
    }

    private int findMinIndex(double[] arr){
        double minVal = Double.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < arr.length; i++) {
            if(arr[i] < minVal){
                minVal = arr[i];
                minIndex = i;
            }
        }
        return minIndex;
    }
    @Override
    public void process(AudioData inputAudioData) {
        audioData = inputAudioData;
        if(audioData.length() < frameLength){
            maxFrames = (int) Math.ceil( (double)(audioData.length() - frameLength) / (double) frameShift);
        } else {
            maxFrames = 1;
        }
        atFrame = 1;
        head = 0;
        double[] chromagram;
        while((tempBuffer = getNextFrame()) != null ){
            chromagram = getChromagram(tempBuffer);
            detectedChord = detectChord(chromagram,targetChords);
        }
        processingFinished();
    }

    @Override
    public void processingFinished() {

    }

    private double[] getHammingWindow(int windowLength){
        double alpha = 0.54;
        double beta = 1 - alpha;
        double [] window = new double[windowLength];

        for (int i = 0; i < windowLength; i++) {
            window[i] = alpha - beta * Math.cos( (2*Math.PI * i) / (windowLength - 1));
        }

        return window;
    }


}
