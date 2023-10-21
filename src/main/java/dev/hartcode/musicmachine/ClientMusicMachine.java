package dev.hartcode.musicmachine;

import dev.hartcode.io.Loading;
import dev.hartcode.io.Saving;

import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMusicMachine {
    // User interface components
    private JList<String> incomingList;
    private JTextArea userMessage;
    private ArrayList<JCheckBox> checkBoxArrayList;

    // Data structures for storing information
    private final Vector<String> listVector = new Vector<>();
    private final HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    private String userName;
    private int nextNum;
    private final int tempo = 120;

    // MIDI components
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private final String[] instrumentNames = new String[16];
    private final int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        new ClientMusicMachine().startUp();
    }

    private void startUp() {
        // Initialize instrument names
        for (int i = 0; i < 16; i++) {
            instrumentNames[i] = (i + 1) + ". ";
        }
        // Prompt for users name
        System.out.print("Please enter your name -> ");
        this.userName = new Scanner(System.in).nextLine();
        // Set up network connection
        setUpNetwork();
        // Set up MIDI
        setUpMIDI();
        // Set up the graphical user interface
        setUpGUI();
    }

    private void setUpNetwork() {
        // Define the server's socket address
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 6000);
        try {
            // Establish a socket connection with the server
            Socket socket = new Socket("127.0.0.1", 6000);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // Create a background thread for receiving data from the server
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new IncomingReader());
            System.out.println("Connection established with the server...");
        } catch (IOException e) {
            System.out.println("Could not establish a connection with the server...");
        }
    }

    private void setUpMIDI() {
        try {
            // Get the system's MIDI sequencer
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            // Create a new MIDI sequence and track
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
        } catch (Exception e) {
            System.out.println("Could not set up MIDI successfully -> " + e.getMessage());
        }
    }

    public void setUpGUI() {
        checkBoxArrayList = new ArrayList<>();
        JFrame frame = new JFrame("BeatBox 1.0");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel background = new JPanel(new BorderLayout());
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        // Start button to begin playing the beat
        JButton startButton = new JButton("Start");
        startButton.addActionListener(event -> buildTrackAndStart());
        buttonBox.add(startButton);

        // Stop button to stop playing the beat
        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(event -> sequencer.stop());
        buttonBox.add(stopButton);

        // Increase tempo button
        JButton upTempoButton = new JButton("Tempo++");
        upTempoButton.addActionListener(event -> changeTempo(1.03f));
        buttonBox.add(upTempoButton);

        // Decrease tempo button
        JButton downTempoButton = new JButton("Tempo--");
        downTempoButton.addActionListener(event -> changeTempo(0.97f));
        buttonBox.add(downTempoButton);

        // Save file button
        JButton saveButton = new JButton("Save file");
        saveButton.addActionListener(event -> Saving.saveFile(checkBoxArrayList));
        buttonBox.add(saveButton);

        // Load file button
        JButton loadButton = new JButton("Load file");
        loadButton.addActionListener(event -> {
            Loading.loadFile(checkBoxArrayList);
            sequencer.stop();
            buildTrackAndStart();
        });
        buttonBox.add(loadButton);

        // Send button to send the user's beat to the server
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(event -> sendMessageAndTrack());
        buttonBox.add(sendButton);

        // Text area for user's messages
        userMessage = new JTextArea();
        userMessage.setLineWrap(true);
        userMessage.setWrapStyleWord(true);
        JScrollPane scroller = new JScrollPane(userMessage);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        buttonBox.add(scroller);

        // List to display incoming data from the server
        incomingList = new JList<>();
        incomingList.addListSelectionListener(new MyListSelectionListener());
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        Box labelBox = new Box(BoxLayout.Y_AXIS);

        for (String instrument : instrumentNames) {
            JLabel instrumentLabel = new JLabel(instrument);
            instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
            labelBox.add(instrumentLabel);
        }
        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, labelBox);
        frame.getContentPane().add(background);

        // Grid layout for checkboxes
        GridLayout grid = new GridLayout(16, 16);
        grid.setHgap(2);
        grid.setVgap(1);
        JPanel mainPanel = new JPanel(grid);

        for (int i = 0; i < 256; i++) {
            JCheckBox checkBox = new JCheckBox();
            checkBox.setSelected(false);
            mainPanel.add(checkBox);
            checkBoxArrayList.add(checkBox);
        }
        background.add(BorderLayout.CENTER, mainPanel);

        frame.setBounds(30, 30, 300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    private void buildTrackAndStart() {
        int[] trackList;
        // Clear the existing track and create a new one
        sequence.deleteTrack(track);
        track = sequence.createTrack();

        // Iterate through 16 instruments
        for (int i = 0; i < 16; i++) {
            trackList = new int[16];
            int key = instruments[i];

            // Iterate through 16 beats
            for (int j = 0; j < 16; j++) {
                JCheckBox checkBox = checkBoxArrayList.get(j + i * 16);
                if (checkBox.isSelected()) {
                    trackList[j] = key; // Set the key if the checkbox is selected
                } else {
                    trackList[j] = 0; // Set to 0 if the checkbox is not selected
                }
            }
            makeTracks(trackList); // Create note events for the current instrument
            track.add(makeEvent(ShortMessage.CONTROL_CHANGE, 1, 127, 0, 16)); // Add control change event
        }
        track.add(makeEvent(ShortMessage.PROGRAM_CHANGE, 9, 1, 0, 15)); // Change the program (instrument)

        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(120);
            sequencer.start();
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
    }

    private void changeTempo(float tempoMultiplier) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
    }

    private void makeTracks(int[] trackList) {
        for (int i = 0; i < 16; i++) {
            if (trackList[i] != 0) {
                // Create a note on event
                track.add(makeEvent(ShortMessage.NOTE_ON, 9, trackList[i], 100, i));
                // Create a note off event
                track.add(makeEvent(ShortMessage.NOTE_OFF, 9, trackList[i], 100, i + 1));
            }
        }
    }

    private static MidiEvent makeEvent(int cmd, int channel, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            // Create a MIDI message
            ShortMessage msg = new ShortMessage();
            msg.setMessage(cmd, channel, one, two);
            // Create a MIDI event with the message
            event = new MidiEvent(msg, tick);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        }
        return event;
    }

    private void sendMessageAndTrack() {
        boolean[] checkBoxState = new boolean[256];
        for (int i = 0; i < 256; i++) {
            checkBoxState[i] = checkBoxArrayList.get(i).isSelected();
        }
        try {
            // Send user message and checkbox state to the server
            out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
            out.writeObject(checkBoxState);
        } catch (IOException e) {
            System.out.println("Failed to send message over to the server -> " + e.getMessage());
        }
        userMessage.setText("");
        userMessage.setFocusable(true);
    }


    // This class listens for changes in the selection of items in the "incomingList" JList.
    // When a new item is selected, it retrieves the associated boolean state array from the
    // "otherSeqsMap," updates the checkboxes' selected states, and triggers the "buildTrackAndStart"
    public class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                String selected = incomingList.getSelectedValue();
                if (selected != null) {
                    boolean[] selectedState = otherSeqsMap.get(selected);
                    for (int i = 0; i < 256; i++) {
                        checkBoxArrayList.get(i).setSelected(selectedState[i]);
                    }
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }

    public class IncomingReader implements Runnable {
        @Override
        public void run() {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    String nameToShow = (String) obj;
                    boolean[] checkBoxState = (boolean[]) in.readObject();
                    // Update the map with incoming data
                    otherSeqsMap.put(nameToShow, checkBoxState);
                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Unsuccessful in reading object -> " + e.getMessage());
            }
        }
    }

}
