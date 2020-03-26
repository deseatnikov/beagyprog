import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Watcher implements MqttCallbackExtended {
    private MqttClient client;
    private String defaultServerTopic = "server";
    private List<MqttTopicData> subbedTopics = new ArrayList<>();

    private int armageddon = 0;

    public Watcher(String ip, String name) throws MqttException {
        client = new MqttClient(ip, name);
        client.connect();
        client.setCallback(this);
        subToTopicIfAble("Login", MqttTopicType.global);
        subToTopicIfAble("Starter", MqttTopicType.global);
        subToTopicIfAble("Loaded", MqttTopicType.global);
        subToTopicIfAble("Armageddon", MqttTopicType.global);
        publishMessage("Szerver készen áll!");
    }

    private void saveStatus(String topic, String status) {
        subbedTopics.stream().filter(x -> x.getTopicName().equals(topic)).findFirst()
                .ifPresent(x -> x.setSavedStatus(status));
    }

    private void subToTopicIfAble(String topic, MqttTopicType type) throws MqttException {
        if (subbedTopics.stream().noneMatch(x -> x.getTopicName().equals(topic))) {
            client.subscribe(topic);
            subbedTopics.add(new MqttTopicData(topic, type));
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void process() throws MqttException {
        if (0 < armageddon) {
            publishMessage("ARMAGEDDON!!! (" + armageddon + ")");
            if (1 == armageddon--) {
                restoreAllClientStatuses();
                publishMessage("A munka sajnos nem állhat meg! Vissza mindenki!");
            }
        }
    }

    public void publishMessage(String message) throws MqttException {
        publishMessage(message, defaultServerTopic);
    }

    public void publishMessage(String message, String topic) throws MqttException {
        client.publish(topic, new MqttMessage(message.getBytes()));
    }

    public void disconnect() throws MqttException {
        if (null != client) {
            client.disconnect();
        }
    }

    @Override
    public void connectComplete(boolean b, String s) {
    }

    @Override
    public void connectionLost(Throwable throwable) {
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        if (s.equals("Login")) {
            handleLogin(mqttMessage);
        } else if (s.equals("Starter")) {
            handleStart(mqttMessage);
        } else if (s.equals("Armageddon")) {
            handleArmageddon();
        } else if (s.equals("Loaded")) {
            handleLoaded(mqttMessage);
        }
    }

    private void handleLogin(MqttMessage msg) throws MqttException {
        setClientStatus("Parked", msg.toString());
        saveStatus(msg.toString(), "Parked");
    }

    private void handleStart(MqttMessage msg) throws MqttException {
        setClientStatus("Started", msg.toString());
        saveStatus(msg.toString(), "Started");
        getAllClients().stream().filter(x -> x.getSavedStatus().equals("Parked"))
                .forEach(x -> {
                    try {
                        // exception too many publishes
                        setClientStatusNoLog("Halt", x.getTopicName());
                        x.setSavedStatus("Halt");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void handleLoaded(MqttMessage msg) throws MqttException {
        String id = msg.toString().split(",")[0];
        String csomagid = msg.toString().split(",")[1];
        setClientStatus("Work In Progress", id);
        saveStatus(id, "Work In Progress");
        getAllClients().stream().filter(x -> x.getSavedStatus().equals("Halt"))
                .forEach(x ->{
                    try {
                        // exception too many publishes
                        setClientStatusNoLog("Parked", x.getTopicName());
                        x.setSavedStatus("Parked");
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                });
        // TODO csomagid
    }

    private void setClientStatusNoLog(String status, String topic) throws MqttException {
        subToTopicIfAble(topic, MqttTopicType.client);
        publishMessage(status, topic);
    }

    private void setClientStatus(String status, String topic) throws MqttException {
        subToTopicIfAble(topic, MqttTopicType.client);
        publishMessage(status, topic);
        publishMessage(topic + " státusza a következőre állítva: " + status);
    }

    private void handleArmageddon() {
        getAllClients().forEach(x -> {
            try {
                // error too many publishes
                setClientStatusNoLog("Armageddon", x.getTopicName());
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
        armageddon = 2500;
    }

    private List<MqttTopicData> getAllClients() {
        return subbedTopics.stream().filter(x -> x.getType().equals(MqttTopicType.client))
                .collect(Collectors.toList());
    }

    private void restoreAllClientStatuses() {
        getAllClients().forEach(x -> {
            try {
                publishMessage(x.getSavedStatus(), x.getTopicName());
            } catch (MqttException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
    }
}
