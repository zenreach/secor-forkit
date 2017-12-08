package com.zenreach.data.secor

import java.io.File
import java.util

import com.amazonaws.services.s3.AmazonS3URI
import com.pinterest.secor.common.SecorConfig
import com.typesafe.config.ConfigFactory
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.{Logger, LoggerFactory}
import pureconfig.error.ConfigReaderFailures

import scala.collection.JavaConversions._

case class HostPort(str: String) extends AnyVal {
  def host: String = str.split(':').head
  def port: Int = str.split(':').last.toInt
}

case class S3Destination(str: String) extends AnyVal {
  def s3Parsed = new AmazonS3URI(str)
  def bucket: String = s3Parsed.getBucket
  def path: String = s3Parsed.getKey
}

case class EnvConfig(
  kafkaGroup: String,
  kafkaBroker: HostPort,
  s3: S3Destination,
  zookeeperHost: HostPort,
  topicFilter: String,
  protobufClass: String,
  localPath: String = File.createTempFile("secor", "archives").getAbsolutePath
) {
  def toProperties: Map[String, String] = Map(
    "secor.kafka.group" -> kafkaGroup,
    "kafka.seed.broker.host" -> kafkaBroker.host,
    "kafka.seed.broker.port" -> kafkaBroker.port.toString,
    "zookeeper.quorum" -> zookeeperHost.str,
    "secor.kafka.topic_filter" -> topicFilter,
    s"secor.protobuf.message.class.$topicFilter" -> protobufClass,
    "secor.local.path" -> localPath
  )
}

class EnvironmentSecorConfig(props: PropertiesConfiguration, envConfig: Option[EnvConfig])
    extends SecorConfig(props) {

  def get[T](getter: EnvConfig => T, default: => T): T = envConfig.fold(default)(getter)

  override def getKafkaGroup: String = get(_.kafkaGroup, super.getKafkaGroup)

  override def getKafkaSeedBrokerHost: String =
    get(_.kafkaBroker.host, super.getKafkaSeedBrokerHost)

  override def getKafkaSeedBrokerPort: Int = get(_.kafkaBroker.port, super.getKafkaSeedBrokerPort)

  override def getS3Bucket: String = get(_.s3.bucket, super.getS3Bucket)

  override def getS3Path: String = get(_.s3.path, super.getS3Path)

  override def getKafkaTopicFilter: String = get(_.topicFilter, super.getKafkaTopicFilter)

  override def getProtobufMessageClassPerTopic: util.Map[String, String] =
    envConfig
      .map { e =>
        Map(getKafkaTopicFilter -> e.protobufClass)
      }
      .getOrElse(Map.empty) ++ super.getProtobufMessageClassPerTopic

  override def getLocalPath: String = get(_.localPath, super.getLocalPath)

}

object EnvironmentSecorConfig {
  import pureconfig._

  lazy val log: Logger = LoggerFactory.getLogger(getClass)

  lazy val envCfg: Either[ConfigReaderFailures, EnvConfig] =
    loadConfig[EnvConfig](ConfigFactory.load())

  def load(configName: String): SecorConfig =
    new ThreadLocal[SecorConfig]() {
      val systemProperties = System.getProperties
      val configProperty = systemProperties.getProperty("config", "defaults.properties")
      val properties = new PropertiesConfiguration(configProperty)
      log.debug(s"loaded properties ${properties}")

      for (entry <- properties.getKeys) {
        log.debug(s"LOADED $entry = ${properties.getString(entry.toString)}")
      }

      for (entry <- systemProperties.entrySet) {
        properties.setProperty(entry.getKey.toString, entry.getValue)
      }

      envCfg.left.foreach { e =>
        log.error("Failed to load environment config", e)
      }

      new EnvironmentSecorConfig(properties, envCfg.right.toOption)
    }.get()
}
