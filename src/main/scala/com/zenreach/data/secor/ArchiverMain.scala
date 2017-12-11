package com.zenreach.data.secor

import java.util

import com.pinterest.secor.common.{OstrichAdminService, SecorConfig}
import com.pinterest.secor.consumer.Consumer
import com.pinterest.secor.tools.LogFileDeleter
import com.pinterest.secor.util.{FileUtil, RateLimitUtil}
import com.twitter.app.{App, Flag}
import com.twitter.util.{Await, Future}
import io.prometheus.client.exporter.HTTPServer
import org.slf4j.{Logger, LoggerFactory}

object ArchiverMain extends App {
  val configEnv: Flag[String] =
    flag("config.name", "defaults.properties", "The name of the config file to load.")

  private lazy val log: Logger = LoggerFactory.getLogger(getClass)

  def main(): Unit =
    try {
      val config: SecorConfig = EnvironmentSecorConfig.load()
      new HTTPServer(config.getOstrichPort)
      FileUtil.configure(config)
      val logFileDeleter: LogFileDeleter = new LogFileDeleter(config)
      logFileDeleter.deleteOldLogs()
      RateLimitUtil.configure(config)
      val handler: Thread.UncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
        override def uncaughtException(thread: Thread, exception: Throwable): Unit = {
          log.error(s"Thread $thread failed", exception)
          System.exit(1)
        }
      }
      log.info("starting {} consumer threads", config.getConsumerThreads)
      val consumers: util.LinkedList[Consumer] = new util.LinkedList[Consumer]
      var i: Int = 0
      while ({
        i < config.getConsumerThreads
      }) {
        val consumer: Consumer = new Consumer(config)
        consumer.setUncaughtExceptionHandler(handler)
        consumers.add(consumer)
        consumer.start()

        {
          i += 1; i
        }
      }
      import scala.collection.JavaConversions._
      for (consumer <- consumers) {
        consumer.join()
      }
      Await.ready(Future.never)
    } catch {
      case t: Throwable =>
        log.error("Consumer failed", t)
        System.exit(1)
    }
}
