package dev.hartcode.io;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class Loading {
    // Load a boolean array from a file and update the state of JCheckBox objects
    public static void loadFile(ArrayList<JCheckBox> checkBoxArrayList) {

        boolean[] booleanArray = new boolean[256];

        // Create a file chooser dialog for the user to select the file to load
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showOpenDialog(null);

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileChooser.getSelectedFile()))){
            // Read the boolean array from the selected file using ObjectInputStream
            booleanArray = (boolean[]) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("File could not be loaded -> " + e.getMessage());
        }

        // Set the state of JCheckBox objects based on the loaded boolean array
        for (int i = 0; i < 256; i++) {
            checkBoxArrayList.get(i).setSelected(booleanArray[i]);
        }
    }
}
