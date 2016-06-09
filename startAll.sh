STEP=10 #need to wait or else get exception that the broker already registerd with zookeeper

/opt/confluent-3.0.0/bin/zookeeper-server-start /opt/confluent-3.0.0/etc/kafka/zookeeper.properties 2>&1 > zookeeper.log &

echo "Starting zookeeper. Waiting 10 seconds"

sleep $STEP #wait till zookeeper starts

/opt/confluent-3.0.0/bin/kafka-server-start /vagrant/config/server1.properties 2>&1 > server1.log &


/opt/confluent-3.0.0/bin/kafka-server-start /vagrant/config/server2.properties 2>&1 > server2.log &


/opt/confluent-3.0.0/bin/kafka-server-start /vagrant/config/server3.properties 2>&1 > server3.log &

echo "Starting kafka brokers. Waiting 10 seconds"

sleep $STEP #wait till kafka brokers start
 
/opt/confluent-3.0.0/bin/schema-registry-start /opt/confluent-3.0.0/etc/schema-registry/schema-registry.properties 2>&1 > schema-registry.log &

./logAll.sh
