package com.github.pister.tson.utils;

import java.util.Arrays;

/**
 * Base33是一种base32的变种，用数字1替换了base32默认的PAD符号
 *
 * 总容量约扩大了 8/5 = 1.6 倍
 * Created by songlihuang on 2019/1/25.
 */
public class Base33 {

    private static final byte PAD = '1';

    /**
     * BASE32 characters are 5 bits in length.
     * They are formed by taking a block of five octets to form a 40-bit string,
     * which is converted into eight BASE32 characters.
     */
    private static final int BITS_PER_ENCODED_BYTE = 5;
    private static final int BYTES_PER_ENCODED_BLOCK = 8;
    private static final int BYTES_PER_UNENCODED_BLOCK = 5;


    private static final int EOF = -1;

    private static final int MASK_8BITS = 0xff;
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final int DEFAULT_BUFFER_RESIZE_FACTOR = 2;

    /**
     * This array is a lookup table that translates Unicode characters drawn from the "Base32 Alphabet" (as specified
     * in Table 3 of RFC 4648) into their 5-bit positive integer equivalents. Characters that are not in the Base32
     * alphabet but fall within the bounds of the array are translated to -1.
     */
    private static final byte[] DECODE_TABLE = {
            //  0   1   2   3   4   5   6   7   8   9   A   B   C   D   E   F
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 00-0f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 10-1f
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, // 20-2f
            -1, -1, 26, 27, 28, 29, 30, 31, -1, -1, -1, -1, -1, -1, -1, -1, // 30-3f 2-7
            -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, // 40-4f A-N
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25,                     // 50-5a O-Z
    };

    /**
     * This array is a lookup table that translates 5-bit positive integer index values into their "Base32 Alphabet"
     * equivalents as specified in Table 3 of RFC 4648.
     */
    private static final byte[] ENCODE_TABLE = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            '2', '3', '4', '5', '6', '7',
    };

    /**
     * Mask used to extract 5 bits, used when encoding Base32 bytes
     */
    private static final int MASK_5BITS = 0x1f;

    /**
     * Decode table to use.
     */
    private static final byte[] decodeTable = DECODE_TABLE;

    /**
     * Convenience variable to help us determine when our buffer is going to run out of room and needs resizing.
     * <code>encodeSize = {@link #BYTES_PER_ENCODED_BLOCK} + lineSeparator.length;</code>
     */
    private static final int encodeSize = BYTES_PER_ENCODED_BLOCK;

    /**
     * Convenience variable to help us determine when our buffer is going to run out of room and needs resizing.
     * <code>decodeSize = {@link #BYTES_PER_ENCODED_BLOCK} - 1 + lineSeparator.length;</code>
     */
    private static final int decodeSize = BYTES_PER_ENCODED_BLOCK - 1;

    /**
     * Encode table to use.
     */
    private static final byte[] encodeTable = ENCODE_TABLE;

    /**
     * Line separator for encoding. Not used when decoding. Only used if lineLength > 0.
     */
    private static final byte[] lineSeparator = {}; //CHUNK_SEPARATOR;


    private static byte[] ensureBufferSize(final int size, final Context context) {
        if ((context.buffer == null) || (context.buffer.length < context.pos + size)) {
            return resizeBuffer(context);
        }
        return context.buffer;
    }

    private static byte[] resizeBuffer(final Context context) {
        if (context.buffer == null) {
            context.buffer = new byte[DEFAULT_BUFFER_SIZE];
            context.pos = 0;
            context.readPos = 0;
        } else {
            final byte[] b = new byte[context.buffer.length * DEFAULT_BUFFER_RESIZE_FACTOR];
            System.arraycopy(context.buffer, 0, b, 0, context.buffer.length);
            context.buffer = b;
        }
        return context.buffer;
    }

    /**
     * <p>
     * Decodes all of the provided data, starting at inPos, for inAvail bytes. Should be called at least twice: once
     * with the data to decode, and once with inAvail set to "-1" to alert decoder that EOF has been reached. The "-1"
     * call is not necessary when decoding, but it doesn't hurt, either.
     * </p>
     * <p>
     * Ignores all non-Base32 characters. This is how chunked (e.g. 76 character) data is handled, since CR and LF are
     * silently ignored, but has implications for other bytes, too. This method subscribes to the garbage-in,
     * garbage-out philosophy: it will not check the provided data for validity.
     * </p>
     *
     * @param in      byte[] array of ascii data to Base32 decode.
     * @param inPos   Position to start reading data from.
     * @param inAvail Amount of bytes available from input for encoding.
     * @param context the context to be used
     *                <p>
     *                Output is written to {@link Context#buffer} as 8-bit octets, using {@link Context#pos} as the buffer position
     */
    static void decode(final byte[] in, int inPos, final int inAvail, final Context context) {
        // package protected for access from I/O streams

        if (context.eof) {
            return;
        }
        if (inAvail < 0) {
            context.eof = true;
        }
        for (int i = 0; i < inAvail; i++) {
            final byte b = in[inPos++];
            if (b == PAD) {
                // We're done.
                context.eof = true;
                break;
            } else {
                final byte[] buffer = ensureBufferSize(decodeSize, context);
                if (b >= 0 && b < decodeTable.length) {
                    final int result = decodeTable[b];
                    if (result >= 0) {
                        context.modulus = (context.modulus + 1) % BYTES_PER_ENCODED_BLOCK;
                        // collect decoded bytes
                        context.lbitWorkArea = (context.lbitWorkArea << BITS_PER_ENCODED_BYTE) + result;
                        if (context.modulus == 0) { // we can output the 5 bytes
                            buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 32) & MASK_8BITS);
                            buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 24) & MASK_8BITS);
                            buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 16) & MASK_8BITS);
                            buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 8) & MASK_8BITS);
                            buffer[context.pos++] = (byte) (context.lbitWorkArea & MASK_8BITS);
                        }
                    }
                }
            }
        }

        // Two forms of EOF as far as Base32 decoder is concerned: actual
        // EOF (-1) and first time '=' character is encountered in stream.
        // This approach makes the '=' padding characters completely optional.
        if (context.eof && context.modulus >= 2) { // if modulus < 2, nothing to do
            final byte[] buffer = ensureBufferSize(decodeSize, context);

            //  we ignore partial bytes, i.e. only multiples of 8 count
            switch (context.modulus) {
                case 2: // 10 bits, drop 2 and output one byte
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 2) & MASK_8BITS);
                    break;
                case 3: // 15 bits, drop 7 and output 1 byte
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 7) & MASK_8BITS);
                    break;
                case 4: // 20 bits = 2*8 + 4
                    context.lbitWorkArea = context.lbitWorkArea >> 4; // drop 4 bits
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 8) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea) & MASK_8BITS);
                    break;
                case 5: // 25bits = 3*8 + 1
                    context.lbitWorkArea = context.lbitWorkArea >> 1;
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 16) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 8) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea) & MASK_8BITS);
                    break;
                case 6: // 30bits = 3*8 + 6
                    context.lbitWorkArea = context.lbitWorkArea >> 6;
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 16) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 8) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea) & MASK_8BITS);
                    break;
                case 7: // 35 = 4*8 +3
                    context.lbitWorkArea = context.lbitWorkArea >> 3;
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 24) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 16) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea >> 8) & MASK_8BITS);
                    buffer[context.pos++] = (byte) ((context.lbitWorkArea) & MASK_8BITS);
                    break;
                default:
                    // modulus can be 0-7, and we excluded 0,1 already
                    throw new IllegalStateException("Impossible modulus " + context.modulus);
            }
        }
    }

    /**
     * <p>
     * Encodes all of the provided data, starting at inPos, for inAvail bytes. Must be called at least twice: once with
     * the data to encode, and once with inAvail set to "-1" to alert encoder that EOF has been reached, so flush last
     * remaining bytes (if not multiple of 5).
     * </p>
     *
     * @param in      byte[] array of binary data to Base32 encode.
     * @param inPos   Position to start reading data from.
     * @param inAvail Amount of bytes available from input for encoding.
     * @param context the context to be used
     */
    static void encode(final byte[] in, int inPos, final int inAvail, final Context context) {
        // package protected for access from I/O streams
        if (context.eof) {
            return;
        }
        // inAvail < 0 is how we're informed of EOF in the underlying data we're
        // encoding.
        if (inAvail < 0) {
            context.eof = true;
            if (0 == context.modulus) {
                return; // no leftovers to process and not using chunking
            }
            final byte[] buffer = ensureBufferSize(encodeSize, context);
            final int savedPos = context.pos;
            switch (context.modulus) { // % 5
                case 0:
                    break;
                case 1: // Only 1 octet; take top 5 bits then remainder
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 3) & MASK_5BITS]; // 8-1*5 = 3
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea << 2) & MASK_5BITS]; // 5-3=2
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    break;
                case 2: // 2 octets = 16 bits to use
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 11) & MASK_5BITS]; // 16-1*5 = 11
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 6) & MASK_5BITS]; // 16-2*5 = 6
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 1) & MASK_5BITS]; // 16-3*5 = 1
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea << 4) & MASK_5BITS]; // 5-1 = 4
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    break;
                case 3: // 3 octets = 24 bits to use
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 19) & MASK_5BITS]; // 24-1*5 = 19
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 14) & MASK_5BITS]; // 24-2*5 = 14
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 9) & MASK_5BITS]; // 24-3*5 = 9
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 4) & MASK_5BITS]; // 24-4*5 = 4
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea << 1) & MASK_5BITS]; // 5-4 = 1
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    buffer[context.pos++] = PAD;
                    break;
                case 4: // 4 octets = 32 bits to use
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 27) & MASK_5BITS]; // 32-1*5 = 27
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 22) & MASK_5BITS]; // 32-2*5 = 22
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 17) & MASK_5BITS]; // 32-3*5 = 17
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 12) & MASK_5BITS]; // 32-4*5 = 12
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 7) & MASK_5BITS]; // 32-5*5 =  7
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 2) & MASK_5BITS]; // 32-6*5 =  2
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea << 3) & MASK_5BITS]; // 5-2 = 3
                    buffer[context.pos++] = PAD;
                    break;
                default:
                    throw new IllegalStateException("Impossible modulus " + context.modulus);
            }
            context.currentLinePos += context.pos - savedPos; // keep track of current line position
            // if currentPos == 0 we are at the start of a line, so don't add CRLF
            if (context.currentLinePos > 0) { // add chunk separator if required
                System.arraycopy(lineSeparator, 0, buffer, context.pos, lineSeparator.length);
                context.pos += lineSeparator.length;
            }
        } else {
            for (int i = 0; i < inAvail; i++) {
                final byte[] buffer = ensureBufferSize(encodeSize, context);
                context.modulus = (context.modulus + 1) % BYTES_PER_UNENCODED_BLOCK;
                int b = in[inPos++];
                if (b < 0) {
                    b += 256;
                }
                context.lbitWorkArea = (context.lbitWorkArea << 8) + b; // BITS_PER_BYTE
                if (0 == context.modulus) { // we have enough bytes to create our output
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 35) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 30) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 25) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 20) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 15) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 10) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) (context.lbitWorkArea >> 5) & MASK_5BITS];
                    buffer[context.pos++] = encodeTable[(int) context.lbitWorkArea & MASK_5BITS];
                    context.currentLinePos += BYTES_PER_ENCODED_BLOCK;
                }
            }
        }
    }


    /**
     * Holds thread context so classes can be thread-safe.
     * <p>
     * This class is not itself thread-safe; each thread must allocate its own copy.
     */
    static class Context {

        /**
         * Place holder for the bytes we're dealing with for our based logic.
         * Bitwise operations store and extract the encoding or decoding from this variable.
         */
        int ibitWorkArea;

        /**
         * Place holder for the bytes we're dealing with for our based logic.
         * Bitwise operations store and extract the encoding or decoding from this variable.
         */
        long lbitWorkArea;

        /**
         * Buffer for streaming.
         */
        byte[] buffer;

        /**
         * Position where next character should be written in the buffer.
         */
        int pos;

        /**
         * Position where next character should be read from the buffer.
         */
        int readPos;

        /**
         * Boolean flag to indicate the EOF has been reached. Once EOF has been reached, this object becomes useless,
         * and must be thrown away.
         */
        boolean eof;

        /**
         * Variable tracks how many characters have been written to the current line. Only used when encoding. We use
         * it to make sure each encoded line never goes beyond lineLength (if lineLength > 0).
         */
        int currentLinePos;

        /**
         * Writes to the buffer only occur after every 3/5 reads when encoding, and every 4/8 reads when decoding. This
         * variable helps track that.
         */
        int modulus;

        Context() {
        }

        /**
         * Returns a String useful for debugging (especially within a debugger.)
         *
         * @return a String useful for debugging.
         */
        @SuppressWarnings("boxing") // OK to ignore boxing here
        @Override
        public String toString() {
            return String.format("%s[buffer=%s, currentLinePos=%s, eof=%s, ibitWorkArea=%s, lbitWorkArea=%s, " +
                            "modulus=%s, pos=%s, readPos=%s]", this.getClass().getSimpleName(), Arrays.toString(buffer),
                    currentLinePos, eof, ibitWorkArea, lbitWorkArea, modulus, pos, readPos);
        }
    }

    public static byte[] encode(final byte[] pArray) {
        if (pArray == null || pArray.length == 0) {
            return pArray;
        }
        final Context context = new Context();
        encode(pArray, 0, pArray.length, context);
        encode(pArray, 0, EOF, context); // Notify encoder of EOF.
        final byte[] buf = new byte[context.pos - context.readPos];
        readResults(buf, 0, buf.length, context);
        return buf;
    }

    public static byte[] decode(final byte[] pArray) {
        if (pArray == null || pArray.length == 0) {
            return pArray;
        }
        final Context context = new Context();
        decode(pArray, 0, pArray.length, context);
        decode(pArray, 0, EOF, context); // Notify decoder of EOF.
        final byte[] result = new byte[context.pos];
        readResults(result, 0, result.length, context);
        return result;
    }

    private static int readResults(final byte[] b, final int bPos, final int bAvail, final Context context) {
        if (context.buffer != null) {
            final int len = Math.min(available(context), bAvail);
            System.arraycopy(context.buffer, context.readPos, b, bPos, len);
            context.readPos += len;
            if (context.readPos >= context.pos) {
                context.buffer = null; // so hasData() will return false, and this method can return -1
            }
            return len;
        }
        return context.eof ? EOF : 0;
    }

    private static int available(final Context context) {  // package protected for access from I/O streams
        return context.buffer != null ? context.pos - context.readPos : 0;
    }

}
