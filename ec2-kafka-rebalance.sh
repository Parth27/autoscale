ssh -oStrictHostKeyChecking=no -i kafka_project.pem ubuntu@"$1" "
cd kafka-package;
nohup bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --topics-to-move-json-file /home/parth/autoscale/topics.json --broker-list $2 >> /dev/null 2>&1 &"
