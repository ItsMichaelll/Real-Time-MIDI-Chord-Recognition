import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;

import java.util.ArrayList;
import java.util.Collections;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class LiveMidiParser {

    // Generates an array list of all possible notes
    private static final String[] noteNames = new String[] {
        "C1", "C#1", "D1", "D#1", "E1", "F1", "F#1", "G1", "G#1", "A1", "A#1", "B1",
        "C2", "C#2", "D2", "D#2", "E2", "F2", "F#2", "G2", "G#2", "A2", "A#2", "B2",
        "C3", "C#3", "D3", "D#3", "E3", "F3", "F#3", "G3", "G#3", "A3", "A#3", "B3",
        "C4", "C#4", "D4", "D#4", "E4", "F4", "F#4", "G4", "G#4", "A4", "A#4", "B4",
        "C5", "C#5", "D5", "D#5", "E5", "F5", "F#5", "G5", "G#5", "A5", "A#5", "B5",
        "C6", "C#6", "D6", "D#6", "E6", "F6", "F#6", "G6", "G#6", "A6", "A#6", "B6",
        "C7", "C#7", "D7", "D#7", "E7", "F7", "F#7", "G7", "G#7", "A7", "A#7", "B7",
        "C8"
    };
    
    private static CopyOnWriteArrayList<String> notesPressed = new CopyOnWriteArrayList<>();

    private static MidiDevice device;
    private static Info deviceInfo;
    private static Scanner scanner;

    /*
     * Sets up a shutdown hook for a graceful exit.
     * Also closes the MIDI device and scanner.
     */
    public static void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nExiting...");
            System.out.println("Exited gracefully.");
            if(device != null && device.isOpen()) {
                device.close();
            }
            if(scanner != null) {
                scanner.close();
            }
        }));
    }

    /*
     * Converts a MIDI note number to a string representation of the note.
     * 
     * @param noteNum The MIDI note number to convert.
     * @return A string representation of the note.
     */
    public static String noteNumToStr(int noteNum) {
        int octave = noteNum / 12 - 1;
        String note = noteNames[noteNum % 12];
        if(note.length() > 2) {
            return note.substring(0, 2) + octave;
        } else {
            return note.substring(0, 1) + octave;
        }
    }

    public static int noteToIndex(String note) {
        // Separate the note name and the octave
        int pos = 0;
        while(pos < note.length() && !Character.isDigit(note.charAt(pos))) {
            pos++;
        }

        String noteName = note.substring(0, pos);
        int octave = Integer.parseInt(note.substring(pos));

        // Get the index of the note in the noteNames array
        int noteIndex = -1;
        for(int i=0; i<noteNames.length; i++) {
            if(noteNames[i].equals(noteName + octave)) {
                noteIndex = i;
                break;
            }
        }

        if(noteIndex!=-1) {
            return noteIndex;
        } return -1;
    }

    public static String distanceToInterval(int distance) {
        switch(distance) {
            case 0:
                return "P1";
            case 1:
                return "m2";
            case 2:
                return "M2";
            case 3:
                return "m3";
            case 4:
                return "M3";
            case 5:
                return "P4";
            case 6:
                return "TT";
            case 7:
                return "P5";
            case 8:
                return "m6";
            case 9:
                return "M6";
            case 10:
                return "m7";
            case 11:
                return "M7";
            case 12:
                return "P8";
            case 13:
                return "m9";
            case 14:
                return "M9";
            default:
                return "Unknown";
        }
    }

    public static String[] getIntervals() {
        Collections.sort(notesPressed, (note1, note2) -> Integer.compare(noteToIndex(note1), noteToIndex(note2)));
        String[] intervals = new String[notesPressed.size()];

        if(notesPressed.size() < 2) {
            return intervals;
        }
        
        System.out.println("\n" + notesPressed);

        for(int i=0; i<notesPressed.size()-1; i++) {
            String note1 = notesPressed.get(0);
            String note2 = notesPressed.get(i+1);

            // Get the interval between the notes (e.g. C-E is a M3)
            int note1Index = noteToIndex(note1);
            int note2Index = noteToIndex(note2);

            // Switch the notes if the second note is lower than the first
            if(note1Index > note2Index) {
                int temp = note1Index;
                note1Index = note2Index;
                note2Index = temp;
            }

            int interval = note2Index - note1Index;


            if(interval < 0) { interval += 12; }
            intervals[i] = distanceToInterval(interval);
        } return intervals;
    }

    /*
     * Identifies the chord based on the notes pressed.
     */
    public static String identifyChord(String[] intervals) {
        String rootNote = notesPressed.get(0);
        rootNote = rootNote.substring(0, rootNote.length()-1);
        String chordType = "Unknown";
        
        try {
            // Remove null values from the intervals array
            List<String> intervalsList = new ArrayList<>();
            for(String interval : intervals) {
                if(interval != null) {
                    intervalsList.add(interval);
                }
            } intervals = intervalsList.toArray(new String[intervalsList.size()]);

            if(intervals.length < 2) {
                return "Unknown";
            }

            // Triads
            else if(intervals.length == 2) {
                String i1 = intervals[0];
                String i2 = intervals[1];

                if(i1.equals("M3") && i2.equals("P5")) { chordType = "Major"; }
                else if(i1.equals("m3") && i2.equals("P5")) { chordType = "Minor"; }
                else if(i1.equals("M3") && i2.equals("TT")) { chordType = "Augmented"; }
                else if(i1.equals("m3") && i2.equals("TT")) { chordType = "Diminished"; }
            }

            // Seventh chords
            else if(intervals.length == 3) {
                String i1 = intervals[0];
                String i2 = intervals[1];
                String i3 = intervals[2];

                if(i1.equals("M3") && i2.equals("P5") && i3.equals("m7")) { chordType = "Dominant 7th"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("M7")) { chordType = "Major 7th"; }
                else if(i1.equals("m3") && i2.equals("P5") && i3.equals("m7")) { chordType = "Minor 7th"; }
                else if(i1.equals("m3") && i2.equals("P5") && i3.equals("M7")) { chordType = "Minor Major 7th"; }
                else if(i1.equals("M3") && i2.equals("TT") && i3.equals("m7")) { chordType = "Augmented 7th"; }
                else if(i1.equals("m3") && i2.equals("TT") && i3.equals("m7")) { chordType = "Half-Diminished 7th"; }
                else if(i1.equals("m3") && i2.equals("TT") && i3.equals("M7")) { chordType = "Diminished 7th"; }
            }
            
            // Extended chords
            else if(intervals.length == 4) {
                String i1 = intervals[0];
                String i2 = intervals[1];
                String i3 = intervals[2];
                String i4 = intervals[3];

                if(i1.equals("M3") && i2.equals("P5") && i3.equals("m7") && i4.equals("M9")) { chordType = "Dominant 9th"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("M7") && i4.equals("M9")) { chordType = "Major 9th"; }
                else if(i1.equals("m3") && i2.equals("P5") && i3.equals("m7") && i4.equals("M9")) { chordType = "Minor 9th"; }
                else if(i1.equals("m3") && i2.equals("P5") && i3.equals("M7") && i4.equals("M9")) { chordType = "Minor Major 9th"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("m7") && i4.equals("m9")) { chordType = "Dominant 7b9"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("m7") && i4.equals("M9")) { chordType = "Dominant 7#9"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("m7") && i4.equals("m9")) { chordType = "Dominant 7b9"; }
                else if(i1.equals("M3") && i2.equals("P5") && i3.equals("M7") && i4.equals("m9")) { chordType = "Dominant 7#9"; }
                else if(i1.equals("M3") && i2.equals("TT") && i3.equals("m7") && i4.equals("M9")) { chordType = "Augmented 9th"; }
                else if(i1.equals("m3") && i2.equals("TT") && i3.equals("m7") && i4.equals("M9")) { chordType = "Half-Diminished 9th"; }
                else if(i1.equals("m3") && i2.equals("TT") && i3.equals("M7") && i4.equals("M9")) { chordType = "Diminished 9th"; }
            }
        } catch(NullPointerException e) {
            // No notes have been pressed
        }

        

        return rootNote + " " + chordType;
    }

    /*
     * Gets all valid MIDI devices connected to the system.
     * A MIDI device is considered valid if it has at least one transmitter.
     * 
     * @return A list of valid MIDI devices.
     */
    public static List<Info> getValidMidiDevices()
      throws MidiUnavailableException {
        // Get all MIDI devices connected that have transmitters
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        List<Info> validDevices = new ArrayList<>();
        for(Info info : infos) {
            MidiDevice device = MidiSystem.getMidiDevice(info);
            if(device.getMaxTransmitters() != 0) {
                validDevices.add(info);
            }
        } return validDevices;
    }

    /*
     * Main method.
     */
    public static void main(String[] args) {   
        setupShutdownHook();     
        System.out.println("Welcome to the MIDI parser! Press CTRL+C at any time to exit.\n");
        
        try {
            // Get valid connected MIDI devices
            List<Info> availableDevices = getValidMidiDevices();

            if(availableDevices.isEmpty()) {
                System.out.println("No valid MIDI devices were detected. Please connect a MIDI device and restart the program.");
                System.out.println("Exiting...");
                System.out.println("Exited gracefully.");
                return;
            }

            // Set up the MIDI receiver
            Receiver receiver = new Receiver() {
                
                public void send(MidiMessage msg, long timeStamp) {
                    if(msg instanceof ShortMessage) {
                        ShortMessage sm = (ShortMessage) msg;
                        int midiCMD = sm.getCommand();

                        if(midiCMD == ShortMessage.NOTE_ON) { // Note ON event
                            int key = sm.getData1();
                            String noteName = noteNumToStr(key);
                            if(sm.getData2() > 0) {
                                // Add the note to the list of notes pressed
                                notesPressed.add(noteName);
                            }
                        } else if(midiCMD == ShortMessage.NOTE_OFF) { // Note OFF event
                            int key = sm.getData1();
                            String noteName = noteNumToStr(key);
                            // Remove the note from the list of notes pressed
                            notesPressed.remove(noteName);
                        }
                    }
                }

                public void close() {}

            };
            
            // Provide user with a list of available MIDI ports
            System.out.println("Available MIDI devices are:");
            int i = 0;
            for(Info device : availableDevices) {
                System.out.println((i + 1) + ". " + device.getName());
                i++;
            }

            // Prompt user to select a MIDI device
            scanner = new Scanner(System.in);
            int portIndex = -1;

            while(true) {
                try {
                    // Get the user's selection
                    System.out.print("\nSelect a MIDI port to use: ");
                    portIndex = scanner.nextInt() - 1;

                    // Open the selected MIDI port
                    deviceInfo = availableDevices.get(portIndex);
                    device = MidiSystem.getMidiDevice(deviceInfo);
                    device.open();
        
                    // Set the receiver
                    device.getTransmitter().setReceiver(receiver);
                    break;
                }
                catch(IndexOutOfBoundsException e) {
                    // User selected an invalid MIDI port
                    System.out.println("Error: Invalid MIDI port. Please select a valid MIDI port.");
                }
                catch(InputMismatchException IME) {
                    // User entered a non-integer value
                    System.out.println("Error: Invalid input. Please enter a valid integer.");
                    scanner.nextLine(); // Clear the buffer
                }
                catch(NoSuchElementException NSEE) {
                    // Likely CTRL+C, terminate the program
                    System.out.println();
                    return;
                }
            }

            System.out.println("\nListening to MIDI port " + deviceInfo.getName());
            System.out.println("Press CTRL+C to exit.\n");

            // Main loop
            while(true) {
                try {
                    // Get the intervals between the notes
                    String[] intervals = getIntervals();
                    if(intervals.length > 0 && intervals[0] != null) {
                        // remove the null values from the intervals array
                        List<String> intervalsList = new ArrayList<>();
                        for(String interval : intervals) {
                            if(interval != null) {
                                intervalsList.add(interval);
                            }
                        } intervals = intervalsList.toArray(new String[intervalsList.size()]);
                        System.out.println("Intervals: " + String.join(" + ", intervals));
                        // Try to identify the chord
                        String chord = identifyChord(intervals);
                        if(!chord.equals("Unknown")) { System.out.println("Chord: " + chord); }

                        // Wait for a bit before checking again
                        Thread.sleep(1000);
                    }
                } catch(NegativeArraySizeException e) {
                    // No notes have been pressed
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}