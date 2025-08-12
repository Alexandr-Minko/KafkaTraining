package minko.kafkatraining.connector;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomSinkConnector extends SinkConnector {

    private Map<String, String> config;
    private List<String> topics;

    @Override
    public void start(Map<String, String> props) {
        // запуск коннектора
        this.config = props;
        this.topics = props.containsKey("topics")
                ? List.of(props.get("topics").split(","))
                : List.of();
    }

    @Override
    public Class<? extends Task> taskClass() {
        // класс таски
        return CustomSinkTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        // настройки для Таски
        List<Map<String, String>> configs = new ArrayList<>();
        for (int i = 0; i < maxTasks; i++) {
            Map<String, String> taskConfig = new java.util.HashMap<>(config);
            taskConfig.put("task.id", String.valueOf(i));
            configs.add(taskConfig);
        }
        return configs;
    }

    @Override
    public void stop() {
        // Остановка коннектора
    }

    @Override
    public ConfigDef config() {
        // настройки
        return new ConfigDef()
                .define("topics", ConfigDef.Type.STRING, "topic1", ConfigDef.Importance.HIGH, "Список тем Kafka")
                .define("set1", ConfigDef.Type.STRING, "sdsd", ConfigDef.Importance.HIGH, "настройка 1");
    }

    @Override
    public String version() {
        // версия коннектора
        return "SinkConnector ver 1";
    }
}
