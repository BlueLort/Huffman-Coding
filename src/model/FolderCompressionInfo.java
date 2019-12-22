package model;

import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FolderCompressionInfo {
    public ArrayList<String> fileNames;
    public ArrayList<String> folderNames;
    public ArrayList<Pair<Character,Long>> frequencyArray;
    public HashMap<Character,String> huffmanCodes;
    public int codeFormat;
    public boolean isCompressible;

   public FolderCompressionInfo(CompressionInfo CI){
        this.frequencyArray=CI.frequencyArray;
        this.huffmanCodes=CI.huffmanCodes;
        folderNames=new ArrayList<>();
        fileNames=new ArrayList<>();
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
        long lengthCharsFiles=0;//chars of files + 1 for length storing
        for(String s:fileNames){
            lengthCharsFiles+=(s.length()+1);
        }
        long lengthCharsFolders=0;//chars of files + 1 for length storing
        for(String s:folderNames){
            lengthCharsFolders+=(s.length()+1);
        }
        long headerSize=(3*folderNames.size())+lengthCharsFolders+(9*fileNames.size())+lengthCharsFiles+codeFormat*huffmanCodes.size();

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
