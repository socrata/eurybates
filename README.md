## Eurybates

<!---
At the top of the file there should be a short introduction and/ or overview that explains **what** the project is. This description should match descriptions added for package managers (Gemspec, package.json, etc.)
-->

Eurybates is Socrata's internal Eventing Library.  Currently, Eurybates supports Activemq and Kafka.
Both are configured via a Properties Object and a client application defined Source ID.

Note: Currently there have no plans to open source Eurybates and make it publically available.

## Motivation

<!---
A short description of the motivation behind the creation and maintenance of the project. This should explain **why** the project exists.
-->

To support a fully Asynchronous Event base microservice distributed architecture we require a mechanism to produce and consume messages asynchronously.
Eurybates is that mechanism.

## Installation

<!---
Provide code examples and explanations of how to get the project.
-->

Currently we only support using Eurybates as a Maven/Ivy Dependency that is hosted in artifactory and therefore can only be
  referenced internally within Socrata.

To include Eurybates in your SBT Project

Ensure you have the Socrata Artifactory Resolver added the list of resolvers

```
resolvers ++= Seq(
    "socrata maven" at "https://repository-socrata-oss.forge.cloudbees.com/release"
)
```

Then add Eurybates to your list of library dependencies

```
libraryDependencies ++= Seq(
    "com.socrata" %% "eurybates" % EURYBATES_VERSION
)
```

## Build

<!---
Provide an example of how to build this project.

```
# Command(s) for how to build your project.
```
-->

Traditional SBT Tasks

```
sbt compile test package
```

## Code Example

<!---
Show what the library does as concisely as possible, developers should be able to figure out **how** your project solves their
problem by looking at the code example. Make sure the API you are showing off is obvious, and that your code is short and concise.
-->

Eurybates can be used to produce or consume messages.  To do either requires the following properties configuration.  All configuration
  is rooted with the prefix of *eurybates*.

```
eurybates.*
```

Configuration differs based off different components.

* Producer
* Consumer (Planned)

### Configuring Producer Type

1. Picking a Producer type(s).  The available options are activemq, kafka, local_service, and noop.

```
eurybates.producers = activemq | kafka | local_service | noop
```

or

For Multi-Plexing Producers
```
eurybates.producers = activemq,kafka
```

### Configuring Kafka

Configuring Kafka Requires a broker list Properties.  The broker list is a comma separated of Kafka broker host:port.  Each
  broker must be apart of the same Cluster.
```
eurybates.kafka.broker_list = kafka-1:9092,kafka-2:9092
```

### Configuring ActiveMQ

```
eurybates.activemq.connection_string = tcp://activemq-1:PORT
```

#### Adding a queue to ActiveMQ

1. Find and ssh into a zookeeper node (for example, `knife search node 'role:*zookeeper* AND environment:*staging*'`)

2.
```bash
sudo su
/opt/zookeeper/zookeeper-3.4.6/bin/zkCli.sh
create /eurybates/services/new-queue-name "new-queue-name"
```

### Service Definition

You could use any consumer library capable of consuming messages from JMS (for activemq) or Kafka.  We are looking
 to modernize Eurybates to use a better streaming models. Possible options for consuming Kafka Messages.

* Kafka Clients: [Maven Library](http://search.maven.org/#artifactdetails%7Corg.apache.kafka%7Ckafka-clients%7C0.8.2.2%7Cjar)
* Scala Kafka: https://github.com/stealthly/scala-kafka
* Reactive Kafka: https://github.com/softwaremill/reactive-kafka
* Eurybates itself

Eurybates enforces some interesting requirements and design decisions that can make it initially confusing.  Eurybates
also makes some assumptions about how you want to commit offsets as well.  Currently, Eurybates automatically
commits offsets every 10 seconds.  This means you have no direct control over when you tell Kafka you have recieved a
message.  If this does not fit your use case do NOT use Eurybates at this time.  Please use one of the above options.

If you decide to use Eurybates, in order to consume messages you must define a class that extends *com.socrata.eurybates.Service*.

```
class FooBarService extends Service {

  override def messageReceived(message: Message): Unit = {
    // Process the message however you like
    System.out.println(s"Yay I Received a message: $message")
  }

}
```

The service serves as a callback and a consumer must defined in your container class, object, or trait like so.

```
object FooBarWrapper {

  def consumer() = new KafkaServiceConsumer("kafka-broker-1:9062",
    "some-source-id",
    Executors.newFixedThreadPool(1),
    (sn: ServiceName, s: String, t: Throwable) => {},
    Map((Name, new FooBarService())))

}
```

<!---
## API Reference

Depending on the size of the project, if it is small and simple enough the reference docs can be added to the README.
For medium size to larger projects it is important to at least provide a link to where the API reference docs live.
-->

## Tests

TODO Document how to incorporate Eurybates into your test.

<!---
Describe and show how to run the tests with code examples.

```
# Command(s) for how to test your project.
```
-->

None at this time... Yikes.

## Contributors

<!---
Let people know how they can dive into the project, include important links to things like issue trackers, irc, twitter accounts if applicable.
-->

Socrata Engineering Members
* Robert Macomber
* Andrew Gall
* Michael Hotan

## License

<!---
A short snippet describing the license (MIT, Apache, etc.)
-->

???
