ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$1" "
cd kafka-package;
nohup bin/kafka-server-stop.sh >> /dev/null 2>&1 &"

aws ec2 terminate-instances --instance-ids "$2"