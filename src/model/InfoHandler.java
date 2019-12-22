package model;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfoHandler {
    public static synchronized void DisplayInfo(CompressionInfo CI){
        System.out.print("=========================================\nFile Name: ");
        System.out.println(CI.fileName);
        System.out.println("=========================================");
        System.out.printf("Char\tByte\t  Code\t\t\tNew Code\n");
        for(Map.Entry<Character,String> e:CI.huffmanCodes.entrySet()){
            System.out.printf("%s\t\t%d\t\t%s\t\t   %s\n",e.getKey(),(int)e.getKey(),getBinary(e.getKey()),e.getValue());
        }
        System.out.printf("Number of leaves:%d\n",CI.huffmanCodes.size());
        System.out.println("========================================");
    }
    public static String getBinary(int x){
        String res=Integer.toBinaryString(x);
        String padding="";
        int diff=8-res.length();
        for(int i=0;i<diff;i++)padding+="0";
        return padding+res;
    }
}
