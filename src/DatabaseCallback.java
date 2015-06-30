import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class DatabaseCallback implements MqttCallback {

    private String databaseIP;
    private int databasePort;
    private MongoClient mongoClient;
    MongoDatabase database;
    MongoCollection<Document> speedCollection;
    MongoCollection<Document> fuelCollection;
    MongoCollection<Document> distanceTraveledCollection;


    public DatabaseCallback(String databaseIP,int databasePort, String dataBaseUser, String dataBasePass, String defultDataBase){
        this.databaseIP=databaseIP;
        this.databasePort=databasePort;
        char[] pass = dataBasePass.toCharArray();
        String dataBase = defultDataBase;
        String user = dataBaseUser;
        List<MongoCredential> list = new ArrayList<MongoCredential>();
        ServerAddress serverAddress = new ServerAddress(databaseIP,databasePort);
        list.add(MongoCredential.createCredential(user, dataBase, pass));
        mongoClient = new MongoClient(serverAddress, list);
        database = mongoClient.getDatabase("telemetry");

        speedCollection = database.getCollection("speed");
        fuelCollection = database.getCollection("fuel");
        distanceTraveledCollection = database.getCollection("distanceTraveled");
    }

    @Override
    public void connectionLost(Throwable throwable) {
        System.out.println("Connection Lost What to do now? Reconnect?");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
        System.out.println("Received a message:");
        System.out.println("Topic: "+topic);
        System.out.println("Message: "+mqttMessage.toString());

        Document doc;
        switch (topic){
            case "telemetry/speed":
                doc = new Document("name","Speed").append("info",mqttMessage.toString());
                speedCollection.insertOne(doc);
                break;
            case "telemetry/fuel":
                doc = new Document("name","fuel").append("info",mqttMessage.toString());
                fuelCollection.insertOne(doc);
                break;
            case "telemetry/distanceTraveled":
                doc = new Document("name","distanceTraveled").append("info",mqttMessage.toString());
                distanceTraveledCollection.insertOne(doc);
                break;
            default:
                System.out.println("Error unknown topic:\""+topic+"\"");
                System.exit(-1);
        }





    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}
