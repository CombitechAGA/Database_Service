import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.print.Doc;
import java.util.*;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class DatabaseCallback implements MqttCallback {

    private String databaseIP;
    private int databasePort;
    private MongoClient mongoClient;
    MongoDatabase currentDataBase;
    MongoCollection<Document> speedCollection;
    MongoCollection<Document> fuelCollection;
    MongoCollection<Document> distanceTraveledCollection;


    public DatabaseCallback(String databaseIP,int databasePort, String dataBaseUser, String dataBasePass, String defultDataBase, MqttClient mqttClient){
        this.databaseIP=databaseIP;
        this.databasePort=databasePort;
        char[] pass = dataBasePass.toCharArray();
        String dataBase = defultDataBase;
        String user = dataBaseUser;
        List<MongoCredential> list = new ArrayList<MongoCredential>();
        ServerAddress serverAddress = new ServerAddress(databaseIP,databasePort);
        list.add(MongoCredential.createCredential(user, dataBase, pass));
        mongoClient = new MongoClient(serverAddress, list);
        currentDataBase = mongoClient.getDatabase(defultDataBase);
        //currentDataBase.getCollection("configuration");
        //BasicDBObject whereQuery = new BasicDBObject();
        //whereQuery.put("name", "configuration");


        speedCollection = currentDataBase.getCollection("speed");
        fuelCollection = currentDataBase.getCollection("fuel");
        distanceTraveledCollection = currentDataBase.getCollection("distanceTraveled");
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
            case "request/config":
                String result="";
                String deviceID = mqttMessage.toString();

                currentDataBase = mongoClient.getDatabase("configuration");

                FindIterable iterable = currentDataBase.getCollection("configuration").find();

                Iterator iter = iterable.iterator();
                iter.next();
                Document temp;
                while (iter.hasNext()){
                    temp = (Document)iter.next();
                    for(String key : temp.keySet()){
                        result +=key+":"+temp.get(key)+";";
                    }
                }
                result = result.substring(0,result.length()-1);
                System.out.println(result);


                currentDataBase = mongoClient.getDatabase("telemetry");

                //publisha configen hit
                break;
            case "telemetry/snapshot":
                System.out.println("fick snapshot: " + mqttMessage.toString());
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
