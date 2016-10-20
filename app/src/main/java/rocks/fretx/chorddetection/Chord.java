package rocks.fretx.chorddetection;

/**
 * Created by Onur Babacan on 10/20/16.
 */

public class Chord {
    public final String root;
    public final String type;
    public final String[] ALL_ROOT_NOTES = {"A","A#","B","C","C#","D","D#","E","F","G","G#"};
    public final String[] ALL_CHORD_TYPES = {"maj","m","maj7","m7","5","7","9","sus2","sus4","7sus4","7#9","add9","aug","dim","dim7"};

    public Chord(String root, String type){
        //TODO: input handling

        this.root = root;
        this.type = type;
    }

    private int noteNameToSemitoneNumber(String name){
        String newName = name;
        if(name.length() == 2){
            if(name.charAt(1) == 'b'){
                switch (name.charAt(0)){
                    case 'A': newName = "G#"; break;
                    case 'B': newName = "A#"; break;
                    case 'D': newName = "C#"; break;
                    case 'E': newName = "D#"; break;
                    case 'G': newName = "F#"; break;
                    //This shouldn't happen
                    default: break;
                }
            }
        }
        int semitone = 0;
        switch(newName){
            case "A":  semitone = 1; break;
            case "A#": semitone = 2; break;
            case "B":  semitone = 3; break;
            case "C":  semitone = 4; break;
            case "C#": semitone = 5; break;
            case "D":  semitone = 6; break;
            case "D#": semitone = 7; break;
            case "E":  semitone = 8; break;
            case "F":  semitone = 9; break;
            case "F#": semitone = 10; break;
            case "G":  semitone = 11; break;
            case "G#": semitone = 12; break;
            default: semitone = 0;
        }
        return semitone;
    }
    private String semitoneNumberToNoteName(int number){
        switch(number){
            case 1: return "A";
            case 2: return "A#";
            case 3: return "B";
            case 4: return "C";
            case 5: return "C#";
            case 6: return "D";
            case 7: return "D#";
            case 8: return "E";
            case 9: return "F";
            case 10: return "F#";
            case 11: return "G";
            case 12: return "G#";
            //This shouldn't happen
            default: return "NONE";
        }
    }

    private int[] getChordFormula(){
        //int[] majorIntervals = {2,2,1,2,2,2,1,2,2,1,2,2,2,1};
        //in MATLAB: semitoneLookup = cumsum([1 majorIntervals]);
        int[] semitoneLookup = {1,3,5,6,8,10,12,13,15,17,18,20,22,24,25};
        int[] template,modification;

        switch (type){
            case "maj" :
                template = new int[] {1,3,5};
                modification = new int[] {0,0,0};
                break;
            case "m" :
                template = new int[] {1,3,5};
                modification = new int[] {0,-1,0};
                break;
            case "maj7" :
                template = new int[] {1,3,5,7};
                modification = new int[] {0,0,0,0};
                break;
            case "m7" :
                template = new int[] {1,3,5,7};
                modification = new int[] {0,-1,0,-1};
                break;
            case "5" :
                template = new int[] {1,5};
                modification = new int[] {0,0};
                break;
            case "7" :
                template = new int[] {1,3,5,7};
                modification = new int[] {0,0,0,-1};
                break;
            case "9" :
                template = new int[] {1,3,5,7,9};
                modification = new int[] {0,0,0,-1,0};
                break;
            case "sus2" :
                template = new int[] {1,2,5};
                modification = new int[] {0,0,0};
                break;
            case "sus4" :
                template = new int[] {1,4,5};
                modification = new int[] {0,0,0};
                break;
            case "7sus4" :
                template = new int[] {1,4,5,7};
                modification = new int[] {0,0,0,-1};
                break;
            case "7#9" :
                template = new int[] {1,3,5,7,9};
                modification = new int[] {0,0,0,-1,+1};
                break;
            case "add9" :
                template = new int[] {1,3,5,9};
                modification = new int[] {0,0,0,0};
                break;
            case "aug" :
                template = new int[] {1,3,5};
                modification = new int[] {0,0,+1};
                break;
            case "dim" :
                template = new int[] {1,3,5};
                modification = new int[] {0,-1,-1};
                break;
            case "dim7" :
                template = new int[] {1,3,5,7};
                modification = new int[] {0,-1,-1,-2};
                break;
            default:
                //This shouldn't happen
                template = new int[] {0,0,0};
                modification = new int[] {0,0,0};
        }

        int[] formula = new int[template.length];

        for (int i = 0; i < formula.length; i++) {
            formula[i] = semitoneLookup[template[i]] + modification[i];
        }
        return formula;
    }

    public int[] getNotes(){
        int rootNumber = noteNameToSemitoneNumber(root);
        int[] formula = getChordFormula();
        int[] notes = new int[formula.length];
        for (int i = 0; i < notes.length ; i++) {
            notes[i] =  (formula[i] + rootNumber - 1) % 12;
            if(notes[i] == 0) notes[i] = 12;
        }
        return notes;
    }

    public String[] getNoteNames(){
        int[] notes = getNotes();
        String[] noteNames = new String[notes.length];
        for (int i = 0; i < noteNames.length; i++) {
            noteNames[i] = semitoneNumberToNoteName(notes[i]);
        }
        return noteNames;
    }
}
