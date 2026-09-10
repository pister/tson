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
                    if (lexerReader.peek() == 'I') {
                        return handleNegativeInfinity();
                    }
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
                    // 走到输入末尾还没遇到闭合引号，说明文本被截断或者本身就是坏的。
                    // 这里原来是 break，会把读到一半的内容当成完整字符串返回，
                    // 于是损坏的数据看起来和正常数据一样 —— 持久化场景宁可报错。
                    throw new SyntaxException("unterminated string, read " + builder.length() + " chars before end of input");
                }
                if (c == '\"') {
                    break;
                }
                if (c == '\\') {
                    int nextC = lexerReader.nextChar();
                    if (nextC < 0) {
                        throw new SyntaxException("unterminated string, input ends with a dangling escape after " + builder.length() + " chars");
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

        /**
         * 只负责把数字的原始字符收集完整，转换交给 Long.parseLong / Double.parseDouble。
         *
         * <p>这里以前自己累加算值，出过两类静默错值：指数前没有小数点时，
         * E 分支没关掉整数部分状态，指数数字被拼进尾数（1e3 算成 13）；
         * 小数和指数靠浮点累加/累乘，误差逐步放大（随机 double 往返约 86% 失真）。
         * parseLong / parseDouble 的语义是精确定义的标准行为，没有这两类问题，
         * 超出 long 范围也会明确报错而不是回绕成别的数。</p>
         */
        private Token handleNumber(int c) {
            // 纯整数快速路径：直接边消费边累积，避免 StringBuilder + String +
            // parseLong 的三重分配。数字密集的数据里绝大多数 token 走这条路。
            // 一旦发现是浮点（后面跟 '.'/'e'/'E'）或溢出，把已消费的数字
            // 逆序吐回，原样交给通用路径。
            if (isDigit(c) || (c == '-' && isDigit(lexerReader.peek()))) {
                long acc = isDigit(c) ? c - '0' : 0;
                char[] consumed = new char[20]; // long 最多 19 位，溢出守护保证不会越界
                int n = 0;
                boolean overflow = false;
                int p;
                for (; ; ) {
                    p = lexerReader.peek();
                    if (!isDigit(p)) {
                        break;
                    }
                    int d = p - '0';
                    // 溢出守护只看数值：acc 被前导零压住不增长，超长补零数字
                    // （如定宽补零的 ID）永远不会触发它，第 21 个数字就会写穿
                    // consumed[20]。位数到达 long 上限（19 位）同样交给通用路径。
                    if (acc > (Long.MAX_VALUE - d) / 10 || n >= 19) {
                        overflow = true; // 含 Long.MIN 的精确边界，交给通用路径精确处理
                        break;
                    }
                    lexerReader.nextChar();
                    consumed[n++] = (char) p;
                    acc = acc * 10 + d;
                }
                if (!overflow && p != '.' && p != 'e' && p != 'E') {
                    return new Token(TokenType.VALUE_INT, c == '-' ? -acc : acc);
                }
                for (int i = n - 1; i >= 0; i--) {
                    lexerReader.pushBack(consumed[i]);
                }
            }
            StringBuilder builder = new StringBuilder();
            builder.append((char) c);
            // 正负号只允许出现在 e/E 的紧后面
            boolean expectSign = false;
            for (; ; ) {
                int p = lexerReader.peek();
                if (isDigit(p) || p == '.') {
                    builder.append((char) lexerReader.nextChar());
                    expectSign = false;
                } else if (p == 'e' || p == 'E') {
                    builder.append((char) lexerReader.nextChar());
                    expectSign = true;
                } else if (expectSign && (p == '+' || p == '-')) {
                    builder.append((char) lexerReader.nextChar());
                    expectSign = false;
                } else {
                    break;
                }
            }
            String s = builder.toString();
            boolean isFloat = s.indexOf('.') >= 0 || s.indexOf('e') >= 0 || s.indexOf('E') >= 0;
            try {
                if (isFloat) {
                    return new Token(TokenType.VALUE_FLOAT, Double.parseDouble(s));
                }
                return new Token(TokenType.VALUE_INT, Long.parseLong(s));
            } catch (NumberFormatException e) {
                return new Token(TokenType.ERROR, "illegal number: " + s);
            }
        }

        /**
         * 编码端写出 Double.toString(Double.NEGATIVE_INFINITY) 就是 "-Infinity"，
         * 词法层必须认它，否则编码成功、解码抛异常。
         * 调用到这里时 '-' 已被消费，只需读出后面的 "Infinity"。
         */
        private Token handleNegativeInfinity() {
            String rest = "Infinity";
            for (int i = 0; i < rest.length(); i++) {
                int n = lexerReader.nextChar();
                if (n != rest.charAt(i)) {
                    return new Token(TokenType.ERROR, "illegal number: -" + rest.substring(0, i) + (char) n);
                }
            }
            return new Token(TokenType.VALUE_FLOAT, Double.NEGATIVE_INFINITY);
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
