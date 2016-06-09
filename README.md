# geek-day
This is vagrant setup which can help running confluent platform latest release confluent-3.0.0-2.11 with latest kafka kafka_2.11:0.10.0.0-cp1 with supports for kafka streams. The ansible setup can be extended to add additional packages required for your project. (e.g. adding postgresql etc..). 
The repo also has an integration test with utilities to create topic, publish messages on topic and consume them.

## Setup instructions

1. Install Vagrant https://www.vagrantup.com/downloads.html
2. Install Ansible http://docs.ansible.com/ansible/intro_installation.html. Make sure you have ansible 2.0
3. Install Intellij Idea.
4. Checkout geek-day repository. git clone
5. cd geek-day
6. Run command vagrant up. This will pull ubuntu/trusty64 basebox and install following set of packages
	1. JDK 1.8.0_65
	2. confluent-3.0.0-2.11

Once the machine is up and running, 

1. vagrant ssh
2. cd /vagrant
3. run ./startAll.sh
   This script will start three zookeepe, kafka brokers and schema registry.

Open the project in intellij and run KafkaIntegrationTest
