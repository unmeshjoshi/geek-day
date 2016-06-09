package com.cdc.s3.connect;

import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class KafkaConsumerClient {


    private String topic;

    public KafkaConsumerClient(String topic) {
        this.topic = topic;
    }

    List<GenericRecord> consumeUpto(GenericData.Record latestPublishedLocation) throws IOException {
        KafkaConsumer<byte[], byte[]> kafkaConsumer = new KafkaConsumer<>(consumerConfigs());

        kafkaConsumer.subscribe(Arrays.asList(topic));

        ConsumerRecords<byte[], byte[]> poll = kafkaConsumer.poll(0);

        Iterator<ConsumerRecord<byte[], byte[]>> iterator = poll.iterator();

        List<GenericRecord> consumedLocations = new ArrayList<>();
        String latestAddress = "";
        while (!latestAddress.equals(latestPublishedLocation.get("address"))) {
            if (!iterator.hasNext()) {
                iterator = kafkaConsumer.poll(1000).iterator();
                continue;
            }
            ConsumerRecord<byte[], byte[]> consumerRecord = iterator.next();
            List<GenericRecord> genericRecords = parseMessages(consumerRecord.value());
            GenericRecord genericRecord = genericRecords.get(genericRecords.size() - 1);
            latestAddress = genericRecord.get("address").toString();
            consumedLocations.add(genericRecord);
        }
        return consumedLocations;
    }


    private List<GenericRecord> parseMessages(byte[] is) throws IOException {
        List<GenericRecord> messages = new ArrayList<>();
        GenericDatumReader dr = new GenericDatumReader();
        DataFileStream reader = new DataFileStream(new ByteArrayInputStream(is), dr);
        for (Object o : reader) {
            GenericRecord record = (GenericRecord) o;
            messages.add(record);
        }
        return messages;
    }


    private HashMap<String, Object> consumerConfigs() {
        HashMap<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "192.168.33.10:9092");
        configs.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        configs.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        configs.put("key.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        configs.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");
        configs.put("zookeeper.connect", "192.168.33.10:2181");
        configs.put("auto.offset.reset", "earliest");
        configs.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");


        int groupIdRandom = new Random().nextInt(100000);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, "console-consumer-" + groupIdRandom);
        return configs;
    }
}
