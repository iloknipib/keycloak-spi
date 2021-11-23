package com.dehaat.service;

import com.dehaat.config.ConfigLoader;
import com.dehaat.config.ConfigProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.json.JSONObject;

import java.io.IOException;

public class MessagingQueueService implements MessagingQueue {

    private Channel channel = null;
    private final static String RABBITMQ_CONNECT_URL = ConfigLoader.getProp().getProperty(ConfigProperties.RABBITMQ_CONNECT_URL.name());
    private final static String RABBITMQ_EXCHANGE = ConfigLoader.getProp().getProperty(ConfigProperties.RABBITMQ_EXCHANGE.name());
    private final static String RABBITMQ_KEYCLOAK_QUEUE = ConfigLoader.getProp().getProperty(ConfigProperties.RABBITMQ_KEYCLOAK_QUEUE.name());


    @Override
    public void send(JSONObject message) {
        Channel channel = getChannel();
        if (channel == null) {
            return;
        }
        byte[] messageBodyBytes = message.toString().getBytes();
        try {
            channel.basicPublish(RABBITMQ_EXCHANGE, RABBITMQ_KEYCLOAK_QUEUE, null, messageBodyBytes);
        } catch (IOException ex) {
            System.out.println("Error: Error while publishing data to MessagingQueue");
        }
    }

    public Channel getChannel() {
        if (channel != null) {
            return channel;
        } else {
            try {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(RABBITMQ_CONNECT_URL);
                Connection connection = factory.newConnection();
                channel = connection.createChannel();
            } catch (Exception ex) {
                System.out.println("Error: Unable to create channel to connect to messaging queue");
            }
            return channel;
        }
    }

}