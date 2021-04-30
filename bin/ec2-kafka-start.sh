ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
echo 'advertised.listeners=PLAINTEXT://$1:9092' >> config/server.properties;
echo 'broker.id=$2' >> config/server.properties;
bin/autoscale-compile.sh;
nohup bin/kafka-server-start.sh config/server.properties >> /dev/null 2>&1 &"

exit