package com.example.locker.mqtt;

import com.example.locker.common.HealthFile;
import com.example.locker.common.JsonMapper;
import com.example.locker.common.dto.CompartmentOpenedEventMsg;
import com.example.locker.common.dto.EventEnvelope;
import com.example.locker.common.dto.EventHeaders;
import com.example.locker.common.tls.TlsContextFactory;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import com.example.locker.common.LogConfig;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Logger;

public final class PublisherMain {

    private static final Logger LOG = Logger.getLogger(PublisherMain.class.getName());

    public static void main(String[] args) throws Exception {
        LogConfig.init();

        String brokerUri = System.getenv().getOrDefault("MQTT_BROKER_URI", "ssl://localhost:8883");
        String clientId = System.getenv().getOrDefault("MQTT_CLIENT_ID", "mqtt-publisher");
        String topic = System.getenv().getOrDefault("MQTT_TOPIC_OPENED",
                "psfusion/business-event/v010/compartment/opened");
        String lockerId = System.getenv().getOrDefault("LOCKER_ID", "f47ac10b-58cc-4372-a567-0e02b2c3d479");
        String compartmentId = System.getenv().getOrDefault("COMPARTMENT_ID",
                "compartmentInfo@d4e5f6a7-b8c9-0123-defa-234567890123");

        SSLContext sslContext = TlsContextFactory.create();

        MqttConnectionOptions opts = new MqttConnectionOptions();
        opts.setSocketFactory(sslContext.getSocketFactory());
        opts.setCleanStart(true);

        MqttAsyncClient client = new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());
        LOG.info("Connecting to " + brokerUri + " as " + clientId);
        client.connect(opts).waitForCompletion();
        LOG.info("Connected with mTLS");

        var envelope = new EventEnvelope<>(
                new EventHeaders("1.0.1", lockerId, "CompartmentOpenedEvent", Instant.now()),
                new CompartmentOpenedEventMsg(compartmentId, Instant.now())
        );

        String json = JsonMapper.instance().writeValueAsString(envelope);
        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        msg.setRetained(false);

        client.publish(topic, msg).waitForCompletion();
        LOG.info("Published test CompartmentOpenedEventMsg to topic=" + topic);

        client.disconnect().waitForCompletion();
        client.close();

        HealthFile.markDone("mqtt-publisher");
        LOG.info("Done.");
    }
}
