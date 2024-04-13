import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;

import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.List;

public class LiveMidiParser {

    private static MidiDevice device;
    private static Info deviceInfo;
    private static Scanner scanner;

    /*
     * Converts a MIDI note number to a string representation of the note.
     * 
     * @param noteNum The MIDI note number to convert.
     * @return A string representation of the note.
     */
    public static String noteNumToStr(int noteNum) {
        String[] noteNames = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
        return noteNames[noteNum % 12] + (noteNum / 12 - 1);
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
                                System.out.println("Note ON: " + noteName);
                            }
                        } else if(midiCMD == ShortMessage.NOTE_OFF) { // Note OFF event
                            int key = sm.getData1();
                            String noteName = noteNumToStr(key);
                            System.out.println("Note OFF: " + noteName);
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
            
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}