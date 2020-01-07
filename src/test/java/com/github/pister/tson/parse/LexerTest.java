package com.github.pister.tson.parse;

import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class LexerTest extends TestCase {

    public void testParseToken() {
        String s = "#types{12.23:com.github.pister.tson.objects.Person,1:com.github.pister.tson.objects.Contact}\n" +
                "#0@{address:[str@\"xx\",str@\"yy\"],mobiles:+str@[str@\"123\",str@\"555\"],name:str@\"Jack\",birth:date@\"2020-01-07 17:45:16.966\",weight:i64@0,attr2:+i32@[i32@3,i32@4],attr1:+i32@[i32@1,i32@2],married:bool@true,age:i32@42,contacts:[#1@{name:str@\"Peter\",mobile:str@\"133\"},#1@{name:str@\"Tom\",mobile:str@\"134\"}],attrs:{name2:str@\"xxx\",name1:i32@123}}\n";
        Lexer lexer = new Lexer(new LexerReader(new StringReader(s)));
        for (;;) {
            Token token = lexer.nextToken();
            if (token.getTokenType() == TokenType.END) {
                break;
            }
            if (token.getTokenType() == TokenType.ERROR) {
                System.out.println("error:" + token.getValue());
                break;
            }
            System.out.println("<" + token.getTokenType() +"> " + token.getValue());
        }
    }

}