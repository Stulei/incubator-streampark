/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.spark.client.`trait`

import org.apache.streampark.common.util._
import org.apache.streampark.common.util.Implicits._
import org.apache.streampark.spark.client.bean._

import scala.util.{Failure, Success, Try}

trait SparkClientTrait extends Logger {

  @throws[Exception]
  def submit(submitRequest: SubmitRequest): SubmitResponse = {
    logInfo(
      s"""
         |--------------------------------------- spark job start -----------------------------------
         |    userSparkHome    : ${submitRequest.sparkVersion.sparkHome}
         |    sparkVersion     : ${submitRequest.sparkVersion.version}
         |    appName          : ${submitRequest.effectiveAppName}
         |    devMode          : ${submitRequest.developmentMode.name()}
         |    execMode         : ${submitRequest.executionMode.name()}
         |    applicationType  : ${submitRequest.applicationType.getName}
         |    properties       : ${submitRequest.properties.mkString(" ")}
         |    args             : ${submitRequest.args}
         |    appConf          : ${submitRequest.appConf}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    prepareConfig(submitRequest)

    setConfig(submitRequest)

    Try(doSubmit(submitRequest)) match {
      case Success(resp) => resp
      case Failure(e) =>
        logError(
          s"spark job ${submitRequest.appName} start failed, " +
            s"executionMode: ${submitRequest.executionMode.getName}, " +
            s"detail: ${ExceptionUtils.stringifyException(e)}")
        throw e
    }
  }

  def setConfig(submitRequest: SubmitRequest): Unit

  @throws[Exception]
  def stop(stopRequest: StopRequest): StopResponse = {
    logInfo(
      s"""
         |----------------------------------------- spark job stop ----------------------------------
         |     userSparkHome     : ${stopRequest.sparkVersion.sparkHome}
         |     sparkVersion      : ${stopRequest.sparkVersion.version}
         |     jobId             : ${stopRequest.jobId}
         |-------------------------------------------------------------------------------------------
         |""".stripMargin)

    doStop(stopRequest)
  }

  @throws[Exception]
  def doSubmit(submitRequest: SubmitRequest): SubmitResponse

  @throws[Exception]
  def doStop(stopRequest: StopRequest): StopResponse

  private def prepareConfig(submitRequest: SubmitRequest): Unit = {
    // 1) set default config
    val userConfig = submitRequest.properties.filter(c => {
      val k = c._1
      if (k.startsWith("spark.")) {
        true
      } else {
        logger.warn("[StreamPark] config {} doesn't start with \"spark.\" Skip it.", k)
        false
      }
    })
    val defaultConfig = submitRequest.DEFAULT_SUBMIT_PARAM.filter(c => !userConfig.containsKey(c._1))
    submitRequest.properties.clear()
    submitRequest.properties.putAll(userConfig)
    submitRequest.properties.putAll(defaultConfig)
  }

}
