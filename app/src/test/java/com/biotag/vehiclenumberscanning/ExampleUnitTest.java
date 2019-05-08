package com.biotag.vehiclenumberscanning;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testSplit(){
        String str = "20171013\\6364348468327 93357.gif";
        String strArray[] = str.split("\\\\");
        for(int i = 0;i < strArray.length;i ++) {
            System.out.println("strArray[" + i + "] = " + strArray[i]);
        }
        String photoUrl = strArray[1].replaceAll(" ","");
        System.out.println("photoUrl = " + photoUrl);

    }
}