package rocks.fretx.chorddetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

//Plot


//TODO: add BSD 2-clause license for JTransforms

public class MainActivity extends AppCompatActivity {

    private final int PERMISSION_CODE_RECORD_AUDIO = 42; //This is arbitrary, so why not The Answer to Life, Universe, and Everything?
    AudioInputHandler audioInputHandler;
    private Thread audioThread;
    private Thread guiThread;
    private boolean processingIsRunning = false;
    private boolean practiceMode = true;
    private TextView chordText;
	private TextView volumeText;
    ChordDetector chordDetector;
    ArrayList<Chord> targetChords = new ArrayList<Chord>(0);


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String[] majorRoots = new String[] {"A","C","D","E","F","G"};
        String[] minorRoots = new String[] {"A","B","D","E"};
        for (int i = 0; i < majorRoots.length; i++) {
            targetChords.add(new Chord(majorRoots[i],"maj"));
        }
        for (int i = 0; i < minorRoots.length; i++) {
            targetChords.add(new Chord(minorRoots[i],"m"));
        }
        initGui();
    }

    private void initGui(){
        //Initialize GUI
        //All the findViewById()'s go here]
        chordText = (TextView) findViewById(R.id.chordText);
        volumeText = (TextView) findViewById(R.id.volumeText);

    }

    protected void onStart(){
        super.onStart();
    }

    protected void onStop(){
        super.onStop();
    }

    protected void onPause(){
        super.onPause();
    }

    protected void onResume(){
        super.onResume();
        //Ask for runtime permissions
        boolean permissionsGranted = askForPermissions();
        Log.d("onResume","permissionsGranted: " + permissionsGranted);
        if(permissionsGranted) {
            Log.d("onResume","resuming");
            startProcessing();
        }
    }

    protected void onDestroy(){
        super.onDestroy();
        stopProcessing();
    }

    private void startAudioThread(){
        //Audio Parameters
        int maxFs = AudioInputHandler.getMaxSamplingFrequency();
//        if(maxFs > 8000){
//            maxFs = 8000;
//        }

        int minBufferSize = AudioInputHandler.getMinBufferSize(maxFs);
        double bufferSizeInSeconds = 0.25;
        int targetBufferSize = (int) Math.round(maxFs * bufferSizeInSeconds);
        int audioBufferSize = (int) Math.pow(2,Math.ceil(Math.log((double)targetBufferSize)/Math.log(2))); //round up to nearest power of 2

        int frameLength = audioBufferSize/2;

        audioInputHandler = new AudioInputHandler(maxFs,audioBufferSize);

        //Create new chord detector
        chordDetector = new ChordDetector(audioInputHandler.samplingFrequency, frameLength, frameLength/4, targetChords);
        //Patch it to audio handler
        audioInputHandler.addAudioAnalyzer(chordDetector);



        //Start the audio thread
        audioThread = new Thread(audioInputHandler,"Audio Thread");
        audioThread.start();
        Log.d("Audio thread","started");
    }

    private void startGuiThread(){
        guiThread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        //Even though YIN is producing a pitch estimate every 16ms, that's too fast for the UI on some devices
                        //So we set it to 25ms, which is good enough
                        Thread.sleep(25);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                            //TODO: GUI stuff
//	                            volumeText.setText(Double.toString(chordDetector.volume));
                                if(chordDetector.detectedChord != null && chordDetector.volume > 0.03 ){
                                    chordText.setText(chordDetector.detectedChord.getChordString());
//                                    Log.d("detected chord:",chordDetector.detectedChord.getChordString());
                                } else {
	                                chordText.setText("");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        guiThread.start();
        Log.d("GUI Thread","started");

    }

    private void startProcessing(){
        Log.d("startProcessing","method called");
        if(!processingIsRunning){
            startAudioThread();
            startGuiThread();
            processingIsRunning = true;
            Log.d("startProcessing","processes started");
        }

    }

    private void stopProcessing(){
        Log.d("stopProcessing","method called");
        if(processingIsRunning){
            if(audioInputHandler != null){
                audioInputHandler.onDestroy();
                audioInputHandler = null;
            }
            if(audioThread != null){
                try {
                    audioThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                audioThread = null;
            }
            if(guiThread != null){
                try {
                    guiThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                guiThread = null;
            }

            //Also release audioAnalyzers here, if any

            processingIsRunning = false;
            Log.d("stopProcessing","processes stopped");
        }

    }

    //Permissions
    private boolean askForPermissions(){
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECORD_AUDIO)){
                //If the user has denied the permission previously your code will come to this block
                //Here you can explain why you need this permission
                //Explain here why you need this permission
            }
            //And finally ask for the permission
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_CODE_RECORD_AUDIO);
            return false;
        }
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Checking the request code of our request
        if(requestCode == PERMISSION_CODE_RECORD_AUDIO){
            //If permission is granted
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startProcessing();
            }else{
                //Displaying another toast if permission is not granted
                Toast.makeText(this,"FretX Note Detector cannot work without this permission. Restart the app to ask for it again.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
