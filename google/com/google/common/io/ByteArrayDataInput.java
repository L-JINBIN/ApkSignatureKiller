

package com.google.common.io;

import java.io.DataInput;


public interface ByteArrayDataInput extends DataInput {
    @Override
    void readFully(byte b[]);

    @Override
    void readFully(byte b[], int off, int len);

    @Override
    int skipBytes(int n);

    @Override
    boolean readBoolean();

    @Override
    byte readByte();

    @Override
    int readUnsignedByte();

    @Override
    short readShort();

    @Override
    int readUnsignedShort();

    @Override
    char readChar();

    @Override
    int readInt();

    @Override
    long readLong();

    @Override
    float readFloat();

    @Override
    double readDouble();

    @Override
    String readLine();

    @Override
    String readUTF();
}
