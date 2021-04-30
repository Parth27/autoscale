ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 2 --partitions 6 --topic $2"