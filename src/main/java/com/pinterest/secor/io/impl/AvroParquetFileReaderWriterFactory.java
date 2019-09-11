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
package com.pinterest.secor.io.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.google.protobuf.Message;
import com.pinterest.secor.common.SecorSchemaRegistryClient;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import com.pinterest.secor.common.LogFilePath;
import com.pinterest.secor.common.SecorConfig;
import com.pinterest.secor.io.FileReader;
import com.pinterest.secor.io.FileReaderWriterFactory;
import com.pinterest.secor.io.FileWriter;
import com.pinterest.secor.io.KeyValue;
import com.pinterest.secor.util.ParquetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvroParquetFileReaderWriterFactory implements FileReaderWriterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AvroParquetFileReaderWriterFactory.class);
    protected final int blockSize;
    protected final int pageSize;
    protected final boolean enableDictionary;
    protected final boolean validating;
    protected SecorSchemaRegistryClient schemaRegistryClient;

    public AvroParquetFileReaderWriterFactory(SecorConfig config) {
        blockSize = ParquetUtil.getParquetBlockSize(config);
        pageSize = ParquetUtil.getParquetPageSize(config);
        enableDictionary = ParquetUtil.getParquetEnableDictionary(config);
        validating = ParquetUtil.getParquetValidation(config);
        schemaRegistryClient = new SecorSchemaRegistryClient(config);
    }

    @Override
    public FileReader BuildFileReader(LogFilePath logFilePath, CompressionCodec codec) throws Exception {
        return new AvroParquetFileReader(logFilePath, codec);
    }

    @Override
    public FileWriter BuildFileWriter(LogFilePath logFilePath, CompressionCodec codec) throws Exception {
        return new AvroParquetFileWriter(logFilePath, codec);
    }

    protected class AvroParquetFileReader implements FileReader {

        private ParquetReader<GenericRecord> reader;
        private SpecificDatumWriter<GenericRecord> writer;
        private long offset;
        private String topic;

        public AvroParquetFileReader(LogFilePath logFilePath, CompressionCodec codec) throws IOException {
            Path path = new Path(logFilePath.getLogFilePath());
            topic = logFilePath.getTopic();
            reader = AvroParquetReader.<GenericRecord>builder(path).build();
            offset = logFilePath.getOffset();
        }

        @Override
        public KeyValue next() throws IOException {
            GenericRecord record = reader.read();
            if (record != null) {
                return new KeyValue(offset++, schemaRegistryClient.encodeMessage(topic, record));
            }
            return null;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }

    }

    protected class AvroParquetFileWriter implements FileWriter {

        private ParquetWriter writer;
        private String topic;
        private CompressionCodecName codecName;
        private Path path;

        public AvroParquetFileWriter(LogFilePath logFilePath, CompressionCodec codec) throws IOException {
            path = new Path(logFilePath.getLogFilePath());
            LOG.debug("Creating Brand new Writer for path {}", path);
            codecName = CompressionCodecName
                    .fromCompressionCodec(codec != null ? codec.getClass() : null);
            topic = logFilePath.getTopic();
            // Not setting blockSize, pageSize, enableDictionary, and validating

            // Lazily instantiate writer as a decode must happen first to populate registry
            writer = null;
        }

        private ParquetWriter getWriter() throws IOException {
            if (writer == null) {
                writer = AvroParquetWriter.builder(path)
                        .withSchema(schemaRegistryClient.getSchema(topic))
                        .withCompressionCodec(codecName)
                        .build();
            }
            return writer;
        }

        @Override
        public long getLength() throws IOException {
            return getWriter().getDataSize();
        }

        @Override
        public void write(KeyValue keyValue) throws IOException {
            GenericRecord record = schemaRegistryClient.decodeMessage(topic, keyValue.getValue());
            LOG.trace("Writing record {}", record);
            if (record != null){
                getWriter().write(record);
            }
        }

        @Override
        public void close() throws IOException {
            getWriter().close();
        }
    }
}

