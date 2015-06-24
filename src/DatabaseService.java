import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class DatabaseService {
    private String broker = "tcp://81.236.122.249:1883";
    private String clientID = "DatabaseServiceClient";
    int qos = 2;

    private String databaseIP = "81.236.122.195";
    private int databasePort = 27017;

    private ArrayList<String> topics;

    MqttClient client;

    public DatabaseService() throws MqttException {
        readConfig();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(broker, clientID, persistence);
    }

    //read config settings (IP, qos, etc,topics, address of the database)
    private void readConfig(){
        topics = new ArrayList<String>();
        //testa med telemetry/*
        topics.add("telemetry/speed");
        topics.add("telemetry/fuel");
        topics.add("telemetry/distanceTraveled");
    }
    private void subscribeToTopics() throws MqttException {
        for (String topic : topics){
            client.subscribe(topic);
        }
    }

    public void start() throws MqttException {
        client.connect();
        //PrintOutCallback printOutCallback = new PrintOutCallback();
        DatabaseCallback databaseCallback = new DatabaseCallback(databaseIP,databasePort);
        client.setCallback(databaseCallback);
        subscribeToTopics();


    }
}
