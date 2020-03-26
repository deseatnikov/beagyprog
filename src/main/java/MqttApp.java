import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPingResp;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttPubAck;

/**
 * Basic launcher for Publisher and Subscriber
 */
public class MqttApp {
  public static void main(String[] args) throws MqttException {
    Watcher w = new Watcher("tcp://localhost:1883", "eyeOfServer");
    while (w.isConnected()) {
      w.process();
    }
    w.disconnect();
  }
}

