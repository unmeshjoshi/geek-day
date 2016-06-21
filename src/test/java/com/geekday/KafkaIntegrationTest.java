package com.geekday;


import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

public class KafkaIntegrationTest {

    private final String topic = "location";

    private String bootStrapServers = "192.168.33.10:9092";
    private String zookeeperConnect = "192.168.33.10:2181";

    @Test
    public void shouldProduceAndConsumeMessagesOnKafka() throws IOException, InterruptedException {

        Schema schema = SchemaBuilder
                .record("address")
                .fields()
                .name("address").type().stringType().noDefault()
                .name("address1").type().stringType().noDefault()
                .name("city").type().stringType().noDefault()
                .name("state").type().stringType().noDefault()
                .endRecord();

        Random random = new Random();
        GenericData.Record value = new GenericData.Record(schema);
        String addressLine1 = random.nextInt(100) + " city center";
        value.put("address", addressLine1);
        value.put("address1", "main street");
        value.put("city", "lexington");
        value.put("state", "ma");


        new KafkaProducerClient(topic, bootStrapServers, zookeeperConnect).produce(schema, value);

        List<GenericRecord> genericRecords = new KafkaConsumerClient(topic, bootStrapServers).consumeUpto(value);
        assertEquals(addressLine1, genericRecords.get(genericRecords.size() - 1).get("address").toString());
    }
}
