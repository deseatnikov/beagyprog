import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Watcher implements MqttCallbackExtended {
    private MqttClient client;
    private List<MqttTopicData> subbedTopics = new ArrayList<>();

    private int armageddon = 0;

    public Watcher(String ip, String name) throws MqttException {
        client = new MqttClient(ip, name);
        client.connect();
        client.setCallback(this);
        subToTopicIfAble(BrrStringConsts.TOPIC_LOGIN, MqttTopicType.global);
        subToTopicIfAble(BrrStringConsts.TOPIC_START, MqttTopicType.global);
        subToTopicIfAble(BrrStringConsts.TOPIC_LOAD_PACKAGE, MqttTopicType.global);
        subToTopicIfAble(BrrStringConsts.TOPIC_STOP_ALL, MqttTopicType.global);
        publishMessage(BrrStringConsts.MESSAGE_READY);
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
        // TODO scratch this garbage (fb group)
        if (0 < armageddon) {
            publishMessage(BrrStringConsts.MESSAGE_STOP_ALL + " (" + armageddon + ")");
            if (1 == armageddon--) {
                restoreAllClientStatuses();
                publishMessage(BrrStringConsts.MESSAGE_RESUME_ALL);
            }
        }
    }

    public void publishMessage(String message) throws MqttException {
        publishMessage(message, BrrStringConsts.TOPIC_SERVER_DEFAULT);
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
        switch (s) {
            case BrrStringConsts.TOPIC_LOGIN:
                handleLogin(mqttMessage);
                break;
            case BrrStringConsts.TOPIC_START:
                handleStart(mqttMessage);
                break;
            case BrrStringConsts.TOPIC_STOP_ALL:
                handleArmageddon();
                break;
            case BrrStringConsts.TOPIC_LOAD_PACKAGE:
                handleLoaded(mqttMessage);
                break;
        }
    }

    private void handleLogin(MqttMessage msg) throws MqttException {
        setClientStatus(BrrStringConsts.CLIENT_STATUS_READY, msg.toString());
        saveStatus(msg.toString(), BrrStringConsts.CLIENT_STATUS_READY);
    }

    private void handleStart(MqttMessage msg) throws MqttException {
        setClientStatus(BrrStringConsts.CLIENT_STATUS_STARTED, msg.toString());
        saveStatus(msg.toString(), BrrStringConsts.CLIENT_STATUS_STARTED);
        getAllClients().stream().filter(x -> x.getSavedStatus().equals(BrrStringConsts.CLIENT_STATUS_READY))
                .forEach(x -> {
                    try {
                        setClientStatusNoLog(BrrStringConsts.CLIENT_STATUS_HALTED, x.getTopicName());
                        x.setSavedStatus(BrrStringConsts.CLIENT_STATUS_HALTED);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                });
    }

    private void handleLoaded(MqttMessage msg) throws MqttException {
        String id = null;
        String csomagid = null;
        try {
            id = msg.toString().split(",")[0];
            csomagid = msg.toString().split(",")[1];
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (null == id || id.isEmpty()) {
                id = msg.toString();
            }
        }

        setClientStatus(BrrStringConsts.CLIENT_STATUS_MOVING, id);
        saveStatus(id, BrrStringConsts.CLIENT_STATUS_MOVING);
        getAllClients().stream().filter(x -> x.getSavedStatus().equals(BrrStringConsts.CLIENT_STATUS_HALTED))
                .forEach(x ->{
                    try {
                        setClientStatusNoLog(BrrStringConsts.CLIENT_STATUS_READY, x.getTopicName());
                        x.setSavedStatus(BrrStringConsts.CLIENT_STATUS_READY);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                });
        // TODO csomagid kezelés
    }

    // needed in loops because exception is thrown if there are too many publishes
    private void setClientStatusNoLog(String status, String topic) throws MqttException {
        subToTopicIfAble(topic, MqttTopicType.client);
        publishMessage(status, topic);
    }

    private void setClientStatus(String status, String topic) throws MqttException {
        subToTopicIfAble(topic, MqttTopicType.client);
        publishMessage(status, topic);
        publishMessage(topic + BrrStringConsts.MESSAGE_STATUS_CHANGED + status);
    }

    private void handleArmageddon() {
        getAllClients().forEach(x -> {
            try {
                setClientStatusNoLog(BrrStringConsts.CLIENT_STATUS_STOPPED, x.getTopicName());
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
