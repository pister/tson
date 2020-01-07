package com.github.pister.tson.parse;

import com.github.pister.tson.io.FastBufferedReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class LexerReader {

    private FastBufferedReader reader;

    public LexerReader(Reader reader) {
        this.reader = new FastBufferedReader(reader);
    }

    private LinkedList<Integer> bufferQueue = new LinkedList<Integer>();

    public int nextChar() {
        if (!bufferQueue.isEmpty()) {
            return bufferQueue.removeFirst();
        }
        try {
            return reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void pushBack(int c) {
        bufferQueue.addFirst(c);
    }

    public int peek() {
        int c = nextChar();
        pushBack(c);
        return c;
    }
}
