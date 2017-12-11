package com.zenreach.data.secor

import java.io.File
import java.net.URI
import java.util

import com.pinterest.secor.common.SecorConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

case class HostPort(str: String) extends AnyVal {
  def host: String = str.split(':').head
  def port: Int = str.split(':').last.toInt
}

case class EnvConfig(
  kafkaGroup: String,
  kafkaBroker: HostPort,
  remotePath: String,
  zookeeperHost: HostPort,
  topicFilter: String,
  protobufClass: String,
  localPath: String = File.createTempFile("secor", "archives").getAbsolutePath,
  statsPort: Int = 9990
) {
  val s3Url: URI = URI.create(remotePath)
  require(s3Url.getScheme == "s3", "Only support S3 at this time as a remote destination")
  val s3Bucket: String = s3Url.getHost
  val s3Path: String = s3Url.getPath

  def toProperties: Map[String, String] = Map(
    "secor.kafka.group" -> kafkaGroup,
    "kafka.seed.broker.host" -> kafkaBroker.host,
    "kafka.seed.broker.port" -> kafkaBroker.port.toString,
    "zookeeper.quorum" -> zookeeperHost.str,
    "secor.kafka.topic_filter" -> topicFilter,
    s"secor.protobuf.message.class.$topicFilter" -> protobufClass,
    "secor.local.path" -> localPath,
    "secor.s3.bucket" -> s3Bucket,
    "secor.s3.path" -> s3Path,
    "ostrich.port" -> statsPort.toString
  )
}

class EnvironmentSecorConfig(props: PropertiesConfiguration, envConfig: EnvConfig)
    extends SecorConfig(props) {

  def get[T](getter: EnvConfig => T, default: => T): T =
    Option(getter(envConfig)).getOrElse(default)

  override def getKafkaGroup: String = get(_.kafkaGroup, super.getKafkaGroup)

  override def getKafkaSeedBrokerHost: String =
    get(_.kafkaBroker.host, super.getKafkaSeedBrokerHost)

  override def getKafkaSeedBrokerPort: Int = get(_.kafkaBroker.port, super.getKafkaSeedBrokerPort)

  override def getS3Bucket: String = get(_.s3Bucket, super.getS3Bucket)

  override def getS3Path: String = get(_.s3Path, super.getS3Path)

  override def getKafkaTopicFilter: String = get(_.topicFilter, super.getKafkaTopicFilter)

  override def getProtobufMessageClassPerTopic: util.Map[String, String] =
    Map(envConfig.topicFilter -> envConfig.protobufClass)

  override def getLocalPath: String = get(_.localPath, super.getLocalPath)

  override def getOstrichPort: Int = get(_.statsPort, super.getOstrichPort)

}

object EnvironmentSecorConfig {
  import pureconfig._

  lazy val log: Logger = LoggerFactory.getLogger(getClass)

  lazy val cfg: Config = ConfigFactory
    .load("application")
    .withFallback(ConfigFactory.defaultApplication())

  lazy val envCfg: EnvConfig =
    loadConfigOrThrow[EnvConfig](cfg)

  lazy val threadLocalCfg: ThreadLocal[SecorConfig] = new ThreadLocal[SecorConfig]() {

    override def initialValue(): SecorConfig = {
      val systemProperties = System.getProperties
      val properties: PropertiesConfiguration = new PropertiesConfiguration(
        System.getProperty("config", "defaults.properties")
      )
      log.debug(s"loaded properties ${properties}")

      for (entry <- systemProperties.entrySet) {
        properties.setProperty(entry.getKey.toString, entry.getValue)
      }

      envCfg.toProperties.foreach {
        case (k, v) =>
          properties.setProperty(k, v)
      }

      for (entry <- properties.getKeys) {
        log.debug(s"LOADED $entry = ${properties.getString(entry.toString)}")
      }

      new EnvironmentSecorConfig(properties, envCfg)
    }
  }

  def load(): SecorConfig = threadLocalCfg.get
}
