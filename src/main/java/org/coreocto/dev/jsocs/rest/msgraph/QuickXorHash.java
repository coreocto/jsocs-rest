package org.coreocto.dev.jsocs.rest.msgraph;

import java.math.BigInteger;
import java.security.MessageDigest;

public class QuickXorHash extends MessageDigest implements Cloneable {

    private final static int BitsInLastCell = 32;
    private final static byte Shift = 11;
    private final static int WidthInBits = 160;

    private long[] _data;
    private long _lengthSoFar;
    private int _shiftSoFar;

    public QuickXorHash() {
        super("QuickXorHash");
        engineReset();
    }


    byte[] long2bytes(long l) {
        byte[] result = new byte[8];
        for (int i = 0; i < 8; ++i) {
            result[i] = (byte) (l & 0xFF);
            l >>= 8;
        }

        return result;
    }


    @Override
    protected byte[] engineDigest() {
        // Create a byte array big enough to hold all our data
        byte[] rgb = new byte[(QuickXorHash.WidthInBits - 1) / 8 + 1];

        // Block copy all our bitvectors to this byte array
        for (int i = 0; i < this._data.length - 1; i++) {
            System.arraycopy(
                    long2bytes(this._data[i]), 0,
                    rgb, i * 8,
                    8);
        }

        System.arraycopy(
                long2bytes(this._data[this._data.length - 1]), 0,
                rgb, (this._data.length - 1) * 8,
                rgb.length - (this._data.length - 1) * 8);

        // XOR the file length with the least significant bits
        // Note that GetBytes is architecture-dependent, so care should
        // be taken with porting. The expected value is 8-bytes in length in little-endian format
        byte[] lengthBytes = long2bytes(this._lengthSoFar);

        for (int i = 0; i < lengthBytes.length; i++) {
            rgb[(QuickXorHash.WidthInBits / 8) - lengthBytes.length + i] ^= lengthBytes[i];
        }

        return rgb;
    }

    @Override
    protected void engineReset() {
        this._data = new long[(QuickXorHash.WidthInBits - 1) / 64 + 1];
        this._shiftSoFar = 0;
        this._lengthSoFar = 0;
    }

    @Override
    protected void engineUpdate(byte arg0) {

    }

    @Override
    protected void engineUpdate(byte[] array, int ibStart, int cbSize) {

        int currentShift = this._shiftSoFar;

// The bitvector where we'll start xoring
        int vectorArrayIndex = currentShift / 64;

        // The position within the bit vector at which we begin xoring
        int vectorOffset = currentShift % 64;
        int iterations = Math.min(cbSize, QuickXorHash.WidthInBits);

        for (int i = 0; i < iterations; i++) {
            boolean isLastCell = vectorArrayIndex == this._data.length - 1;
            int bitsInVectorCell = isLastCell ? QuickXorHash.BitsInLastCell : 64;

            // There's at least 2 bitvectors before we reach the end of the array
            if (vectorOffset <= bitsInVectorCell - 8) {
                for (int j = ibStart + i; j < cbSize + ibStart; j += QuickXorHash.WidthInBits) {
                    this._data[vectorArrayIndex] ^= ((long) array[j] & 0xff) << vectorOffset;
                }
            } else {
                int index1 = vectorArrayIndex;
                int index2 = isLastCell ? 0 : (vectorArrayIndex + 1);
                byte low = (byte) (bitsInVectorCell - vectorOffset);

                long xoredByte = 0;
                for (int j = ibStart + i; j < cbSize + ibStart; j += QuickXorHash.WidthInBits) {
                    xoredByte ^= ((long) array[j] & 0xff);
                }
                this._data[index1] ^= xoredByte << vectorOffset;
                this._data[index2] ^= xoredByte >> low;

            }
            vectorOffset += QuickXorHash.Shift;
            while (vectorOffset >= bitsInVectorCell) {
                vectorArrayIndex = isLastCell ? 0 : vectorArrayIndex + 1;
                vectorOffset -= bitsInVectorCell;
            }
        }

        // Update the starting position in a circular shift pattern
        this._shiftSoFar = (this._shiftSoFar + QuickXorHash.Shift * (cbSize % QuickXorHash.WidthInBits)) % QuickXorHash.WidthInBits;


        this._lengthSoFar += cbSize;
    }
}
