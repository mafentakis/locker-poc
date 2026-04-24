package com.example.locker.it;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public final class MqttTestClient implements AutoCloseable {

    private final MqttAsyncClient client;
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    public MqttTestClient(SSLContext sslContext, String brokerUri, String topic) throws Exception {
        MqttConnectionOptions opts = new MqttConnectionOptions();
        opts.setSocketFactory(sslContext.getSocketFactory());
        opts.setCleanStart(true);

        client = new MqttAsyncClient(brokerUri,
                "it-test-" + System.nanoTime(), new MemoryPersistence());

        client.setCallback(new MqttCallback() {
            @Override public void disconnected(MqttDisconnectResponse r) {}
            @Override public void mqttErrorOccurred(MqttException e) {}
            @Override public void deliveryComplete(IMqttToken t) {}
            @Override public void connectComplete(boolean reconnect, String uri) {}
            @Override public void authPacketArrived(int rc, MqttProperties p) {}

            @Override
            public void messageArrived(String t, MqttMessage msg) {
                messages.add(new String(msg.getPayload(), StandardCharsets.UTF_8));
            }
        });

        client.connect(opts).waitForCompletion(10_000);
        client.subscribe(topic, 1).waitForCompletion(10_000);
    }

    public String poll(long timeoutSeconds) throws InterruptedException {
        return messages.poll(timeoutSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        client.disconnect().waitForCompletion(5000);
        client.close();
    }
}

