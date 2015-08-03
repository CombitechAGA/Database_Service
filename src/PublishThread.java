/**
 * Created by thomasstrahl on 2015-08-03.
 */
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;

public class PublishThread extends Thread {
    private MqttClient client;
    private String topic;
    private String message;

    public PublishThread(MqttClient client, String topic, String message){
        this.client=client;
        this.topic=topic;
        this.message=message;
    }

    @Override
    public void run(){
        try {
            client.publish(topic,message.getBytes(),0,false);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
