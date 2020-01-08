package com.github.pister.tson.parse;

import com.github.pister.tson.TsonObject;
import com.github.pister.tson.Tsons;
import com.github.pister.tson.models.Item;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.io.StringReader;

/**
 * Created by songlihuang on 2020/1/7.
 */
public class ParserTest extends TestCase {

    public void testParse() {
        String s1 = "#types{0:com.github.pister.tson.objects.Person,1:com.github.pister.tson.objects.Contact}\n" +
                "#0@{address:[str@\"xx\",str@\"yy\"],mobiles:+str@[str@\"123\",str@\"555\"],name:str@\"Jack\",birth:date@\"2020-01-07 17:45:16.966\",weight:i64@0,attr2:+i32@[i32@3,i32@4],attr1:+i32@[i32@1,i32@2],married:bool@true,age:i32@42,contacts:[#1@{name:str@\"Peter\",mobile:str@\"133\"},#1@{name:str@\"Tom\",mobile:str@\"134\"}],attrs:{name2:str@\"xxx\",name1:i32@123}}";
        Lexer lexer = new Lexer(new LexerReader(new StringReader(s1)));
        Parser parser = new Parser(lexer);
        Item item = parser.parse();
        TsonObject tsonObject = new TsonObject(item);
        String s2 = Tsons.toTsonString(tsonObject);
        System.out.println(s1);
        System.out.println(s2);
        Assert.assertEquals(s1, s2);
    }

}