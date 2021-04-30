image=$(aws ec2 describe-images \
    --filters Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-focal-20.04-amd64* \
    --query 'Images[*].[ImageId]' --output text \
    | sort -k2 -r \
    | head -n1)

instance=$(aws ec2 run-instances --image-id "$image" --security-group-ids sg-63f2c210 \
    --instance-type t2.micro --key-name kafka_project \
    --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=Kafka Broker}]' | jq -r '.Instances[0].InstanceId')
# echo "$instance"
while [ $(aws ec2 describe-instance-status --instance-ids "$instance" | jq -r '.InstanceStatuses[0].InstanceStatus.Status') != "ok" ]; do
    :
done
# echo "Instance ready"
ip=$(aws ec2 describe-instances --instance-ids "$instance" --query 'Reservations[*].Instances[*].PublicIpAddress' --output text)
echo "$instance"
echo "$ip"

ssh -oStrictHostKeyChecking=no -i /home/parth/kafka_project.pem ubuntu@"$ip" "
git clone https://github.com/Parth27/kafka-package;
sudo apt update;
sudo apt install -y openjdk-11-jdk-headless;
export JAVA_HOME=/usr/lib/jvm/java-1.11.0-openjdk-amd64;
export PATH=$JAVA_HOME/bin:$PATH"