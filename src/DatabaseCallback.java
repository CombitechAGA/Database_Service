import com.mongodb.*;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;

import javax.print.Doc;
import java.text.DateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;

/**
 * Created by Fredrik on 2015-06-24.
 */
public class DatabaseCallback implements MqttCallback {

    private String message;
    private String id;
    private BasicDBObject requestedConfig;
    private String databaseIP;
    private int databasePort;
    private MongoClient mongoClient;
    MongoDatabase currentDataBase;
    MongoCollection<Document> speedCollection;
    MongoCollection<Document> fuelCollection;
    MongoCollection<Document> distanceTraveledCollection;
    MongoCollection<Document> configCollection;
    MongoCollection<Document> snapshotCollection;
    MqttClient client;
    private FindIterable<Document> config;


    public DatabaseCallback( String databaseIP,int databasePort, String dataBaseUser, String dataBasePass, String defultDataBase, MqttClient mqttClient){

        this.databaseIP=databaseIP;
        this.databasePort=databasePort;
        char[] pass = dataBasePass.toCharArray();
        String dataBase = defultDataBase;
        String user = dataBaseUser;
        List<MongoCredential> list = new ArrayList<MongoCredential>();
        ServerAddress serverAddress = new ServerAddress(databaseIP,databasePort);
        list.add(MongoCredential.createCredential(user, dataBase, pass));
        mongoClient = new MongoClient(serverAddress, list);
//        mongoClient = new MongoClient(serverAddress);
        currentDataBase = mongoClient.getDatabase(defultDataBase);
        speedCollection = currentDataBase.getCollection("speed");
        fuelCollection = currentDataBase.getCollection("fuel");
        distanceTraveledCollection = currentDataBase.getCollection("distanceTraveled");
        snapshotCollection = currentDataBase.getCollection("snapshot");
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
        String[] splittedString;
        switch (topic){
            case "telemetry/speed":
                splittedString = mqttMessage.toString().split(";");
//                doc = new Document("name","Speed").append("info",mqttMessage.toString());
                doc = new Document("name","speed").append("carID",splittedString[0]).append("timestamp", new Date(Long.parseLong(splittedString[1]))).append("speedInfo",Double.parseDouble(splittedString[2]));
                speedCollection.insertOne(doc);
                break;
            case "telemetry/fuel":
                System.out.println("message: "+mqttMessage.toString() );
                splittedString = mqttMessage.toString().split(";");
//                doc = new Document("name","fuel").append("info",mqttMessage.toString());
                doc = new Document("name","fuel").append("carID",splittedString[0]).append("timestamp",new Date(Long.parseLong(splittedString[1]))).append("fuelInfo",Double.parseDouble(splittedString[2]));
                fuelCollection.insertOne(doc);
                break;
            case "telemetry/distanceTraveled":
                splittedString = mqttMessage.toString().split(";");
//                doc = new Document("name","distanceTraveled").append("info",mqttMessage.toString());
                doc = new Document("name","distanceTraveled").append("carID",splittedString[0]).append("timestamp",new Date(Long.parseLong(splittedString[1]))).append("distanceTraveledinfo",Long.parseLong(splittedString[2]));
                distanceTraveledCollection.insertOne(doc);
                break;
            case "new/config":
                message = mqttMessage.toString();
                id = message.substring(message.indexOf(":") + 1, message.indexOf(";"));
                System.out.println("id: " + id);
                currentDataBase = mongoClient.getDatabase("configuration");
                configCollection = currentDataBase.getCollection("configuration");
                requestedConfig = new BasicDBObject().append("id", id);
                config = configCollection.find(requestedConfig);
                if (config.first() != null) {
                    System.out.println("failedToCreateNewDevice\",\"The device id must be unique\"");
                    new PublishThread(client,id+"/failedToCreateNewDevice","That device id is all ready in use.").start();
                } else {
                    doc = new Document();
                    for(String keyValuePair : message.split(";")) {
                        String key = keyValuePair.split(":")[0];
                        String value = keyValuePair.split(":")[1];
                        doc.append(key, value);
                    }
                    configCollection.insertOne(doc);
                    //String newConfig = mqttMessage.toString();
                    //String carid = newConfig.substring(newConfig.indexOf(":")+1,newConfig.indexOf(";"));
                }
                currentDataBase = mongoClient.getDatabase("telemetry");
                break;
            case "set/config":
                //alltid uppdatering av en config, int en ny.
                BasicDBObject newDocument = new BasicDBObject();
                message = mqttMessage.toString();
                id = message.substring(message.indexOf(":") + 1, message.indexOf(";"));

                requestedConfig = new BasicDBObject().append("id", id);
                config = configCollection.find(requestedConfig);
                if (config.first() == null) {
                    System.out.println(id+"/failedToUpdate");
                    new PublishThread(client,id+"/failedToUpdate","That device id does not exist. Create a new device insted.").start();
                } else {

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
                }
                currentDataBase = mongoClient.getDatabase("telemetry");
                break;
            case "request/config":
                String deviceID = mqttMessage.toString();

                currentDataBase = mongoClient.getDatabase("configuration");
                configCollection = currentDataBase.getCollection("configuration");
                requestedConfig = new BasicDBObject().append("id", deviceID);

                config = configCollection.find(requestedConfig);
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
                splittedString = mqttMessage.toString().split(";");
//                doc = new Document("name","snapshot").append("carID",splittedString[0].split("carID:")[1]).append("timestamp",splittedString[1].split("timestamp:")[1]).append("fuel",splittedString[2].split("fuel:")[1]);
//                doc.append("speed",splittedString[3].split("speed:")[1]).append("distanceTraveled",splittedString[4].split("distanceTraveled:")[1]).append("longitude",splittedString[5].split("longitude")[1]);
//                doc.append("latitude",splittedString)
                doc = new Document("name","snapshot");
                for(String keyAndValue : splittedString){
                    String[] keyAndValueSplitted=keyAndValue.split(":");
                    String key = keyAndValueSplitted[0];
                    String value = keyAndValueSplitted[1];
                    System.out.println("keyAndValue:"+keyAndValue);
                    System.out.println("key: "+key);
                    System.out.println("value:"+value);

 //                    if(!key.equals("carID")){
//                        doc.append(key,keyAndValueSplitted[1]);
//                    }
//                    else{
//                        doc.append(key,keyAndValue.substring(4));
//                    }
                    switch (key) {
                        case "carID":
                            doc.append(key,keyAndValue.substring(4));
                            break;

                        case "timestamp":
                            doc.append(key,new Date(Long.parseLong(value)));
                            break;
                        case "fuel":
                            doc.append(key,Double.parseDouble(value));
                            break;
                        case "speed":
                            doc.append(key,Double.parseDouble(value));
                            break;
                        case "distanceTraveled":
                            doc.append(key,Long.parseLong(value));
                            break;
                        case "longitude":
                            doc.append(key, Double.parseDouble(value));
                            break;
                        case "latitude":
                            doc.append(key,Double.parseDouble(value));
                            break;
                        case "zbeename":
                            doc.append(key,value);
                            break;
                        default:
                            doc.append(key,value);
                            break;
                    }

                }

                snapshotCollection.insertOne(doc);
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
