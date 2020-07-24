package com.github.pister.tson.utils;


import com.github.pister.tson.io.FastByteArrayInputStream;
import com.github.pister.tson.io.FastByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * base629 是一种二进制的字符串表示格式，最终转换的字符串只会包含a-z,A-Z,9-0中的这些字符。
 * <p>
 * 实现原理借鉴了base64的方案，由于a-z,A-Z,9-0只有62个字符，所以剩余的2个字符会用2个字符来表示，
 * 另外pad也会用2个字符来表示，这个特殊的转义的字符就是数字9.
 * 这就是base629名字的由来。
 * <p>
 * Created by songlihuang on 2020/6/23.
 */
public final class Base629 {

    private static final int BUF_SIZE = 4 * 1024;

    private static final int ENCODE_MODE_SIZE = 3;

    private static final int DECODE_MODE_SIZE = 4;

    private static final char VAL_61 = '\uFF91';
    private static final char VAL_62 = '\uFF92';
    private static final char VAL_63 = '\uFF93';

    private static final byte[] BYTES_61 = {'9', '1'};
    private static final byte[] BYTES_62 = {'9', '2'};
    private static final byte[] BYTES_63 = {'9', '3'};


    private static final char[] ENCODE_MAP = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', VAL_61, VAL_62, VAL_63,
    };

    private static final byte[][] TAIL1_MAP = {
            {'9', '5'}, {'9', '6'}, {'9', '7'}, {'9', '8'}
    };

    private static final byte[][] TAIL2_MAP = {
            {'9', 'A'}, {'9', 'B'}, {'9', 'C'}, {'9', 'D'}, {'9', 'E'}, {'9', 'F'}, {'9', 'G'}, {'9', 'H'},
            {'9', 'I'}, {'9', 'J'}, {'9', 'K'}, {'9', 'L'}, {'9', 'M'}, {'9', 'N'}, {'9', 'O'}, {'9', 'P'}
    };


    private Base629() {
    }

    public static void encode(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] buf = new byte[BUF_SIZE];
        Base629Encoder encoder = new Base629Encoder(new OutputStreamBytesAppender(outputStream));
        for (; ; ) {
            int len = inputStream.read(buf, 0, BUF_SIZE);
            if (len < 0) {
                break;
            }
            encoder.doUpdate(buf, 0, len);
        }
        encoder.doFinal();
    }

    public static byte[] encode(byte[] bytes) {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream(1024);
        try {
            encode(new FastByteArrayInputStream(bytes), outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void decode(InputStream inputStream, OutputStream outputStream) throws IOException {
        Base629Decoder base629Decoder = new Base629Decoder(new OutputStreamBytesAppender(outputStream), inputStream);
        base629Decoder.decode();
    }

    public static byte[] decode(byte[] bytes) {
        FastByteArrayOutputStream outputStream = new FastByteArrayOutputStream(1024);
        try {
            decode(new FastByteArrayInputStream(bytes), outputStream);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class Base629CodecBase {

        protected BytesAppender bytesAppender;

        protected SizeLimitedBytesQueue bytesQueue;

        protected static final int QUEUE_SIZE = 1024 * 2;

        protected static final int BATCH_SIZE = QUEUE_SIZE / 4;

    }

    private enum Handle9Result {
        NORMA_VAL,
        TAIL_1,
        TAIL_2;
    }

    private static class Base629Decoder extends Base629CodecBase {

        private BufferedByteReader bufferedByteReader;

        private byte[] bytesBuf = new byte[DECODE_MODE_SIZE];

        private int tail1Val;
        private int tail2Val;

        public Base629Decoder(BytesAppender bytesAppender, InputStream inputStream) {
            this.bytesAppender = bytesAppender;
            this.bytesQueue = new SizeLimitedBytesQueue(QUEUE_SIZE);
            this.bufferedByteReader = new BufferedByteReader(inputStream);
        }

        private int handle9Char() throws IOException {
            int b1 = bufferedByteReader.nextByte();
            if (b1 < 0) {
                throw new IllegalArgumentException("need a char after '9'");
            }
            switch (b1) {
                case '1':
                    return 61;
                case '2':
                    return 62;
                case '3':
                    return 63;
                default:
                    throw new IllegalArgumentException("illegal char after '9':" + (char)b1);
            }
        }

        private Handle9Result handle9() throws IOException {
            int b1 = bufferedByteReader.nextByte();
            if (b1 < 0) {
                throw new IllegalArgumentException("need a char after '9'");
            }
            switch (b1) {
                case '1':
                    bytesQueue.addLast((byte)61);
                    return Handle9Result.NORMA_VAL;
                case '2':
                    bytesQueue.addLast((byte)62);
                    return Handle9Result.NORMA_VAL;
                case '3':
                    bytesQueue.addLast((byte)63);
                    return Handle9Result.NORMA_VAL;
                case '5':
                case '6':
                case '7':
                case '8':
                    tail1Val = b1 - '5';
                    return Handle9Result.TAIL_1;
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                    tail2Val = b1 - 'A';
                    return Handle9Result.TAIL_2;
                default:
                    throw new IllegalArgumentException("illegal char after '9'");
            }
        }

        private int handleChar(int b) throws IOException {
            if (b >= 'A' && b <= 'Z') {
                return b - 'A';
            }
            if (b >= 'a' && b <= 'z') {
                return b - 'a' + 26;
            }
            if (b >= '0' && b <= '8') {
                return b - '0' + 52;
            }
            if (b == '9') {
                return handle9Char();
            }
            throw new IllegalArgumentException("illegal char " + (char)b);
        }

        private void decodeBuf(byte[] inputBuf, int pos) {
            // 111111, 110000, 000000, 000000
            // b0 = (c0(00111111) << 2 | c1(00110000))
            int b0 = ((inputBuf[pos] & 0x3F) << 2) | ((inputBuf[pos + 1] & 0x30) >> 4);
            // 000000, 001111, 111100, 000000
            // b1 = (c1(00001111) << 4) | c2(00111100) >> 4
            int b1 = ((inputBuf[pos + 1] & 0x0F) << 4) | ((inputBuf[pos + 2] & 0x3C) >> 2);
            // 000000, 000000, 000011, 111111
            int b2 = ((inputBuf[pos + 2] & 0x03) << 6) | inputBuf[pos + 3];

            bytesAppender.append((byte)b0);
            bytesAppender.append((byte)b1);
            bytesAppender.append((byte)b2);
        }

        private void decodeBytes() {
            while (bytesQueue.size() >= DECODE_MODE_SIZE) {
                bytesQueue.removeFirst(bytesBuf);
                decodeBuf(bytesBuf, 0);
            }
        }

        private void handleTail(Handle9Result handle9Result) throws IOException {
            decodeBytes();
            int b1, b2;
            int x1, x2;
            switch (handle9Result) {
                case TAIL_1:
                    b1 = bufferedByteReader.nextByte();
                    if (b1 < 0) {
                        throw new IllegalArgumentException("miss char after tail1");
                    }
                    // 11000000
                    // 00111111
                    b1 = handleChar(b1);
                    x1 = ((tail1Val << 6) | (b1 & 0x3F));
                    bytesAppender.append((byte) x1);
                    break;
                case TAIL_2:
                    b1 = bufferedByteReader.nextByte();
                    if (b1 < 0) {
                        throw new IllegalArgumentException("miss char after tail2");
                    }
                    b1 = handleChar(b1);
                    // x1 = tail2(1111) | b1(111100)
                    x1 = (tail2Val << 4) | ((b1 & 0x3C) >> 2);
                    bytesAppender.append((byte)x1);
                    b2 = bufferedByteReader.nextByte();
                    if (b2 < 0) {
                        throw new IllegalArgumentException("miss char after tail2");
                    }
                    b2 = handleChar(b2);
                    // x2 = b1(11) | b2(111111)
                    x2 = ((b1 & 0x03) << 6) | (b2 & 0x3F);
                    bytesAppender.append((byte)x2);
                    break;
            }

        }

        public void decode() throws IOException {
            Handle9Result handle9Result;
            for (;;) {
                int b = bufferedByteReader.nextByte();
                if (b < 0) {
                    break;
                }
                if (b == '9') {
                    handle9Result = handle9();
                    if (handle9Result != Handle9Result.NORMA_VAL) {
                        handleTail(handle9Result);
                        break;
                    }
                } else {
                    int val = handleChar(b);
                    bytesQueue.addLast((byte)val);
                }
                decodeBytes();
            }
        }


    }

    private static class Base629Encoder extends Base629CodecBase {

        private byte[] bytesBuf = new byte[ENCODE_MODE_SIZE];

        public Base629Encoder(BytesAppender bytesAppender) {
            this.bytesAppender = bytesAppender;
            this.bytesQueue = new SizeLimitedBytesQueue(QUEUE_SIZE);
        }

        private void appendChar(int b) {
            char c = ENCODE_MAP[b];
            switch (c) {
                case VAL_61:
                    bytesAppender.append(BYTES_61);
                    break;
                case VAL_62:
                    bytesAppender.append(BYTES_62);
                    break;
                case VAL_63:
                    bytesAppender.append(BYTES_63);
                    break;
                default:
                    bytesAppender.append((byte) c);
                    break;
            }
        }


        private void encodeBuf(byte[] inputBuf, int pos) {
            // 0-7, 8-15, 16-23
            // ===============>
            // 0-5, 6-11, 12-17, 18-23
            // 11111100 = 0xFC
            int b0 = (inputBuf[pos] & 0xFC) >> 2;
            // byte0:00000011 = 0x03
            // byte1:11110000 = 0xF0
            int b1 = ((inputBuf[pos] & 0x03) << 4) | ((inputBuf[pos + 1] & 0xF0) >> 4);
            // byte1:00001111 = 0x0F
            // byte2:11000000 = 0xC0
            int b2 = ((inputBuf[pos + 1] & 0x0F) << 2) | ((inputBuf[pos + 2] & 0xC0) >> 6);
            // byte2:00111111 = 0x3F
            int b3 = (inputBuf[pos + 2] & 0x3F);
            appendChar(b0);
            appendChar(b1);
            appendChar(b2);
            appendChar(b3);
        }

        private void encodeTail1(byte[] inputBuf, int pos) {
            // 11000000
            int b0 = (inputBuf[pos] & 0xC0) >> 6;
            // 00111111
            int b1 = (inputBuf[pos] & 0x3F);
            byte[] bytes = TAIL1_MAP[b0];
            bytesAppender.append(bytes);
            appendChar(b1);
        }

        private void encodeTail2(byte[] inputBuf, int pos) {
            // 11110000, 00000000
            int b0 = (inputBuf[pos] & 0xF0) >> 4;
            // 00001111, 11000000
            int b1 = ((inputBuf[pos] & 0x0F) << 2) | ((inputBuf[pos + 1] & 0xC0) >> 6);
            // 00000000, 00111111
            int b2 = (inputBuf[pos + 1] & 0x3F);
            byte[] bytes = TAIL2_MAP[b0];
            bytesAppender.append(bytes);
            appendChar(b1);
            appendChar(b2);
        }

        public void doUpdate(byte[] buf, int offset, int len) throws IOException {
            int i = 0;
            for (; ; ) {
                int size = Math.min(BATCH_SIZE, len - i);
                if (size <= 0) {
                    break;
                }
                bytesQueue.addLast(buf, offset + i, size);
                i += size;
                while (bytesQueue.size() >= 3) {
                    bytesQueue.removeFirst(bytesBuf);
                    encodeBuf(bytesBuf, 0);
                }
            }
        }

        public void doFinal() {
            int remainSize = bytesQueue.size();
            while (remainSize >= ENCODE_MODE_SIZE) {
                remainSize = bytesQueue.removeFirst(bytesBuf);
                encodeBuf(bytesBuf, 0);
            }
            bytesQueue.removeFirst(bytesBuf);
            switch (remainSize) {
                case 0:
                    return;
                case 1:
                    encodeTail1(bytesBuf, 0);
                    break;
                case 2:
                    encodeTail2(bytesBuf, 0);
                    break;
                default:
                    throw new IllegalStateException("must not be reach here");
            }
            bytesAppender.finish();
        }

    }

    public static abstract class BytesAppender {
        public abstract void append(byte b);

        public void append(byte[] buf) {
            for (byte b : buf) {
                append(b);
            }
        }

        public void append(byte[] buf, int pos, int len) {
            for (int i = 0; i < len; i++) {
                append(buf[pos + i]);
            }
        }

        public abstract void finish();
    }

    public static class BufferedByteReader {

        private static final int BUFFER_SIZE = 1024 * 2;

        private byte[] buffer;

        private int pos = 0;

        private int len = 0;

        private InputStream inputStream;

        public BufferedByteReader(InputStream inputStream) {
            this.inputStream = inputStream;
            this.buffer = new byte[BUFFER_SIZE];
        }

        public int nextByte() throws IOException {
            while (pos >= len) {
                len = inputStream.read(buffer);
                if (len < 0) {
                    return -1;
                }
                pos = 0;
            }
            return buffer[pos++];
        }

    }

    public static class OutputStreamBytesAppender extends BytesAppender {

        private OutputStream outputStream;

        public OutputStreamBytesAppender(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void append(byte b) {
            try {
                outputStream.write(b);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void append(byte[] buf, int pos, int len) {
            try {
                outputStream.write(buf, pos, len);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void finish() {
            try {
                outputStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class SizeLimitedBytesQueue {
        private byte[] data0;
        private byte[] data1;

        private byte[] currentData;

        private int first = 0;

        private int last = 0;

        private final int maxSize;

        public SizeLimitedBytesQueue(int maxSize) {
            this.maxSize = maxSize;
            data0 = new byte[maxSize];
            data1 = new byte[maxSize];
            currentData = data0;
        }

        public int size() {
            return last - first;
        }

        public void addLast(byte b) {
            if (size() + 1 > maxSize) {
                throw new IllegalStateException("byte queue is full");
            }
            while (last >= maxSize - 1) {
                flap();
            }
            currentData[last++] = b;
        }

        public void addLast(byte[] buf) {
            addLast(buf, 0, buf.length);
        }

        public void addLast(byte[] buf, int pos, int len) {
            if (size() + len > maxSize) {
                throw new IllegalStateException("byte queue is full");
            }
            while (last >= maxSize - len) {
                flap();
            }
            System.arraycopy(buf, pos, currentData, last, len);
            last += len;
        }

        public int removeFirst(byte[] buf) {
            return removeFirst(buf, 0, buf.length);
        }

        public int removeFirst(byte[] buf, int pos, int len) {
            int size = size();
            int copySize = Math.min(size, len);
            System.arraycopy(currentData, first, buf, pos, copySize);
            first += copySize;
            return copySize;
        }

        private void flap() {
            byte[] srcData, destData;
            if (currentData == data0) {
                srcData = data0;
                destData = data1;
                currentData = data1;
            } else {
                srcData = data1;
                destData = data0;
                currentData = data0;
            }
            int len = size();
            System.arraycopy(srcData, first, destData, 0, len);
            first = 0;
            last = len;
        }
    }

}
