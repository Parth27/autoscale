ssh -oStrictHostKeyChecking=no -i kafka_project.pem ubuntu@"$1" "
cd kafka-package;
nohup bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 3 --topic Messages >> /dev/null 2>&1 &"