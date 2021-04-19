ssh -oStrictHostKeyChecking=no -i kafka_project.pem ubuntu@"$1" "
cd kafka-package;
nohup bin/kafka-server-stop.sh >> /dev/null 2>&1 &"