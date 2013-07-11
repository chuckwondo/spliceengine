package com.splicemachine.encoding;

import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.nio.ByteBuffer;

/**
 * @author Scott Fines
 * Created on: 6/10/13
 */
public class MultiFieldEncoder {
    private final byte[][] fields;
    private final int numFields;
    private int currentPos;
    private int currentSize;
    private int initialPos;
    private int initalSize;

    private MultiFieldEncoder(int numFields){
        fields = new byte[numFields][];
        this.numFields = numFields;
        this.initialPos=0;
        this.initalSize=0;

        //initialize ourselves
        reset();
    }

    public static MultiFieldEncoder create(int numFields){
        return new MultiFieldEncoder(numFields);
    }

    public MultiFieldEncoder encodeNext(boolean value){
        assert currentPos<fields.length;
        byte[] next = ScalarEncoding.toBytes(value,false);
        currentSize+=next.length;
        fields[currentPos] = next;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(boolean value,boolean desc){
        assert currentPos<fields.length;
        byte[] next = ScalarEncoding.toBytes(value, desc);
        currentSize+=next.length;
        fields[currentPos] = next;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(short value){
        assert currentPos<fields.length;
        byte[] next = ScalarEncoding.toBytes(value, false);
        currentSize+=next.length;
        fields[currentPos] = next;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(short value,boolean desc){
        assert currentPos<fields.length;
        byte[]  next = ScalarEncoding.toBytes(value, desc);
        currentSize+=next.length;
        fields[currentPos] = next;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(int value){
        assert currentPos<fields.length;
        byte[]  next = ScalarEncoding.toBytes(value, false);
        currentSize+=next.length;
        fields[currentPos] = next;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(int value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = ScalarEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;

    }

    public MultiFieldEncoder encodeNext(long value){
        assert currentPos<fields.length;
        byte[] bytes = ScalarEncoding.toBytes(value, false);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(long value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = ScalarEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(float value){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, false);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(float value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;

    }

    public MultiFieldEncoder encodeNext(double value){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, false);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(double value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(BigDecimal value){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, false);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(BigDecimal value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = DecimalEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(String value){
        assert currentPos<fields.length;
        byte[] bytes = StringEncoding.toBytes(value, false);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(String value,boolean desc){
        assert currentPos<fields.length;
        byte[] bytes = StringEncoding.toBytes(value, desc);
        currentSize+=bytes.length;
        fields[currentPos] = bytes;
        currentPos++;
        return this;
    }

    /**
     * WARNING: This encoding is <em>not</em> sortable, and will <em>not</em> retain
     * the sort order of the original byte[]. Only use this if sorting that byte[] is unnecessary.
     *
     * @param value the value to be encoded
     * @return a MultiFieldEncoder with {@code value} set in the next available
     * position.
     */
    public MultiFieldEncoder encodeNextUnsorted(byte[] value){
        assert currentPos<fields.length;
        //append a length field
        byte[] total = Encoding.encodeBytesUnsorted(value);

        currentSize+=total.length;
        fields[currentPos] = total;
        currentPos++;
        return this;
    }

    public MultiFieldEncoder encodeNext(byte[] value,boolean desc){
        assert currentPos<fields.length;
        byte[] encode = ByteEncoding.encode(value, desc);
        currentSize+=encode.length;
        fields[currentPos] = encode;
        currentPos++;
        return this;
    }

    public byte[] build(){
        //if you haven't tried to encode anything, return empty array
        if(currentPos==0) return new byte[0];

        byte[] data = new byte[currentSize+currentPos-1];
        int destPos=0;
        boolean isFirst = true;
        for(int srcPos=0;srcPos<currentPos;srcPos++){
            byte[] src = fields[srcPos];
            //TODO -sf- should we blow up here instead?
            if(!isFirst){
                data[destPos] = 0x00; //we know that 0x00 is never allowed, so it's a safe terminator
                destPos++;
            } else{
                isFirst=false;
            }
            if(src==null||src.length==0)
                continue;

            System.arraycopy(src,0,data,destPos,src.length);
            destPos+=src.length;
        }
        return data;
    }

    public void reset(){
        /*
         * Rather than waste time manually clearing data, just write over the positions
         * as we need to. Any remaining garbage in the array will get destroyed along with this object
         * then. Just make sure we don't keep one of these around for forever without using it repeatedly.
         */
        currentPos=initialPos;
        currentSize=initalSize;
    }

    public void mark(){
        initialPos=currentPos;
        initalSize=currentSize;
    }

    public byte[] getEncodedBytes(int i) {
        Preconditions.checkArgument(i<currentPos,"No bytes available in the current encoder");
        return fields[i];
    }

    public MultiFieldEncoder setRawBytes(byte[] keyPrefix) {
        assert currentPos < numFields;
        fields[currentPos] = keyPrefix;
        currentPos++;
        currentSize+=keyPrefix!=null?keyPrefix.length: 0;
        return this;
    }

    public MultiFieldEncoder setRawBuffer(ByteBuffer buffer){
        assert currentPos < numFields;
        byte[] bytes;
        if(buffer==null)
            bytes= null;
        else{
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return setRawBytes(bytes);
    }

    public MultiFieldEncoder encodeEmpty() {
        fields[currentPos] = null;
        currentPos++;
        return this;
    }
}
