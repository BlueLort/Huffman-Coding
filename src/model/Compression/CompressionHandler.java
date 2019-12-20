package model.Compression;

import java.io.File;
import java.io.FileOutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


public class CompressionHandler {

    public static  CompressedFileInfo getCompressedFile(
            HashMap<Character,String> HuffmanTable
            , byte[] input
            , String fileName
            ){
            CompressedFileInfo CFI;
            boolean doubleBytes=NeedMoreBytes(HuffmanTable);
            boolean isCompressable=isCompressable(input,HuffmanTable,doubleBytes);
            if(isCompressable==false){
                CFI=getFileNotCompressed(input,fileName);
            }else {
                CFI=getFileCompressed(HuffmanTable,input,fileName,doubleBytes);
            }

        return CFI;

    }
    private static CompressedFileInfo getFileNotCompressed(
             byte[] input
            , String fileName
    ) {
        //Rejction for files is to check CFI.isCompressed
        //Assuming that i will never reject folder compression request
        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int isCompressed = 0x00;
        headerInfo.add((byte) isCompressed);
        //not compressed because header size is larger than file data...
        //so won't need more than 1 byte for length
        headerInfo.add((byte)(input.length));
        headerInfo.add((byte)((fileName.length()-1)&0xff));

        addFileNameData(headerInfo,fileName);
        CFI=new CompressedFileInfo();
        CFI.headerData=headerInfo;
        CFI.fileData=new byte[input.length];
        for(int i=0;i<input.length;i++){
            CFI.fileData[i]=input[i];
        }
        return CFI;
    }
    private static CompressedFileInfo getFileCompressed(
            HashMap<Character,String> HuffmanTable
            ,byte[] input
            , String fileName
            ,boolean doubleBytes
    ) {
        /**Header DATA & FILE DATA INFORMATION
         * -------------------------------------
         * 8 BITS COMPRESSION TYPE
         *   0xff COMPRESSED DOUBLE BYTE FOR CODES
         *   0x80 COMPRESSED SIGNLE BYTE FOR CODES
         *   0x00 NOT COMPRESSED
         *
         * 8 BITS NUMBER OF CHARS IN DICTIONARY [let this be nc]
         *
         * 32 BITS ORIGINAL FILE LENGTH [BIG ENDIAN]
         *
         * RECURRING FOR $(nc) 8BITS <Character> 8/16BITS <Code>
         *
         * 8 BITS FILE NAME LENGTH
         *
         * FILE NAME DATA COMPRESSED
         *
         * DATA COMPRESSED
         *
         */
        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int isCompressed = 128;
        if(doubleBytes)isCompressed=0xff;
        headerInfo.add((byte) isCompressed);
        /**
         * 8bits max is 255 so if i have huffman size of 256[in binary files happen]
         * so i just decrement and increment on decompression
         */
        int numChars = HuffmanTable.size()-1;
        headerInfo.add((byte) numChars);

        int len = input.length;
        headerInfo.add((byte) (len >> 24));
        headerInfo.add((byte) (len >> 16));
        headerInfo.add((byte) (len >> 8));
        headerInfo.add((byte) (len));

        headerInfo.add((byte)((fileName.length()-1)&0xff));
        addFileNameData(headerInfo,fileName);

        addDicationary(headerInfo,HuffmanTable,doubleBytes);



        BitSet bs=new BitSet();
        Integer bitIndex = 0;
        addFileData(bs,bitIndex,input,HuffmanTable);
        /**
         * say f has code 000
         * and end of text had ....fffff
         * so i wont set any bit in bitset so that would lead to less number of bytes given by the BitSet
         * set last bitindex i reached to ensure that won't happen.
         */
        bs.set(bitIndex);
        byte[] fileData=bs.toByteArray();

        CFI=new CompressedFileInfo(headerInfo,fileData);
        return CFI;
    }
    private static boolean NeedMoreBytes(HashMap<Character,String> HuffmanTable){
        int max=-1;
        int min=0x7fffffff;

        for(Map.Entry ite:HuffmanTable.entrySet()){
            if(max<ite.getValue().toString().length()){
                max=ite.getValue().toString().length();
            }
            if(min>ite.getValue().toString().length()){
                min=ite.getValue().toString().length();
            }
        }
        System.out.printf("nBits min:%d , max:%d\n",min,max);
        return max>8;
    }
    private static boolean isCompressable(byte[] input,HashMap<Character,String>  HuffmanTable,boolean doubleBytes){
        //TODO FIX THIS
        return input.length-(1+1+4+HuffmanTable.size()*(doubleBytes?3:2)) >0;
    }

    private static void addDicationary(ArrayList<Byte> out,HashMap<Character,String>  HuffmanTable,boolean doubleBytes){
        for (Map.Entry<Character,String> ite : HuffmanTable.entrySet()) {
            out.add((byte)(ite.getKey()&0xff));
            if(doubleBytes){
                short val=Short.parseShort(ite.getValue(),2);
                val |=(2<<ite.getValue().length()-1);
                out.add((byte)((val&0xff00)>>8));
                out.add((byte)(val&0xff));

            }else{
                short val=Short.parseShort(ite.getValue(),2);
                val |=(2<<ite.getValue().length()-1);
                out.add((byte)val);
            }
        }
    }
    private static void addFileNameData(ArrayList<Byte> out,String fileName){
        int len=fileName.length();
        for(int i=0;i<len;i++){
            out.add((byte)(fileName.charAt(i)&0xff));
        }
    }
    private static void addFileData(BitSet bs,Integer bitIndex,byte[] input,HashMap<Character,String> HuffmanTable){
        for (int i = 0; i < input.length; i++) {
            String binaryValue=HuffmanTable.get((char)(input[i]&0xff));
            for(int j=0;j<binaryValue.length();j++){
                if(binaryValue.charAt(j)=='1')
                    bs.set(bitIndex);
                bitIndex++;
            }

        }
    }

}

