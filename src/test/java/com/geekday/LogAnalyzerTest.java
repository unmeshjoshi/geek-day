package com.geekday;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;

public class LogAnalyzerTest {

    private static final String topic = "location-views";

    private static final String bootStrapServers = "192.168.33.10:9092";
    private static final String zookeeperConnect = "192.168.33.10:2181";

    public static void main(String[] args) throws IOException, InterruptedException {
        Schema schema = SchemaBuilder
                .record("weblog")
                .fields()
                .name("region").type().stringType().noDefault()
                .name("location").type().stringType().noDefault()
                .name("time").type().longType().noDefault()
                .endRecord();


        startProducingLocationViews(schema);
        startLogAnalyzerStream();
    }

    private static void startProducingLocationViews(final Schema schema) throws IOException {
        String[] regions = new String[]{"USA", "UK", "Asia", "Australia"};
        String[] locations = new String[]{"Goa", "Pune", "Mumbai"};

        new Thread(new Runnable()  {
            @Override
            public void run() {
                try {
                    while (true) {

                        final GenericData.Record value = new GenericData.Record(schema);
                        value.put("region", getRandom(regions));
                        value.put("location", getRandom(locations));
                        value.put("time", System.currentTimeMillis());

                        new KafkaProducerClient(topic, bootStrapServers, zookeeperConnect).produce(schema, value);
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }

            private String getRandom(String[] array) {
                Random random = new Random();
                return array[random.nextInt(array.length - 1)];
            }

        }).start();
    }


    private static void startLogAnalyzerStream() {
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "log-analyzer");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
        streamsConfiguration.put(StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, zookeeperConnect);
        streamsConfiguration.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        streamsConfiguration.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        streamsConfiguration.put(StreamsConfig.TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());

        KStreamBuilder builder = new KStreamBuilder();

        KStream<byte[], byte[]> locationViews = builder.stream(Serdes.ByteArray(), Serdes.ByteArray(), topic);

        printViewsPerRegionLocation(locationViews);

        KafkaStreams streams = new KafkaStreams(builder, streamsConfiguration);
        streams.start();
    }

    private static void printViewsPerRegionLocation(KStream<byte[], byte[]> locationViews) {
//        KTable<String, Integer> aggregated = locationViews
//                .map((key, value) -> {
//                    GenericRecord parsedRecord = parse(value);
//                    String parsedKey = parsedRecord.get("region").toString() + parsedRecord.get("location").toString();
//                    return new KeyValue<>(parsedKey, 1);
//                }).reduceByKey((v1, v2) -> v1 + v2, Serdes.String(), Serdes.Integer(), "aggregated");


        KTable<Windowed<String>, Integer> aggregated = locationViews
                .map((key, value) -> {
                    GenericRecord parsedRecord = parse(value);
                    String parsedKey = parsedRecord.get("region").toString() + parsedRecord.get("location").toString();
                    return new KeyValue<>(parsedKey, 1);
                }).reduceByKey((v1, v2) -> v1 + v2, TimeWindows.of("aggregated", 100000), Serdes.String(), Serdes.Integer());

        aggregated.foreach((k, v) -> System.out.println(k + ", " + v));
    }

    private static GenericRecord parse(byte[] value) {
        GenericDatumReader dr = new GenericDatumReader();
        DataFileStream reader = null;
        try {
            reader = new DataFileStream(new ByteArrayInputStream(value), dr);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return (GenericRecord)reader.next();
    }

}
