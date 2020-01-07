package com.github.pister.tson.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by songlihuang on 2020/1/6.
 */
public class IoUtil {

    private static final int BUFFER_SIZE = 1024 * 4;

    public static byte[] readAll(InputStream inputStream) throws IOException {
        FastByteArrayOutputStream byteArrayOutputStream = new FastByteArrayOutputStream();
        byte[] buf = new byte[BUFFER_SIZE];
        while (true) {
            int len = inputStream.read(buf);
            if (len < 0) {
                break;
            }
            byteArrayOutputStream.write(buf, 0, len);
        }
        return byteArrayOutputStream.toByteArray();
    }


}
