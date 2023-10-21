package dev.hartcode.io;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Saving {
    // Save the state of JCheckBox objects to a file
    public static void saveFile(ArrayList<JCheckBox> checkBoxArrayList) {
        // Create a boolean array to store the state of the checkboxes
        boolean[] booleanArray = new boolean[256];

        for (int i = 0; i < 256; i++) {
            // Store the selected state of each JCheckBox in the boolean array
            booleanArray[i] = checkBoxArrayList.get(i).isSelected();
        }

        // Create a file chooser dialog for the user to select the save location
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showSaveDialog(null);

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileChooser.getSelectedFile()))) {
            // Write the boolean array to the selected file using ObjectOutputStream
            objectOutputStream.writeObject(booleanArray);
        } catch (IOException e) {
            System.out.println("File could not be saved -> " + e.getMessage());
        }
    }
}
