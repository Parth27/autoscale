ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
bin/autoscale-compile.sh;
nohup bin/kafka-server-start.sh config/server.properties >> /dev/null 2>&1 &"