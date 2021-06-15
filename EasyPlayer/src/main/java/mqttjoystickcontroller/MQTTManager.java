/**
 * This project is made by Vincenzo Petrolo on April 2020
 */
package mqttjoystickcontroller;

import android.content.Context;

import com.pgyer.pgyersdk.PgyerSDKManager;
import com.pgyersdk.crash.PgyCrashManager;

import org.easydarwin.easyplayer.activity.PlayActivity;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

import java.io.UnsupportedEncodingException;





public class MQTTManager {
    private String PROT = "tcp://";     //connection protocol
    private String userBrokerAddress;   //user broker address
    private String PORT = ":1883";      //default port for MQTT connections

    private MqttAndroidClient   mqttClient;
    private MqttConnectOptions  mqttConnectOptions;


    /**
     *  Default constructor the only difference is for the address string passed as argument
     * @param controllerActivity the activity where everything happens
     */
    public MQTTManager(PlayActivity controllerActivity, String android_id){
        this.userBrokerAddress  = "lvsrobot.cn";
        Context cntx            = controllerActivity.getApplicationContext();
        String address         = this.PROT + this.userBrokerAddress + this.PORT;
        this.mqttClient         = new MqttAndroidClient(cntx,address,"AndroidMqttController"+android_id);
        this.mqttConnectOptions = new MqttConnectOptions();
        this.mqttConnectOptions.setKeepAliveInterval(60);
        this.mqttConnectOptions.setCleanSession(true);
        this.mqttConnectOptions.setAutomaticReconnect(true);
    }

    /**
     *  Overloaded constructor which needs to be passed a string representing the address of the broker
     * @param userBrokerAddress the address of the broker
     * @param controllerActivity the activity where everything happens
     */
    public MQTTManager(String userBrokerAddress, PlayActivity controllerActivity, String android_id){
        this.userBrokerAddress  = userBrokerAddress;
        Context cntx            = controllerActivity.getApplicationContext();
        String address         = this.PROT + this.userBrokerAddress + this.PORT;
        this.mqttClient         = new MqttAndroidClient(cntx,address,"AndroidMqttController"+android_id);
        this.mqttConnectOptions = new MqttConnectOptions();
        this.mqttConnectOptions.setKeepAliveInterval(60);
        this.mqttConnectOptions.setCleanSession(true);
        this.mqttConnectOptions.setAutomaticReconnect(true);
    }

    /**
     *  Simple getter
     * @return the mqtt client object
     */
    public MqttAndroidClient getMqttClient(){
        return this.mqttClient;
    }

    /**
     *  Simple getter
     * @return the mqtt connection options
     */
    public MqttConnectOptions getMqttConnectOptions() {
        return mqttConnectOptions;
    }

    /**
     *  This method simply offers the possibility to send a message
     * @param payload to send
     * @param topic of the message
     */
    public void sendMessage(String payload, String topic){
        byte[] encodedPayload  = new byte[0];


        try {
            encodedPayload  = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);

            if (this.mqttClient.isConnected()){
                this.mqttClient.publish(topic,message);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MqttPersistenceException e) {
            e.printStackTrace();
        } catch (MqttException e) {
            e.printStackTrace();
//            PgyCrashManager.reportCaughtException(e);
            PgyerSDKManager.reportException(e);
        }
    }

    /**
     *
     * @return the default broker address
     */
    public String getUserBrokerAddress(){
        return  this.userBrokerAddress;
    }
}
