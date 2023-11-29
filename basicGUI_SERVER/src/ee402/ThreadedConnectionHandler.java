/* The Connection Handler Class - Written by Derek Molloy for the EE402 Module
 * See: ee402.eeng.dcu.ie
 */
package ee402;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

public class ThreadedConnectionHandler extends Thread {
    private Socket clientSocket;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private DateTimeService theDateService;
    private String deviceName;

    // New attributes for simulated sensors
    private double methaneValue;
    private double co2Value;
    private double temperatureValue;
    private Random random = new Random();

    public ThreadedConnectionHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        this.theDateService = new DateTimeService();
        this.deviceName = "Default Device";

        // Initialize simulated sensor values
        this.methaneValue = 0.0;
        this.co2Value = 0.0;
        this.temperatureValue = 0.0;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public void run() {
        try {
            this.is = new ObjectInputStream(clientSocket.getInputStream());
            this.os = new ObjectOutputStream(clientSocket.getOutputStream());

            while (this.readCommand()) {
            }
        } catch (IOException e) {
            System.out.println("XX. There was a problem with the Input/Output Communication:");
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    private boolean readCommand() {
        try {
            Object received = is.readObject();
            if (received instanceof String) {
                String command = (String) received;
                if (command.startsWith("SetDeviceName: ")) {
                    // Extract device name from the command
                    String newName = command.replace("SetDeviceName: ", "");
                    setDeviceName(newName);
                } else if (command.equals("GetDate")) {
                    this.getDate();
                }
            }
        } catch (Exception e) {
            closeSocket();
            return false;
        }
        return true;
    }

    private void getDate() {
        String theDateCommand = "GetDate";
        String theDateAndTime = theDateService.getDateAndTime();

        send(theDateAndTime);
    }

    private void send(Object o) {
        try {
            os.writeObject(o);
            os.flush();
        } catch (Exception e) {
            System.out.println("XX. Exception Occurred on Sending:" + e.toString());
        }
    }

    // Additional method to simulate sensor updates
    public void simulateSensorUpdate() {
        // Update sensor values with configurable random noise
        methaneValue = generateSensorValue(methaneValue);
        co2Value = generateSensorValue(co2Value);
        temperatureValue = generateSensorValue(temperatureValue);

        // Construct a message with sensor readings
        String updateMessage = "Sensor Update: Methane - " + methaneValue +
                ", CO2 - " + co2Value + ", Temperature - " + temperatureValue;

        // Send the message to the server
        send(updateMessage);
    }

    private double generateSensorValue(double currentValue) {
        // Add random noise to simulate sensor readings
        double noise = (random.nextDouble() - 0.5) * 2.0; // Random value between -1 and 1
        return currentValue + noise;
    }

    private void closeSocket() {
        try {
            if (os != null) os.close();
            if (is != null) is.close();
            if (clientSocket != null) clientSocket.close();
        } catch (Exception e) {
            System.out.println("XX. " + e.getStackTrace());
        }
    }
}
