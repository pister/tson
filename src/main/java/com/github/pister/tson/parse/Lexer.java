package com.github.pister.tson.parse;

import java.util.LinkedList;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class Lexer {

    private LexerImpl lexerImpl;

    private LinkedList<Token> bufferQueue = new LinkedList<Token>();

    public Lexer(LexerReader lexerReader) {
        lexerImpl = new LexerImpl(lexerReader);
    }

    public Token nextToken() {
        if (!bufferQueue.isEmpty()) {
            return bufferQueue.removeFirst();
        }
        return lexerImpl.nextToken();
    }

    public void pushBack(Token token) {
        bufferQueue.addFirst(token);
    }

    public Token peek() {
        Token token = nextToken();
        pushBack(token);
        return token;
    }

    public boolean popIfMatchesType(TokenType tokenType) {
        Token token = nextToken();
        if (token.getTokenType() == tokenType) {
            return true;
        }
        pushBack(token);
        return false;
    }

    static private class LexerImpl {

        private LexerReader lexerReader;

        LexerImpl(LexerReader lexerReader) {
            this.lexerReader = lexerReader;
        }

        Token nextToken() {
            int c = lexerReader.nextChar();
            out_for:
            for (; ; ) {
                switch (c) {
                    case ' ':
                    case '\n':
                    case '\r':
                    case '\t':
                        c = lexerReader.nextChar();
                        continue;
                    default:
                        break out_for;
                }
            }
            if (c < 0) {
                return new Token(TokenType.END, null);
            }
            switch (c) {
                case '{':
                    return new Token(TokenType.PROPERTY_BEGIN, (char) c);
                case '}':
                    return new Token(TokenType.PROPERTY_END, (char) c);
                case '[':
                    return new Token(TokenType.LIST_BEGIN, (char) c);
                case ']':
                    return new Token(TokenType.LIST_END, (char) c);
                case ',':
                    return new Token(TokenType.COMMA, (char) c);
                case ':':
                    return new Token(TokenType.COLON, (char) c);
                case '@':
                    return new Token(TokenType.AT, (char) c);
                case '.':
                    return handleDot(c);
                case '+':
                    return new Token(TokenType.ARRAY_PREFIX, (char) c);
                case '!':
                    return new Token(TokenType.ENUM_PREFIX, (char) c);
                case '#':
                    return handleMark(c);
                case '\"':
                    return handleString(c);
                case '-':
                    return handleNumber(c);
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    return handleNumber(c);
                default:
                    break;
            }
            if (isAlpha(c) || c == '_' || c == '$') {
                return handleId(c);
            }
            return new Token(TokenType.ERROR, "unknown char: " + (char) c);
        }

        private Token handleId(int c) {
            StringBuilder builder = new StringBuilder();
            builder.append((char) c);
            for (; ; ) {
                c = lexerReader.nextChar();
                if (c < 0) {
                    break;
                }
                if (isAlpha(c) || c == '_' || c == '$' || isDigit(c)) {
                    builder.append((char) c);
                } else {
                    lexerReader.pushBack(c);
                    break;
                }
            }
            String idName = builder.toString();

            TokenType tokenType = KeyWords.getTokenTypeByName(idName);
            if (tokenType != null) {
                return new Token(tokenType, idName);
            } else {
                return new Token(TokenType.ID, idName);
            }
        }

        private static boolean isAlpha(int c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private Token handleString(int c) {
            StringBuilder builder = new StringBuilder();
            for (; ; ) {
                c = lexerReader.nextChar();
                if (c < 0) {
                    break;
                }
                if (c == '\"') {
                    break;
                }
                if (c == '\\') {
                    int nextC = lexerReader.nextChar();
                    if (nextC < 0) {
                        break;
                    }
                    switch (nextC) {
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case '\\':
                            builder.append('\\');
                            break;
                        case '\"':
                            builder.append('\"');
                            break;
                        case '\'':
                            builder.append('\'');
                            break;
                        default:
                            // ignore '\'
                            builder.append((char) nextC);
                    }
                } else {
                    builder.append((char) c);
                }
            }
            return new Token(TokenType.VALUE_STRING, builder.toString());
        }

        private static boolean isDigit(int c) {
            return c >= '0' && c <= '9';
        }

        private Token handleDot(int c) {
            int nextC = lexerReader.peek();
            if (isDigit(nextC)) {
                return handleNumber(c);
            } else {
                return new Token(TokenType.DOT, (char) c);
            }
        }

        private Token handleNumber(int c) {
            boolean hasFloat = false;
            boolean dealIntPart = true;
            double floatBase = 0.1;
            long intPart = 0;
            double floatPart = 0;
            long minus = 1;
            boolean hasPowerPart = false;
            Long powerPart = null;
            boolean powerPartMinus = false;
            if (c == '-') {
                minus = -1;
                int nextC = lexerReader.peek();
                if (!isDigit(nextC) && nextC != '.') {
                    return new Token(TokenType.ERROR, "need a number or a dot");
                }
                c = lexerReader.nextChar();
            }
            if (c == '.') {
                hasFloat = true;
                dealIntPart = false;
                int nextC = lexerReader.peek();
                if (!isDigit(nextC)) {
                    return new Token(TokenType.ERROR, "need a number after dot");
                }
                c = lexerReader.nextChar();
            }


            for (; ; ) {
                if (dealIntPart) {
                    intPart *= 10;
                    intPart += c - '0';
                } else if (hasPowerPart) {
                    if (powerPart == null) {
                        powerPart = (long)(c - '0');
                    } else {
                        powerPart *= 10;
                        powerPart += c - '0';
                    }
                } else {
                    floatPart += floatBase * (c - '0');
                    floatBase /= 10;
                }
                c = lexerReader.peek();
                if (c == '.') {
                    if (hasFloat) {
                        return new Token(TokenType.VALUE_FLOAT, minus * (intPart + floatPart));
                    } else {
                        hasFloat = true;
                        dealIntPart = false;
                        lexerReader.nextChar(); // pop '.'
                        c = lexerReader.nextChar();
                    }
                } else if (c == 'E' || c == 'e') {
                    if (hasPowerPart) {
                        return new Token(TokenType.ERROR, "Duplicate E part for number");
                    } else {
                        lexerReader.nextChar(); // pop '.'
                        c = lexerReader.nextChar();
                        if (c == '-') {
                            powerPartMinus = true;
                            c = lexerReader.nextChar();
                        }
                        hasPowerPart = true;
                    }
                } else if (!isDigit(c)) {
                    if (powerPart != null) {
                        double real = minus * (intPart + floatPart);
                        if (powerPartMinus) {
                            for (int i = 0; i < powerPart; i++) {
                                real /= 10;
                            }
                        } else {
                            for (int i = 0; i < powerPart; i++) {
                                real *= 10;
                            }
                        }
                        return new Token(TokenType.VALUE_FLOAT, real);
                    }
                    if (hasFloat) {
                        return new Token(TokenType.VALUE_FLOAT, minus * (intPart + floatPart));
                    } else {
                        return new Token(TokenType.VALUE_INT, minus * intPart);
                    }
                } else {
                    lexerReader.nextChar();
                }
            }
        }

        private Token handleMark(int c) {
            if ('t' != lexerReader.peek()) {
                return new Token(TokenType.MARK, (char) c);
            }
            int c1 = lexerReader.nextChar();    // pop t

            if ('y' != lexerReader.peek()) {
                lexerReader.pushBack(c1);
                return new Token(TokenType.MARK, (char) c);
            }
            int c2 = lexerReader.nextChar();    // pop y

            if ('p' != lexerReader.peek()) {
                lexerReader.pushBack(c2);
                lexerReader.pushBack(c1);
                return new Token(TokenType.MARK, (char) c);
            }
            int c3 = lexerReader.nextChar();    // pop p

            if ('e' != lexerReader.peek()) {
                lexerReader.pushBack(c3);
                lexerReader.pushBack(c2);
                lexerReader.pushBack(c1);
                return new Token(TokenType.MARK, (char) c);
            }
            int c4 = lexerReader.nextChar();    // pop e

            if ('s' != lexerReader.peek()) {
                lexerReader.pushBack(c4);
                lexerReader.pushBack(c3);
                lexerReader.pushBack(c2);
                lexerReader.pushBack(c1);
                return new Token(TokenType.MARK, (char) c);
            }
            lexerReader.nextChar(); // pop s

            return new Token(TokenType.KW_MARK_TYPES, "#types");
        }
    }

}
