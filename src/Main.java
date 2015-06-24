import org.eclipse.paho.client.mqttv3.MqttException;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class Main {
    public static void main(String[] args) {
        DatabaseService databaseService = null;
        try {
            databaseService = new DatabaseService();
        } catch (MqttException e) {
            System.err.println("Error creating the mqttclient");
            e.printStackTrace();
            System.exit(1);
        }
        try {
            databaseService.start();
        } catch (MqttException e) {
            System.err.println("Error connecting the mqttclient to the broker");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
