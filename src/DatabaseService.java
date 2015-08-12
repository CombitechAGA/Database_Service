import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class DatabaseService {
    private String broker;// = "tcp://mqtt.phelicks.net:1883";
    private String clientID;// = "DatabaseServiceClient";
    private int qos;// = 2;
    private String brokerUser;
    private String brokerPass;
    private String databaseIP;// = "81.236.122.195";
    private int databasePort;//= 27017;
    private String dataBaseUser;
    private String dataBasePass;
    private String defultDataBase;

    private ArrayList<String> topics;

    MqttClient client;

    public DatabaseService() throws MqttException {
        readConfig();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(broker, clientID, persistence);
    }

    //read config settings (IP, qos, etc,topics, address of the currentDataBase)
    private void readConfig(){
        BufferedReader br;
        ArrayList<String> list = new ArrayList<String>();
        try {
            //väldigt fult!! hitta inte filen i projektet.
            br = new BufferedReader(new FileReader(new File("config.txt").getAbsolutePath()));
            String line = br.readLine();
            int n = 0;
            while (line != null) {
                String[] parts = line.split("#");
                list.add(parts[1]);
                line = br.readLine();
            }
            broker = list.get(0);
            clientID = list.get(1);
            databaseIP = list.get(2);
            databasePort = Integer.parseInt(list.get(3));
            brokerUser = list.get(4);
            brokerPass = list.get(5);
            dataBaseUser = list.get(6);
            dataBasePass = list.get(7);
            defultDataBase = list.get(8);
            qos = Integer.parseInt(list.get(9));
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        topics = new ArrayList<String>();
        //testa med telemetry/*
        topics.add("telemetry/speed");
        topics.add("telemetry/fuel");
        topics.add("telemetry/distanceTraveled");
        topics.add("request/config");
        topics.add("telemetry/snapshot");
        topics.add("set/config");
        topics.add("new/config");
    }
    private void subscribeToTopics() throws MqttException {
        for (String topic : topics){
            client.subscribe(topic);
        }
    }

    public void start() throws MqttException {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(brokerUser);
        options.setPassword(brokerPass.toCharArray());
        client.connect(options);
        //PrintOutCallback printOutCallback = new PrintOutCallback();
        DatabaseCallback databaseCallback = new DatabaseCallback(databaseIP,databasePort, dataBaseUser, dataBasePass, defultDataBase, client);
        client.setCallback(databaseCallback);
        subscribeToTopics();


    }
}
