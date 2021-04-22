/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package net.corda.crypto;

import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;

@org.apache.avro.specific.AvroGenerated
public class SecureHash extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -8904865298851200687L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"SecureHash\",\"namespace\":\"net.corda.crypto\",\"fields\":[{\"name\":\"algorithm\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"serverHash\",\"type\":\"bytes\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  private static SpecificData MODEL$ = new SpecificData();

  private static final BinaryMessageEncoder<SecureHash> ENCODER =
      new BinaryMessageEncoder<SecureHash>(MODEL$, SCHEMA$);

  private static final BinaryMessageDecoder<SecureHash> DECODER =
      new BinaryMessageDecoder<SecureHash>(MODEL$, SCHEMA$);

  /**
   * Return the BinaryMessageEncoder instance used by this class.
   * @return the message encoder used by this class
   */
  public static BinaryMessageEncoder<SecureHash> getEncoder() {
    return ENCODER;
  }

  /**
   * Return the BinaryMessageDecoder instance used by this class.
   * @return the message decoder used by this class
   */
  public static BinaryMessageDecoder<SecureHash> getDecoder() {
    return DECODER;
  }

  /**
   * Create a new BinaryMessageDecoder instance for this class that uses the specified {@link SchemaStore}.
   * @param resolver a {@link SchemaStore} used to find schemas by fingerprint
   * @return a BinaryMessageDecoder instance for this class backed by the given SchemaStore
   */
  public static BinaryMessageDecoder<SecureHash> createDecoder(SchemaStore resolver) {
    return new BinaryMessageDecoder<SecureHash>(MODEL$, SCHEMA$, resolver);
  }

  /**
   * Serializes this SecureHash to a ByteBuffer.
   * @return a buffer holding the serialized data for this instance
   * @throws java.io.IOException if this instance could not be serialized
   */
  public java.nio.ByteBuffer toByteBuffer() throws java.io.IOException {
    return ENCODER.encode(this);
  }

  /**
   * Deserializes a SecureHash from a ByteBuffer.
   * @param b a byte buffer holding serialized data for an instance of this class
   * @return a SecureHash instance decoded from the given buffer
   * @throws java.io.IOException if the given bytes could not be deserialized into an instance of this class
   */
  public static SecureHash fromByteBuffer(
      java.nio.ByteBuffer b) throws java.io.IOException {
    return DECODER.decode(b);
  }

  @Deprecated public java.lang.String algorithm;
  @Deprecated public java.nio.ByteBuffer serverHash;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public SecureHash() {}

  /**
   * All-args constructor.
   * @param algorithm The new value for algorithm
   * @param serverHash The new value for serverHash
   */
  public SecureHash(java.lang.String algorithm, java.nio.ByteBuffer serverHash) {
    this.algorithm = algorithm;
    this.serverHash = serverHash;
  }

  public org.apache.avro.specific.SpecificData getSpecificData() { return MODEL$; }
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return algorithm;
    case 1: return serverHash;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: algorithm = value$ != null ? value$.toString() : null; break;
    case 1: serverHash = (java.nio.ByteBuffer)value$; break;
    default: throw new IndexOutOfBoundsException("Invalid index: " + field$);
    }
  }

  /**
   * Gets the value of the 'algorithm' field.
   * @return The value of the 'algorithm' field.
   */
  public java.lang.String getAlgorithm() {
    return algorithm;
  }


  /**
   * Sets the value of the 'algorithm' field.
   * @param value the value to set.
   */
  public void setAlgorithm(java.lang.String value) {
    this.algorithm = value;
  }

  /**
   * Gets the value of the 'serverHash' field.
   * @return The value of the 'serverHash' field.
   */
  public java.nio.ByteBuffer getServerHash() {
    return serverHash;
  }


  /**
   * Sets the value of the 'serverHash' field.
   * @param value the value to set.
   */
  public void setServerHash(java.nio.ByteBuffer value) {
    this.serverHash = value;
  }

  /**
   * Creates a new SecureHash RecordBuilder.
   * @return A new SecureHash RecordBuilder
   */
  public static net.corda.crypto.SecureHash.Builder newBuilder() {
    return new net.corda.crypto.SecureHash.Builder();
  }

  /**
   * Creates a new SecureHash RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new SecureHash RecordBuilder
   */
  public static net.corda.crypto.SecureHash.Builder newBuilder(net.corda.crypto.SecureHash.Builder other) {
    if (other == null) {
      return new net.corda.crypto.SecureHash.Builder();
    } else {
      return new net.corda.crypto.SecureHash.Builder(other);
    }
  }

  /**
   * Creates a new SecureHash RecordBuilder by copying an existing SecureHash instance.
   * @param other The existing instance to copy.
   * @return A new SecureHash RecordBuilder
   */
  public static net.corda.crypto.SecureHash.Builder newBuilder(net.corda.crypto.SecureHash other) {
    if (other == null) {
      return new net.corda.crypto.SecureHash.Builder();
    } else {
      return new net.corda.crypto.SecureHash.Builder(other);
    }
  }

  /**
   * RecordBuilder for SecureHash instances.
   */
  @org.apache.avro.specific.AvroGenerated
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<SecureHash>
    implements org.apache.avro.data.RecordBuilder<SecureHash> {

    private java.lang.String algorithm;
    private java.nio.ByteBuffer serverHash;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(net.corda.crypto.SecureHash.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.algorithm)) {
        this.algorithm = data().deepCopy(fields()[0].schema(), other.algorithm);
        fieldSetFlags()[0] = other.fieldSetFlags()[0];
      }
      if (isValidValue(fields()[1], other.serverHash)) {
        this.serverHash = data().deepCopy(fields()[1].schema(), other.serverHash);
        fieldSetFlags()[1] = other.fieldSetFlags()[1];
      }
    }

    /**
     * Creates a Builder by copying an existing SecureHash instance
     * @param other The existing instance to copy.
     */
    private Builder(net.corda.crypto.SecureHash other) {
      super(SCHEMA$);
      if (isValidValue(fields()[0], other.algorithm)) {
        this.algorithm = data().deepCopy(fields()[0].schema(), other.algorithm);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.serverHash)) {
        this.serverHash = data().deepCopy(fields()[1].schema(), other.serverHash);
        fieldSetFlags()[1] = true;
      }
    }

    /**
      * Gets the value of the 'algorithm' field.
      * @return The value.
      */
    public java.lang.String getAlgorithm() {
      return algorithm;
    }


    /**
      * Sets the value of the 'algorithm' field.
      * @param value The value of 'algorithm'.
      * @return This builder.
      */
    public net.corda.crypto.SecureHash.Builder setAlgorithm(java.lang.String value) {
      validate(fields()[0], value);
      this.algorithm = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'algorithm' field has been set.
      * @return True if the 'algorithm' field has been set, false otherwise.
      */
    public boolean hasAlgorithm() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'algorithm' field.
      * @return This builder.
      */
    public net.corda.crypto.SecureHash.Builder clearAlgorithm() {
      algorithm = null;
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'serverHash' field.
      * @return The value.
      */
    public java.nio.ByteBuffer getServerHash() {
      return serverHash;
    }


    /**
      * Sets the value of the 'serverHash' field.
      * @param value The value of 'serverHash'.
      * @return This builder.
      */
    public net.corda.crypto.SecureHash.Builder setServerHash(java.nio.ByteBuffer value) {
      validate(fields()[1], value);
      this.serverHash = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'serverHash' field has been set.
      * @return True if the 'serverHash' field has been set, false otherwise.
      */
    public boolean hasServerHash() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'serverHash' field.
      * @return This builder.
      */
    public net.corda.crypto.SecureHash.Builder clearServerHash() {
      serverHash = null;
      fieldSetFlags()[1] = false;
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SecureHash build() {
      try {
        SecureHash record = new SecureHash();
        record.algorithm = fieldSetFlags()[0] ? this.algorithm : (java.lang.String) defaultValue(fields()[0]);
        record.serverHash = fieldSetFlags()[1] ? this.serverHash : (java.nio.ByteBuffer) defaultValue(fields()[1]);
        return record;
      } catch (org.apache.avro.AvroMissingFieldException e) {
        throw e;
      } catch (java.lang.Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumWriter<SecureHash>
    WRITER$ = (org.apache.avro.io.DatumWriter<SecureHash>)MODEL$.createDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  @SuppressWarnings("unchecked")
  private static final org.apache.avro.io.DatumReader<SecureHash>
    READER$ = (org.apache.avro.io.DatumReader<SecureHash>)MODEL$.createDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

  @Override protected boolean hasCustomCoders() { return true; }

  @Override public void customEncode(org.apache.avro.io.Encoder out)
    throws java.io.IOException
  {
    out.writeString(this.algorithm);

    out.writeBytes(this.serverHash);

  }

  @Override public void customDecode(org.apache.avro.io.ResolvingDecoder in)
    throws java.io.IOException
  {
    org.apache.avro.Schema.Field[] fieldOrder = in.readFieldOrderIfDiff();
    if (fieldOrder == null) {
      this.algorithm = in.readString();

      this.serverHash = in.readBytes(this.serverHash);

    } else {
      for (int i = 0; i < 2; i++) {
        switch (fieldOrder[i].pos()) {
        case 0:
          this.algorithm = in.readString();
          break;

        case 1:
          this.serverHash = in.readBytes(this.serverHash);
          break;

        default:
          throw new java.io.IOException("Corrupt ResolvingDecoder.");
        }
      }
    }
  }
}










