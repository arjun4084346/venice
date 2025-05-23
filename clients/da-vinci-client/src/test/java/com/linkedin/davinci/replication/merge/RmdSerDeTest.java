package com.linkedin.davinci.replication.merge;

import static org.mockito.Mockito.mock;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import com.linkedin.davinci.replication.RmdWithValueSchemaId;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.schema.rmd.RmdSchemaEntry;
import com.linkedin.venice.schema.rmd.RmdSchemaGenerator;
import com.linkedin.venice.schema.rmd.v1.CollectionRmdTimestamp;
import com.linkedin.venice.serializer.RecordDeserializer;
import com.linkedin.venice.utils.collections.BiIntKeyCache;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryDecoder;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;


public class RmdSerDeTest {
  private static final int valueSchemaID = 1214;
  private static final int rmdVersionID = 1;
  private static final String storeName = "test_store_name";
  private RmdSerDe rmdSerDe;
  private ByteBuffer rmdAndValueSchemaIDBytes;
  private GenericRecord rmd;
  /**
   * A schema that contains primitive fields and collection fields, specifically, a list field and a map field.
   */
  private static final String VALUE_SCHEMA_STR = "{" + "   \"type\" : \"record\","
      + "   \"namespace\" : \"com.linkedin.avro\"," + "   \"name\" : \"Person\"," + "   \"fields\" : ["
      + "      { \"name\" : \"Name\" , \"type\" : \"string\", \"default\" : \"unknown\" },"
      + "      { \"name\" : \"Age\" , \"type\" : \"int\", \"default\" : -1 },"
      + "      { \"name\" : \"Items\" , \"type\" : {\"type\" : \"array\", \"items\" : \"string\"}, \"default\" : [] },"
      + "      { \"name\" : \"PetNameToAge\" , \"type\" : [\"null\" , {\"type\" : \"map\", \"values\" : \"int\"}], \"default\" : null }"
      + "   ]" + "}";

  @Test
  public void testSerDeRmd() {
    setupTestEnv();

    // Deserialize all bytes and expect to get value schema ID and RMD record back.
    RmdWithValueSchemaId rmdAndValueID = new RmdWithValueSchemaId();
    rmdSerDe.deserializeValueSchemaIdPrependedRmdBytes(rmdAndValueSchemaIDBytes.array(), rmdAndValueID);
    Assert.assertEquals(rmdAndValueID.getValueSchemaId(), valueSchemaID);
    Assert.assertEquals(rmdAndValueID.getRmdRecord(), rmd);
  }

  private void setupTestEnv() {
    // Generate RMD schema and record from value schema.
    Schema valueSchema = AvroCompatibilityHelper.parse(VALUE_SCHEMA_STR);
    Schema rmdSchema = RmdSchemaGenerator.generateMetadataSchema(valueSchema);

    // Prepare the object under test, mocks, etc
    ReadOnlySchemaRepository schemaRepository = mock(ReadOnlySchemaRepository.class);
    RmdSchemaEntry rmdSchemaEntry = mock(RmdSchemaEntry.class);
    Mockito.doReturn(rmdSchema).when(rmdSchemaEntry).getSchema();
    Mockito.doReturn(rmdSchemaEntry)
        .when(schemaRepository)
        .getReplicationMetadataSchema(storeName, valueSchemaID, rmdVersionID);
    StringAnnotatedStoreSchemaCache stringAnnotatedStoreSchemaCache =
        new StringAnnotatedStoreSchemaCache(storeName, schemaRepository);
    rmdSerDe = new RmdSerDe(stringAnnotatedStoreSchemaCache, rmdVersionID);
    Schema annotateRmdSchema = stringAnnotatedStoreSchemaCache.getRmdSchema(valueSchemaID, rmdVersionID).getSchema();
    rmd = createRmdWithCollectionTimestamp(annotateRmdSchema);

    // Serialize this RMD record to bytes.
    Schema actualRmdSchema = rmdSerDe.getRmdSchema(valueSchemaID);
    Assert.assertEquals(actualRmdSchema, annotateRmdSchema);
    ByteBuffer rmdBytes = rmdSerDe.serializeRmdRecord(valueSchemaID, rmd);

    // Prepend value schema ID to RMD bytes.
    rmdAndValueSchemaIDBytes = ByteBuffer.allocate(Integer.BYTES + rmdBytes.remaining());
    rmdAndValueSchemaIDBytes.putInt(valueSchemaID);
    rmdAndValueSchemaIDBytes.put(rmdBytes.array());
  }

  /**
   * Test that when deserializer cache doesn't have the deserializer, it will retry 5 times to get the
   * deserializer from the cache.
   */
  @Test
  public void testRetryInGetRmdDeserializer() {
    setupTestEnv();

    BiIntKeyCache<RecordDeserializer<GenericRecord>> mockDeserializerCache = mock(BiIntKeyCache.class);
    rmdSerDe.setDeserializerCache(mockDeserializerCache);
    RecordDeserializer mockDes = mock(RecordDeserializer.class);

    // Mock to fail twice on BiIntKeyCache.get() and succeed on the third time.
    Mockito.doReturn(rmd).when(mockDes).deserialize((BinaryDecoder) Mockito.any());
    Mockito.doThrow(new VeniceException("Mocked exception"))
        .doThrow(new VeniceException("Mocked exception"))
        .doReturn(mockDes)
        .when(mockDeserializerCache)
        .get(Mockito.anyInt(), Mockito.anyInt());

    // Deserialize all bytes and expect to get value schema ID and RMD record back.
    RmdWithValueSchemaId rmdAndValueID = new RmdWithValueSchemaId();
    rmdSerDe.deserializeValueSchemaIdPrependedRmdBytes(rmdAndValueSchemaIDBytes.array(), rmdAndValueID);
    Assert.assertEquals(rmdAndValueID.getValueSchemaId(), valueSchemaID);
    Assert.assertEquals(rmdAndValueID.getRmdRecord(), rmd);
  }

  /**
   * Test that when deserializer cache doesn't have the deserializer, it will throw an exception after 5 retries.
   */
  @Test(expectedExceptions = VeniceException.class)
  public void testRetryInGetRmdDeserializerWithException() {
    setupTestEnv();

    BiIntKeyCache<RecordDeserializer<GenericRecord>> mockDeserializerCache = mock(BiIntKeyCache.class);
    rmdSerDe.setDeserializerCache(mockDeserializerCache);

    // Mock to fail all the times on BiIntKeyCache.get().
    Mockito.doThrow(new VeniceException("Mocked exception"))
        .when(mockDeserializerCache)
        .get(Mockito.anyInt(), Mockito.anyInt());

    // Deserialize all bytes and expect to get value schema ID and RMD record back.
    RmdWithValueSchemaId rmdAndValueID = new RmdWithValueSchemaId();
    rmdSerDe.deserializeValueSchemaIdPrependedRmdBytes(rmdAndValueSchemaIDBytes.array(), rmdAndValueID);
  }

  private GenericRecord createRmdWithCollectionTimestamp(Schema rmdSchema) {
    Schema rmdTimestampSchema = rmdSchema.getField("timestamp").schema().getTypes().get(1);
    GenericRecord rmdTimestamp = new GenericData.Record(rmdTimestampSchema);
    rmdTimestamp.put("Name", 1L);
    rmdTimestamp.put("Age", 1L);
    rmdTimestamp.put(
        "Items",
        createCollectionFieldMetadataRecord(
            rmdTimestampSchema.getField("Items").schema(),
            23L,
            1,
            3,
            Arrays.asList(1L, 2L, 3L),
            Arrays.asList("foo", "bar"),
            Arrays.asList(1L, 100L)));
    rmdTimestamp.put(
        "PetNameToAge",
        createCollectionFieldMetadataRecord(
            rmdTimestampSchema.getField("PetNameToAge").schema(),
            24L,
            2,
            5,
            Arrays.asList(1L, 2L, 3L, 4L, 5L),
            Arrays.asList("foo", "bar", "qaz"),
            Arrays.asList(1L, 2L, 3L)));
    GenericRecord rmd = new GenericData.Record(rmdSchema);
    rmd.put("timestamp", rmdTimestamp);
    rmd.put("replication_checkpoint_vector", Arrays.asList(1L, 2L, 3L));
    return rmd;
  }

  private GenericRecord createCollectionFieldMetadataRecord(
      Schema collectionFieldMetadataSchema,
      long topLevelTimestamp,
      int topLevelColoID,
      int putOnlyPartLen,
      List<Long> activeElementsTimestamps,
      List<Object> deletedElements,
      List<Long> deletedElementsTimestamps) {
    GenericRecord collectionFieldMetadataRecord = new GenericData.Record(collectionFieldMetadataSchema);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.TOP_LEVEL_TS_FIELD_NAME, topLevelTimestamp);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.TOP_LEVEL_TS_FIELD_NAME, topLevelTimestamp);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.TOP_LEVEL_COLO_ID_FIELD_NAME, topLevelColoID);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.PUT_ONLY_PART_LENGTH_FIELD_NAME, putOnlyPartLen);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.ACTIVE_ELEM_TS_FIELD_NAME, activeElementsTimestamps);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.DELETED_ELEM_FIELD_NAME, deletedElements);
    collectionFieldMetadataRecord.put(CollectionRmdTimestamp.DELETED_ELEM_TS_FIELD_NAME, deletedElementsTimestamps);
    return collectionFieldMetadataRecord;
  }
}
