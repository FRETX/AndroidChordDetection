package rocks.fretx.chorddetection;

import android.util.Log;

import org.jtransforms.fft.DoubleFFT_1D;

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
    protected double[] magnitudeSpectrum;
	protected double volume = 0;
    protected boolean readLock = true;

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
//	    Log.d("length even?", String.valueOf(((audioBuffer.length % 2) == 0)));
        if((audioBuffer.length % 2) == 0){
            tmpAudio = audioBuffer.clone();
        } else{
            tmpAudio = new short[audioBuffer.length+1];
            for (int i = 0; i < audioBuffer.length; i++) {
                tmpAudio[i] = audioBuffer[i];
            }
            tmpAudio[audioBuffer.length] = 0;
        }

//	    double acc = 0;
//	    for (int i = 0; i < audioData.audioBuffer.length; i++) {
//		    acc += Math.abs(((double)audioData.audioBuffer[i]/32768));
//	    }
//	    Log.d("audio buffer sum", Double.toString(acc));

        double[] buf = shortToDouble(audioBuffer);
	    for (int i = 0; i < buf.length; i++) {
		    buf[i] -= 0.5; //center the signal on 0 before windowing
	    }

//	    double acc2 = 0;
//	    for (int i = 0; i < buf.length; i++) {
//		    acc2 += Math.abs(buf[i]);
//	    }
//	    Log.d("audio buffer sum", Double.toString(acc2));

        //FFT
	    double[] window = getHammingWindow(buf.length);
	    for (int i = 0; i < buf.length; i++) {
		    buf[i] *= window[i];
	    }
        DoubleFFT_1D fft = new DoubleFFT_1D(buf.length);
        fft.realForward(buf);
        //TODO: Lock/unlock magnitudeSpectrum while writing
        //Get Magnitude spectrum from FFT
        readLock = true;
        magnitudeSpectrum = new double[buf.length/2];
        magnitudeSpectrum[0] = Math.abs(buf[0]);
        magnitudeSpectrum[magnitudeSpectrum.length-1] = Math.abs(buf[1]);
        for (int i = 1; i < magnitudeSpectrum.length-1; i++) {
            magnitudeSpectrum[i] = ((buf[2*i]*buf[2*i]) + (buf[2*i+1]*buf[2*i+1]));
        }

	    //Normalize and pre-process spectrum

        //Normalize by total energy:
//	    double sum = 0;
//	    for (int i = 0; i < magnitudeSpectrum.length; i++) {
//		    sum+= magnitudeSpectrum[i];
//	    }

        //Normalize by max peak
        double maxVal = 0;
        for (int i = 0; i < magnitudeSpectrum.length; i++) {
            if(magnitudeSpectrum[i] > maxVal) maxVal = magnitudeSpectrum[i];
        }
        double normalizationFactor = maxVal;
        for (int i = 0; i < magnitudeSpectrum.length; i++) {
		    //The sqrt comes from the paper. It's for making the peak differences smaller
		    magnitudeSpectrum[i] = Math.sqrt(magnitudeSpectrum[i]/ normalizationFactor);
	    }
        readLock = false;



        double A1 = 55; //reference note A1 in Hz
        int peakSearchWidth = 2;
	    int kprime,k0,k1;
	    double[] chromagram = new double[12];
	    Arrays.fill(chromagram,0);
        for (int interval = 0; interval < 12; interval++) {
            for (int phi = 1; phi <= 5; phi++) {
                for (int harmonic = 1; harmonic <= 2; harmonic++) {
                    kprime = (int) Math.round( frequencyFromInterval(A1,interval) * (double)phi * (double)harmonic / ((double)samplingFrequency/(double)frameLength) );
                    k0 = kprime - (peakSearchWidth*harmonic);
                    k1 = kprime + (peakSearchWidth*harmonic);
                    chromagram[interval] += findMaxValue(magnitudeSpectrum, k0, k1) / harmonic;
                }
            }
        }

	    return chromagram;
    }

    private Chord detectChord(List<Chord> targetChords,double[] chromagram){
        //Take the square of chromagram so the peak differences are more pronounced. see paper.
        for (int i = 0; i < chromagram.length; i++) {
            chromagram[i] *= chromagram[i];
        }

	    Log.d("Chromagram", Arrays.toString(chromagram));

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

//	        Log.d("Chord",targetChords.get(i).root + " " + targetChords.get(i).type);
//	        Log.d("Notes",Arrays.toString(notes));
//	        Log.d("Bitmask " + Integer.toString(i), Arrays.toString(bitMask));

	        //Calculate the normalized total difference with target chord pattern
            for (int j = 0; j < chromagram.length; j++) {
                deltas[i] += chromagram[j] * bitMask[j];
            }
            deltas[i] /= 12-notes.length;
            deltas[i] = Math.sqrt(deltas[i]);
        }
//	    Log.d("deltas", Arrays.toString(deltas));
        int chordIndex = findMinIndex(deltas);
//	    Log.d("minIndex", Integer.toString(chordIndex));
        return targetChords.get(chordIndex);
    }

    private double frequencyFromInterval(double baseNote, int intervalInSemitones){
        return baseNote * Math.pow(2,(double)intervalInSemitones/12);
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
		volume = audioData.getSignalPower();
//		Log.d("volume",Double.toString(volume));
	    //TODO: Fix this in PitchDetector too!
        if(audioData.length() > frameLength){
            maxFrames = (int) Math.ceil( (double)(audioData.length() - frameLength) / (double) frameShift);
        } else {
            maxFrames = 1;
        }
        atFrame = 1;
        head = 0;
        double[] chromagram;
        while((tempBuffer = getNextFrame()) != null ){
	        chromagram = getChromagram(tempBuffer);
            detectedChord = detectChord(targetChords, chromagram);
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
