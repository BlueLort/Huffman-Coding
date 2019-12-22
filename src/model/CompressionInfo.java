package model;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CompressionInfo {
    public String fileName;
    public ArrayList<Pair<Character,Long>> frequencyArray;
    public HashMap<Character,String> huffmanCodes;
    public int codeFormat;
    public boolean isCompressible;

    CompressionInfo(ArrayList<Pair<Character,Long>> frequencyArray,HashMap<Character,String>  huffmanCodes,String fileName){
        this.frequencyArray=frequencyArray;
        this.huffmanCodes=huffmanCodes;
        this.fileName=fileName;
    }
    public void compressionTest(){
        long freqSum=0;
        for(Pair<Character,Long> p:frequencyArray){
            freqSum+=p.getValue();
        }
        long counter=0;
        for(Pair<Character,Long> p:frequencyArray){
            counter+= huffmanCodes.get(p.getKey()).length() * p.getValue();
        }
        counter=(long)(((double)counter/8.0)+0.999999);

        codeFormat=getCodeFormat(huffmanCodes);
        long headerSize=7+fileName.length()+(codeFormat+1)*huffmanCodes.size();

        isCompressible=(headerSize+counter)<freqSum;
    }
    private static int getCodeFormat(HashMap<Character,String> huffmanTable){
        int max=-1;
        int min=0x7fffffff;

        for(Map.Entry ite:huffmanTable.entrySet()){
            if(max<ite.getValue().toString().length()){
                max=ite.getValue().toString().length();
            }
            if(min>ite.getValue().toString().length()){
                min=ite.getValue().toString().length();
            }
        }
        System.out.printf("nBits min:%d , max:%d\n",min,max);

        //supporting up to 4 bytes for codes
        return (max/8)+1;
    }
}
