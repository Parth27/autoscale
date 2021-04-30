# AutoScale: Automatic resource optimization and scaling for distributed systems on cloud

`AutoScale` can be used for auto-scaling any application on any distributed system. It works by predicting the future resource usage and adding/removing application servers to prevent over and under-utilization.
For predicting resource usage, a simple Markov chain is used, but it may be replaced by a more sophisticated model soon.

This system currently has in-built support for Apache Kafka on AWS EC2, with more applications and distributed platforms to be added soon. The current system can be easily extended to any other application and/or distributed environment by simply adding their respective shell scripts.

On each application's server and `AutoScale` client needs to be installed which is available in this repository. For a working example, please refer this [Kafka repository](https://github.com/Parth27/kafka-package).
