package com.example.locker.mqtt;

import com.example.locker.common.HealthFile;
import com.example.locker.common.JsonMapper;
import com.example.locker.common.dto.CompartmentOpenedEventMsg;
import com.example.locker.common.dto.EventEnvelope;
import com.example.locker.common.tls.TlsContextFactory;
import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import com.example.locker.common.LogConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SubscriberMain {

    private static final Logger LOG = Logger.getLogger(SubscriberMain.class.getName());

    public static void main(String[] args) throws Exception {
        LogConfig.init();

        String brokerUri = System.getenv().getOrDefault("MQTT_BROKER_URI", "ssl://localhost:8883");
        String clientId = System.getenv().getOrDefault("MQTT_CLIENT_ID", "mqtt-subscriber");
        String topic = System.getenv().getOrDefault("MQTT_TOPIC_OPENED",
                "psfusion/business-event/v010/compartment/opened");

        SSLContext sslContext = TlsContextFactory.create();
        SSLSocketFactory socketFactory = sslContext.getSocketFactory();

        MqttConnectionOptions opts = new MqttConnectionOptions();
        opts.setSocketFactory(socketFactory);
        opts.setAutomaticReconnect(true);
        opts.setCleanStart(true);

        MqttAsyncClient client = new MqttAsyncClient(brokerUri, clientId, new MemoryPersistence());
        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse resp) {
                LOG.warning("Disconnected: " + resp.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException ex) {
                LOG.log(Level.SEVERE, "MQTT error", ex);
            }

            @Override
            public void messageArrived(String t, MqttMessage msg) {
                String payload = new String(msg.getPayload(), StandardCharsets.UTF_8);
                LOG.info("Received event topic=" + t + " payload=" + payload);
                try {
                    var mapper = JsonMapper.instance();
                    EventEnvelope<CompartmentOpenedEventMsg> envelope = mapper.readValue(payload,
                            mapper.getTypeFactory().constructParametricType(
                                    EventEnvelope.class, CompartmentOpenedEventMsg.class));
                    LOG.info("Deserialized: headers=" + envelope.headers() + " payload=" + envelope.payload());
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Failed to deserialize event", e);
                }
            }

            @Override
            public void deliveryComplete(IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                LOG.info("Connected to " + serverURI + " with mTLS (reconnect=" + reconnect + ")");
                try {
                    client.subscribe(topic, 1);
                    LOG.info("Subscribed to topic=" + topic + " QoS=1");
                    HealthFile.markReady("mqtt-subscriber");
                } catch (MqttException e) {
                    LOG.log(Level.SEVERE, "Failed to subscribe", e);
                }
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {}
        });

        LOG.info("Connecting to " + brokerUri + " as " + clientId);
        client.connect(opts).waitForCompletion();

        // Block forever (long-running container)
        Thread.currentThread().join();
    }
}
