public class MqttTopicData {
    private String topicName;
    private MqttTopicType type;
    private String savedStatus;

    public MqttTopicData(String topicName, MqttTopicType type) {
        this.topicName = topicName;
        this.type = type;
    }

    public MqttTopicData(String topicName, MqttTopicType type, String savedStatus) {
        this.topicName = topicName;
        this.type = type;
        this.savedStatus = savedStatus;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public MqttTopicType getType() {
        return type;
    }

    public void setType(MqttTopicType type) {
        this.type = type;
    }

    public String getSavedStatus() {
        return savedStatus;
    }

    public void setSavedStatus(String savedStatus) {
        this.savedStatus = savedStatus;
    }
}
