package minko.kafkatraining.connector;

import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

import java.util.Collection;
import java.util.Map;


public class CustomSinkTask extends SinkTask {

    private Map<String, String> config;

    @Override
    public String version() {
        // версия таски
        return "SinkTask ver 1";
    }

    @Override
    public void start(Map<String, String> props) {
        // старт таски
        this.config = props;
        System.out.println("Starting SinkTask. Properties: " + props);
    }

    @Override
    public void put(Collection<SinkRecord> records) {
        // вот тут основная логика обработки сообщения из Kafka
        for (SinkRecord record : records) {
            System.out.println("CUSTOM CONNECTOR: Received record: " + record.toString());
        }
    }

    @Override
    public void stop() {
        // Остановка таски
    }
}

