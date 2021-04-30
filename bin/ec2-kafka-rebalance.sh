ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --topics-to-move-json-file /home/ubuntu/kafka-package/topics.json --broker-list $2 --generate > full-reassignment.json;
cat full-reassignment.json | grep version | tail -n 1 > reassignment.json;
bin/kafka-reassign-partitions.sh --zookeeper localhost:2181 --reassignment-json-file reassignment.json --execute"
