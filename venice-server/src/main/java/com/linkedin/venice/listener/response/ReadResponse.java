package com.linkedin.venice.listener.response;

import com.linkedin.venice.compression.CompressionStrategy;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * This class is used to store common fields shared by various read responses.
 */
public abstract class ReadResponse {
  private double databaseLookupLatency = -1;
  private double readComputeLatency = -1;
  private double readComputeDeserializationLatency = -1;
  private double readComputeSerializationLatency = -1;
  private double storageExecutionSubmissionWaitTime;
  private int multiChunkLargeValueCount = 0;
  private CompressionStrategy compressionStrategy = CompressionStrategy.NO_OP;
  private boolean isStreamingResponse = false;
  private Optional<List<Integer>> optionalKeySizeList = Optional.empty();
  private Optional<List<Integer>> optionalValueSizeList = Optional.empty();
  private int dotProductCount = 0;
  private int cosineSimilarityCount = 0;
  private int hadamardProductCount = 0;

  public void setCompressionStrategy(CompressionStrategy compressionStrategy) {
    this.compressionStrategy = compressionStrategy;
  }

  public void setStreamingResponse() {
    this.isStreamingResponse = true;
  }

  public boolean isStreamingResponse() {
    return this.isStreamingResponse;
  }

  public CompressionStrategy getCompressionStrategy() {
    return compressionStrategy;
  }

  public void setDatabaseLookupLatency(double latency) {
    this.databaseLookupLatency = latency;
  }

  public void addDatabaseLookupLatency(double latency) {
    this.databaseLookupLatency += latency;
  }

  public double getDatabaseLookupLatency() {
    return this.databaseLookupLatency;
  }

  public void setReadComputeLatency(double latency) {
    this.readComputeLatency = latency;
  }

  public void addReadComputeLatency(double latency) {
    this.readComputeLatency += latency;
  }

  public double getReadComputeLatency() {
    return this.readComputeLatency;
  }

  public void setReadComputeDeserializationLatency(double latency) {
    this.readComputeDeserializationLatency = latency;
  }

  public void addReadComputeDeserializationLatency(double latency) {
    this.readComputeDeserializationLatency += latency;
  }

  public void setOptionalKeySizeList(Optional<List<Integer>> optionalKeySizeList) {
    this.optionalKeySizeList = optionalKeySizeList;
  }

  public void setOptionalValueSizeList(Optional<List<Integer>> optionalValueSizeList) {
    this.optionalValueSizeList = optionalValueSizeList;
  }

  public double getReadComputeDeserializationLatency() {
    return this.readComputeDeserializationLatency;
  }

  public void setReadComputeSerializationLatency(double latency) {
    this.readComputeSerializationLatency = latency;
  }

  public void addReadComputeSerializationLatency(double latency) {
    this.readComputeSerializationLatency += latency;
  }

  public void incrementDotProductCount() {
    dotProductCount++;
  }

  public void incrementCosineSimilarityCount() {
    cosineSimilarityCount++;
  }

  public void incrementHadamardProductCount() {
    hadamardProductCount++;
  }

  public double getReadComputeSerializationLatency() {
    return this.readComputeSerializationLatency;
  }

  public double getStorageExecutionHandlerSubmissionWaitTime() {
    return storageExecutionSubmissionWaitTime;
  }

  public void setStorageExecutionSubmissionWaitTime(double storageExecutionSubmissionWaitTime) {
    this.storageExecutionSubmissionWaitTime = storageExecutionSubmissionWaitTime;
  }

  public void incrementMultiChunkLargeValueCount() {
    multiChunkLargeValueCount++;
  }

  public int getMultiChunkLargeValueCount() {
    return multiChunkLargeValueCount;
  }

  public boolean isFound() {
    return true;
  }

  public Optional<List<Integer>> getOptionalKeySizeList() {
    return optionalKeySizeList;
  }

  public Optional<List<Integer>> getOptionalValueSizeList() {
    return optionalValueSizeList;
  }

  public int getDotProductCount() {
    return dotProductCount;
  }

  public int getCosineSimilarityCount() {
    return cosineSimilarityCount;
  }

  public int getHadamardProductCount() {
    return hadamardProductCount;
  }

  public abstract int getRecordCount();

  public abstract ByteBuf getResponseBody();

  public abstract int getResponseSchemaIdHeader();

  public abstract String getResponseOffsetHeader();
}
