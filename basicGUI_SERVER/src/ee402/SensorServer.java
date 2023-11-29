package ee402;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class SensorServer {
    private static final int PORT_NUMBER = 5050;
    private ServerSocket serverSocket;
    Map<String, SensorDevice> connectedDevices;
    private List<Double> methaneReadings;
    private List<Double> co2Readings;
    private List<Double> temperatureReadings;
    private Timer analysisTimer;

    private JFrame frame;
    private JTextArea statusTextArea;
    private JTextArea analysisTextArea;
    private Gauge methaneGauge;
    private Gauge co2Gauge;
    private Gauge temperatureGauge;

    public SensorServer() {
        connectedDevices = new HashMap<>();
        methaneReadings = new ArrayList<>();
        co2Readings = new ArrayList<>();
        temperatureReadings = new ArrayList<>();

        initializeGUI();
        startServer();
        startAnalysisTimer();
    }

    private void initializeGUI() {
        frame = new JFrame("Sensor Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new GridLayout(2, 2));

        // Status area
        statusTextArea = new JTextArea();
        JScrollPane statusScrollPane = new JScrollPane(statusTextArea);
        statusScrollPane.setBorder(BorderFactory.createTitledBorder("Connected Devices"));
        frame.add(statusScrollPane);

        // Analysis area
        analysisTextArea = new JTextArea();
        JScrollPane analysisScrollPane = new JScrollPane(analysisTextArea);
        analysisScrollPane.setBorder(BorderFactory.createTitledBorder("Analysis Results"));
        frame.add(analysisScrollPane);

        // Gauges
        methaneGauge = new Gauge("Methane");
        frame.add(methaneGauge);
        co2Gauge = new Gauge("CO2");
        frame.add(co2Gauge);
        temperatureGauge = new Gauge("Temperature");
        frame.add(temperatureGauge);

        // X button to shut down the server gracefully
        JButton closeButton = new JButton("X");
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shutDownServer();
            }
        });
        frame.add(closeButton);

        frame.pack();
        frame.setVisible(true);
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
            statusTextArea.append("Server started on port: " + PORT_NUMBER + "\n");
            acceptConnections();
        } catch (IOException e) {
            statusTextArea.append("Error: Unable to start server on port " + PORT_NUMBER + "\n");
        }
    }

    private void acceptConnections() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    handleNewConnection(clientSocket);
                } catch (IOException e) {
                    statusTextArea.append("Error accepting connection.\n");
                }
            }
        }).start();
    }

    private void handleNewConnection(Socket clientSocket) {
        SensorDevice sensorDevice = new SensorDevice(clientSocket, this);
        connectedDevices.put(sensorDevice.getDeviceName(), sensorDevice);
        updateConnectedDevices();
        new Thread(sensorDevice).start();
    }

    void updateConnectedDevices() {
        SwingUtilities.invokeLater(() -> {
            statusTextArea.setText("");
            for (SensorDevice device : connectedDevices.values()) {
                statusTextArea.append(device.getDeviceName() + " - Connected\n");
            }
        });
    }

    public void updateSensorReading(String deviceName, double methane, double co2, double temperature) {
        methaneReadings.add(methane);
        co2Readings.add(co2);
        temperatureReadings.add(temperature);

        updateGauges();
        updateAnalysis();
    }

    private void updateGauges() {
        SwingUtilities.invokeLater(() -> {
            methaneGauge.updateValues(getAverage(methaneReadings), getMin(methaneReadings), getMax(methaneReadings));
            co2Gauge.updateValues(getAverage(co2Readings), getMin(co2Readings), getMax(co2Readings));
            temperatureGauge.updateValues(getAverage(temperatureReadings), getMin(temperatureReadings), getMax(temperatureReadings));
        });
    }

    private double getAverage(List<Double> readings) {
        return readings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double getMin(List<Double> readings) {
        return readings.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
    }

    private double getMax(List<Double> readings) {
        return readings.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
    }

    private void updateAnalysis() {
        SwingUtilities.invokeLater(() -> {
            analysisTextArea.setText("");
            if (methaneReadings.size() > 10) {
                methaneReadings.remove(0);
            }
            if (co2Readings.size() > 10) {
                co2Readings.remove(0);
            }
            if (temperatureReadings.size() > 10) {
                temperatureReadings.remove(0);
            }

            analysisTextArea.append("Methane Average: " + getAverage(methaneReadings) + "\n");
            analysisTextArea.append("CO2 Average: " + getAverage(co2Readings) + "\n");
            analysisTextArea.append("Temperature Average: " + getAverage(temperatureReadings) + "\n");
        });
    }

    private void startAnalysisTimer() {
        analysisTimer = new Timer();
        analysisTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAnalysis();
            }
        }, 0, 10000); // Update every 10 seconds
    }

    private void shutDownServer() {
        analysisTimer.cancel();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SensorServer());
    }
}

class Gauge extends JPanel {
    private String sensorType;
    private JLabel titleLabel;
    private JLabel averageLabel;
    private JLabel minLabel;
    private JLabel maxLabel;

    public Gauge(String sensorType) {
        this.sensorType = sensorType;
        initialize();
    }

    private void initialize() {
        setLayout(new GridLayout(4, 1));
        setBorder(BorderFactory.createTitledBorder(sensorType + " Gauge"));

        titleLabel = new JLabel(sensorType);
        averageLabel = new JLabel("Average: 0.0");
        minLabel = new JLabel("Min: 0.0");
        maxLabel = new JLabel("Max: 0.0");

        add(titleLabel);
        add(averageLabel);
        add(minLabel);
        add(maxLabel);
    }

    public void updateValues(double average, double min, double max) {
        SwingUtilities.invokeLater(() -> {
            averageLabel.setText("Average: " + average);
            minLabel.setText("Min: " + min);
            maxLabel.setText("Max: " + max);
        });
    }
}

class SensorDevice implements Runnable {
    private Socket clientSocket;
    private SensorServer server;
    private ObjectOutputStream os;
    private ObjectInputStream is;
    private String deviceName;

    public SensorDevice(Socket clientSocket, SensorServer server) {
        this.clientSocket = clientSocket;
        this.server = server;
        this.deviceName = "Unknown Device";
    }

    public String getDeviceName() {
        return deviceName;
    }

    @Override
    public void run() {
        try {
            os = new ObjectOutputStream(clientSocket.getOutputStream());
            is = new ObjectInputStream(clientSocket.getInputStream());

            // Send an initial acknowledgment to the client
            send("Connection established. Welcome, " + deviceName + "!");

            while (true) {
                Object received = is.readObject();
                if (received instanceof String) {
                    String command = (String) received;
                    if (command.startsWith("Sensor Update: ")) {
                        // Extract sensor values from the update command
                        String[] values = command.replace("Sensor Update: ", "").split(", ");
                        double methane = Double.parseDouble(values[0].split(" - ")[1]);
                        double co2 = Double.parseDouble(values[1].split(" - ")[1]);
                        double temperature = Double.parseDouble(values[2].split(" - ")[1]);

                        // Notify the server about the new sensor readings
                        server.updateSensorReading(deviceName, methane, co2, temperature);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            // Handle disconnection or errors
            server.connectedDevices.remove(deviceName);
            server.updateConnectedDevices();
            closeSocket();
        }
    }

    private void send(Object o) {
        try {
            os.writeObject(o);
            os.flush();
        } catch (IOException e) {
            // Handle sending errors
        }
    }

    private void closeSocket() {
        try {
            os.close();
            is.close();
            clientSocket.close();
        } catch (IOException e) {
            // Handle closing errors
        }
    }
}

