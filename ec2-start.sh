image=$(aws ec2 describe-images --owners amazon --filters 'Name=name,Values=amzn2-ami-hvm-2.0.????????-x86_64-gp2' 'Name=state,Values=available' --output json | jq -r '.Images | sort_by(.CreationDate) | last(.[]).ImageId');

instance=$(aws ec2 run-instances --image-id "$image" --security-group-ids sg-63f2c210 --instance-type t2.micro --key-name kafka_project | jq -r '.Instances[0].InstanceId');
ip=$(aws ec2 describe-instances --instance-ids "$instance" --query 'Reservations[*].Instances[*].PublicIpAddress' --output text);
echo "$instance";
echo "$ip";