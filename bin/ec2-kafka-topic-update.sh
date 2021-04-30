ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
nohup bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic Messages --replication-factor $2 --partitions $3 >> /dev/null 2>&1 &"