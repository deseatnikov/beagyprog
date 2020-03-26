import org.eclipse.paho.client.mqttv3.MqttException;

public class MqttApp {
  public static void main(String[] args) throws MqttException {
    Watcher w = new Watcher(BrrStringConsts.CONNECTION_STRING, BrrStringConsts.NAME_SERVER_DEFAULT);
    while (w.isConnected()) {
      w.process();
    }
    w.disconnect();
  }
}

