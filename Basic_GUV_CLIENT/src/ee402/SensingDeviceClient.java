package ee402;

import java.net.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

public class SensingDeviceClient {
    private static int portNumber = 5050;
    private Socket socket = null;
    private ObjectOutputStream os = null;
    private ObjectInputStream is = null;
    
    private String deviceName = "Default Device";
    private JTextField deviceNameField;
    private JLabel sensorLabel;
    private JButton updateButton;
    private Timer updateTimer;

    // Other sensor-related variables
    private double methaneValue;
    private double co2Value;
    private double temperatureValue;
    private Random random = new Random();
    
    public SensingDeviceClient(String serverIP) {
        if (!connectToServer(serverIP)) {
            System.out.println("XX. Failed to open socket connection to: " + serverIP);
            return;
        }

        // Initialize GUI
        initializeGUI();

        // Start the timer for sending updates every 10 seconds
        startUpdateTimer();
    }

    private boolean connectToServer(String serverIP) {
        try {
            this.socket = new Socket(serverIP, portNumber);
            this.os = new ObjectOutputStream(this.socket.getOutputStream());
            this.is = new ObjectInputStream(this.socket.getInputStream());
            System.out.println("00. -> Connected to Server:" + this.socket.getInetAddress()
                    + " on port: " + this.socket.getPort());
            System.out.println("    -> from local address: " + this.socket.getLocalAddress()
                    + " and port: " + this.socket.getLocalPort());
        } catch (Exception e) {
            System.out.println("XX. Failed to Connect to the Server at port: " + portNumber);
            System.out.println("    Exception: " + e.toString());
            return false;
        }
        return true;
    }

    private void initializeGUI() {
        // Create and set up the GUI components
        JFrame frame = new JFrame("Sensing Device Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(4, 1));

        // Add a text field to change the device name
        deviceNameField = new JTextField(deviceName);
        frame.add(deviceNameField);

        // Add a label to display sensor readings
        sensorLabel = new JLabel("Sensor Readings: Methane - " + methaneValue +
                                 ", CO2 - " + co2Value + ", Temperature - " + temperatureValue);
        frame.add(sensorLabel);

        // Add a button to manually trigger sensor updates
        updateButton = new JButton("Update Sensor Readings");
        updateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendSensorUpdate();
            }
        });
        frame.add(updateButton);

        // Add an X button to disconnect from the server and close the application
        JButton disconnectButton = new JButton("X");
        disconnectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                disconnectAndClose();
            }
        });
        frame.add(disconnectButton);

        // Display the frame
        frame.pack();
        frame.setVisible(true);
    }

    private void startUpdateTimer() {
        // Use a Timer to send updates every 10 seconds
        updateTimer = new Timer(10000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendSensorUpdate();
            }
        });
        updateTimer.start();
    }

    private void sendSensorUpdate() {
        // Update sensor values with configurable random noise
        methaneValue = generateSensorValue(methaneValue);
        co2Value = generateSensorValue(co2Value);
        temperatureValue = generateSensorValue(temperatureValue);

        // Construct a message with sensor readings
        String updateMessage = "Sensor Update: Methane - " + methaneValue +
                               ", CO2 - " + co2Value + ", Temperature - " + temperatureValue;

        // Send the message to the server
        send(updateMessage);

        // Update the GUI with the new sensor readings
        updateSensorLabel();
    }

    private double generateSensorValue(double currentValue) {
        // Add random noise to simulate sensor readings
        double noise = (random.nextDouble() - 0.5) * 2.0; // Random value between -1 and 1
        return currentValue + noise;
    }

    private void updateSensorLabel() {
        // Update the GUI label with the latest sensor readings
        sensorLabel.setText("Sensor Readings: Methane - " + methaneValue +
                            ", CO2 - " + co2Value + ", Temperature - " + temperatureValue);
    }

    private void send(Object o) {
        try {
            System.out.println("02. -> Sending (" + o + ") to the server.");
            this.os.writeObject(o);
            this.os.flush();
        } catch (Exception e) {
            System.out.println("XX." + e.getStackTrace());
        }
    }

    private void disconnectAndClose() {
        // Stop the update timer
        updateTimer.stop();

        // Close the socket and the application
        closeSocket();
        System.exit(0);
    }

    private void closeSocket() {
        // Gracefully close the socket connection
        try {
            this.os.close();
            this.is.close();
            this.socket.close();
        } catch (Exception e) {
            System.out.println("XX. " + e.getStackTrace());
        }
    }

    public static void main(String args[]) {
    	String ip = "192.168.137.1";
        System.out.println("**. Java Sensing Device Client Application - EE402 OOP Module, DCU");
        if (ip != "") {
            SensingDeviceClient sensingDevice = new SensingDeviceClient(ip);
        } else {
            System.out.println("Error: you must provide the address of the server");
            System.out.println("Usage is:  java SensingDeviceClient x.x.x.x  (e.g., java SensingDeviceClient 192.168.7.2)");
            System.out.println("      or:  java SensingDeviceClient hostname (e.g., java SensingDeviceClient localhost)");
        }
        System.out.println("**. End of Application.");
    }
}
