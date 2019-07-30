/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.pinterest.secor.common;

import com.pinterest.secor.message.Message;
import org.apache.thrift.TException;
import com.pinterest.secor.timestamp.KafkaMessageTimestampFactory;
import kafka.api.FetchRequestBuilder;
import kafka.api.PartitionOffsetRequestInfo;
import kafka.common.TopicAndPartition;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.OffsetRequest;
import kafka.javaapi.OffsetResponse;
import kafka.javaapi.PartitionMetadata;
import kafka.javaapi.TopicMetadata;
import kafka.javaapi.TopicMetadataRequest;
import kafka.javaapi.TopicMetadataResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.message.MessageAndOffset;
import org.apache.kafka.common.protocol.Errors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public interface KafkaClient {
    int getNumPartitions(String topic);

    Message getCommittedMessage(TopicPartition topicPartition) throws Exception;

    public Message getLastMessage(TopicPartition topicPartition) throws TException {
        SimpleConsumer consumer = null;
        try {
            consumer = createConsumer(topicPartition);
            long lastOffset = findLastOffset(topicPartition, consumer);
            if (lastOffset < 1) {
                return null;
            }
            return getMessage(topicPartition, lastOffset, consumer);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    void init(SecorConfig config);
}
