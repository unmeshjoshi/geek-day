package com.geekday;


import kafka.admin.AdminUtils;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.kafka.clients.producer.ProducerRecord;
import scala.collection.Seq;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class KafkaProducerClient {

    private String topic;

    public KafkaProducerClient(String topic) {
        this.topic = topic;
    }

    void createTopic(String topic) {
        String zookeeperConnect = "192.168.33.10:2181";
        int sessionTimeoutMs = 10 * 1000;
        int connectionTimeoutMs = 8 * 1000;
        // Note: You must initialize the ZkClient with ZKStringSerializer.  If you don't, then
        // createTopic() will only seem to work (it will return without error).  The topic will exist in
        // only ZooKeeper and will be returned when listing topics, but Kafka itself does not create the
        // topic.
        ZkClient zkClient = new ZkClient(
                zookeeperConnect,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);

        // Security for Kafka was added in Kafka 0.9.0.0
        boolean isSecureKafkaCluster = false;
        ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), isSecureKafkaCluster);
        Seq<String> allTopics = zkUtils.getAllTopics();
        if (allTopics.contains(topic)) {
            System.out.println("topic = " + topic + " already exists");
            return;
        }

        int partitions = 2;
        int replication = 2;
        AdminUtils.createTopic(zkUtils, topic, partitions, replication, new Properties(), null);

        zkClient.close();
    }

    void produce(Schema schema, GenericData.Record value) throws IOException {
        createTopic(topic);

        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.33.10:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        props.put("request.required.acks", "1");


        org.apache.kafka.clients.producer.KafkaProducer producer = new org.apache.kafka.clients.producer.KafkaProducer(props);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>();
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(writer).create(schema, out);
        dataFileWriter.append(value);
        dataFileWriter.flush();

        byte[] serializedBytes = out.toByteArray();

        System.out.println("Producing message " + value);

        ProducerRecord<String, byte[]> message = new ProducerRecord<String, byte[]>(topic, serializedBytes);
        producer.send(message);
        producer.close();

    }
}