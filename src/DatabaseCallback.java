import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;

import javax.print.Doc;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

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
    MongoCollection<Document> configCollection;
    MqttClient client;


    public DatabaseCallback( String databaseIP,int databasePort, String dataBaseUser, String dataBasePass, String defultDataBase, MqttClient mqttClient){

        this.databaseIP=databaseIP;
        this.databasePort=databasePort;
        char[] pass = dataBasePass.toCharArray();
        String dataBase = defultDataBase;
        String user = dataBaseUser;
        List<MongoCredential> list = new ArrayList<MongoCredential>();
        ServerAddress serverAddress = new ServerAddress(databaseIP,databasePort);
        //list.add(MongoCredential.createCredential(user, dataBase, pass));
        //mongoClient = new MongoClient(serverAddress, list);
        mongoClient = new MongoClient(serverAddress);
        currentDataBase = mongoClient.getDatabase(defultDataBase);
        speedCollection = currentDataBase.getCollection("speed");
        fuelCollection = currentDataBase.getCollection("fuel");
        distanceTraveledCollection = currentDataBase.getCollection("distanceTraveled");
        client = mqttClient;
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
                System.out.println("message: "+mqttMessage.toString() );
                doc = new Document("name","fuel").append("info",mqttMessage.toString());
                fuelCollection.insertOne(doc);
                break;
            case "telemetry/distanceTraveled":
                doc = new Document("name","distanceTraveled").append("info",mqttMessage.toString());
                distanceTraveledCollection.insertOne(doc);
                break;
            case "set/config":
                //alltid uppdatering av en config, int en ny.
                BasicDBObject newDocument = new BasicDBObject();
                String message = mqttMessage.toString();
                String id = message.substring(message.indexOf(":")+1,message.indexOf(";"));
                for(String keyValuePair : message.split(";")){
                    String key = keyValuePair.split(":")[0];
                    String value = keyValuePair.split(":")[1];
                    if (!key.equals("id")) {
                        newDocument.append("$set", new BasicDBObject().append(key, value));
                    }
                    System.out.println("key: "+key);
                    System.out.println("value:"+value);
                }
                currentDataBase = mongoClient.getDatabase("configuration");
                configCollection = currentDataBase.getCollection("configuration");
                BasicDBObject searchQuery = new BasicDBObject().append("id",id);

                configCollection.updateOne(searchQuery, newDocument);
                currentDataBase = mongoClient.getDatabase("telemetry");
                break;
            case "request/config":
                String deviceID = mqttMessage.toString();

                currentDataBase = mongoClient.getDatabase("configuration");
                configCollection = currentDataBase.getCollection("configuration");
                BasicDBObject requestedConfig = new BasicDBObject().append("id",deviceID);

                FindIterable<Document> config = configCollection.find(requestedConfig);
                Document configToSend = config.first();

                if (configToSend != null){
                    String result = "";
                    for (String key :configToSend.keySet()){
                        if (!key.equals("id") && !key.equals("_id")){
                            System.out.println(key);
                            System.out.println(configToSend.get(key));
                            result+=key+"#"+configToSend.get(key)+"\n";
                        }
                    }
                    System.out.println("här är vad vi skickade:" +  result);
                    new PublishThread(client,deviceID+"/config",result).start();
                }

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
        System.out.println("nu har jag skickat config!!");

    }

    public void sendConfigToClient(String clientID,Document config){
        String result = "";
        //try {
//            System.out.println(config.toJson().toString());
              for (String key :config.keySet()){
                  if (!key.equals("id") && !key.equals("_id")){
                      System.out.println(key);
                      System.out.println(config.get(key));
                      result+=key+"#"+config.get(key)+"\n";
                  }
              }
            result = result.substring(0,result.length());
            System.out.println(result);
            //client.publish(clientID+"/config",new MqttMessage(result.getBytes()));
            System.out.println("config sent!!");
       // } catch (MqttException e) {
         //   e.printStackTrace();
       // }
    }
}
