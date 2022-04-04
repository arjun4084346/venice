package com.linkedin.venice.schema.merge;

import com.linkedin.venice.utils.lazy.Lazy;
import com.linkedin.venice.utils.lazy.LazyImpl;
import java.util.function.Supplier;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang.Validate;

import javax.annotation.Nonnull;


/**
 * Wrapper class to hold a pair of {@link GenericRecord}, including a value and its corresponding
 * replication metadata.
 */
public class ValueAndReplicationMetadata<T> {
  private Lazy<T> value;
  private GenericRecord replicationMetadata;
  private boolean updateIgnored; // Whether we should skip the incoming message since it could be a stale message.
  private int resolvedSchemaID;

  public ValueAndReplicationMetadata(Lazy<T> value, @Nonnull GenericRecord replicationMetadata) {
    Validate.notNull(replicationMetadata);
    this.value = value;
    this.replicationMetadata = replicationMetadata;
  }

  public T getValue() {
    return value.get();
  }

  public void setValue(T value) {
    this.value = Lazy.of(() -> value);
  }

  public GenericRecord getReplicationMetadata() {
    return replicationMetadata;
  }

  public void setReplicationMetadata(GenericRecord replicationMetadata) {
    this.replicationMetadata = replicationMetadata;
  }

  public void setUpdateIgnored(boolean updateIgnored) {
    this.updateIgnored = updateIgnored;
  }

  public boolean isUpdateIgnored() {
    return updateIgnored;
  }

  public void setResolvedSchemaID(int schemaID) {
    this.resolvedSchemaID = schemaID;
  }

  public int getResolvedSchemaID() {
    return this.resolvedSchemaID;
  }
}