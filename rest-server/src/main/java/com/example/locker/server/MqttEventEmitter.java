package com.example.locker.server;

import com.example.locker.common.JsonMapper;
import com.example.locker.common.dto.CompartmentOpenedEventMsg;
import com.example.locker.common.dto.EventEnvelope;
import com.example.locker.common.dto.EventHeaders;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MqttEventEmitter {

    private static final Logger LOG = Logger.getLogger(MqttEventEmitter.class.getName());

    private final MqttAsyncClient client;
    private final String topic;

    public MqttEventEmitter(String brokerUri, String clientId, String topic, SSLContext sslContext) throws Exception {
        this.topic = topic;

        MqttConnectionOptions opts = new MqttConnectionOptions();
        opts.setSocketFactory(sslContext.getSocketFactory());
        opts.setAutomaticReconnect(true);
        opts.setCleanStart(true);

        client = new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());
        LOG.info("Connecting MQTT to " + brokerUri + " as " + clientId);
        client.connect(opts).waitForCompletion();
        LOG.info("MQTT connected with mTLS");
    }

    public void publishCompartmentOpened(String lockerId, String compartmentId) {
        try {
            var envelope = new EventEnvelope<>(
                    new EventHeaders("1.0.1", lockerId, "CompartmentOpenedEvent", Instant.now()),
                    new CompartmentOpenedEventMsg(compartmentId, Instant.now())
            );
            String json = JsonMapper.instance().writeValueAsString(envelope);
            MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
            msg.setQos(1);
            msg.setRetained(false);
            client.publish(topic, msg).waitForCompletion();
            LOG.info("Published CompartmentOpenedEventMsg to topic=" + topic);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Failed to publish CompartmentOpenedEventMsg", e);
        }
    }
}
