package model.Compression;

import java.io.File;
import java.io.FileOutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


public class CompressionHandler {
    /**Header DATA & FILE DATA INFORMATION
     * -------------------------------------
     * 8 BITS COMPRESSION TYPE
     *   0xff COMPRESSED DOUBLE BYTE FOR CODES
     *   0x80 COMPRESSED SIGNLE BYTE FOR CODES
     *   0x00 NOT COMPRESSED
     *
     * <IF COMPRESSED FOR FOLDER>
     * 32 BITS NUMBER OF BYTES ALLOCATED [ASSUMING MAX FILE IS 2^32 BITS] 0.5GB file
     *   * </IF>
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
    public static  CompressedFileInfo getCompressedFile(
            HashMap<Character,String> HuffmanTable
            , byte[] input
            , String fileName
            , boolean isFile
            ){
            CompressedFileInfo CFI;
            boolean doubleBytes=NeedMoreBytes(HuffmanTable);
            boolean isCompressable=isCompressable(input.length,fileName.length(),HuffmanTable,doubleBytes);
            if(isCompressable==false){
                CFI=getFileNotCompressed(input,fileName,isFile);
            }else {
                CFI=getFileCompressed(HuffmanTable,input,fileName,doubleBytes,isFile);
            }

        return CFI;

    }
    private static CompressedFileInfo getFileNotCompressed(
             byte[] input
            , String fileName
             ,boolean isFile

    ) {
        //Rejction for files is to check CFI.isCompressed
        //Assuming that i will never reject folder compression request
        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int isCompressed = 0x00;
        headerInfo.add((byte) isCompressed);
        if(isFile==false){
            //2bytes compressed , filename len , 4bytes nBytes
            int len=6+fileName.length()+input.length;
            headerInfo.add((byte)(len>>24));
            headerInfo.add((byte)(len>>16));
            headerInfo.add((byte)(len>>8));
            headerInfo.add((byte)(len));
        }

        headerInfo.add((byte)((fileName.length()-1)&0xff));
        addFileNameData(headerInfo,fileName);
        CFI=new CompressedFileInfo();
        CFI.headerData=headerInfo;
        if(isFile==false) {//can't reject folder requests
            CFI.fileData = new byte[input.length];
            for (int i = 0; i < input.length; i++) {
                CFI.fileData[i] = input[i];
            }
        }
        return CFI;
    }
    private static CompressedFileInfo getFileCompressed(
            HashMap<Character,String> HuffmanTable
            ,byte[] input
            , String fileName
            ,boolean doubleBytes
            ,boolean isFile
    ) {

        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int isCompressed = 128;
        if(doubleBytes)isCompressed=0xff;
        headerInfo.add((byte) isCompressed);
        if(isFile==false){
            //placeholders to put number of bytes later
            headerInfo.add((byte)0);
            headerInfo.add((byte)0);
            headerInfo.add((byte)0);
            headerInfo.add((byte)0);
        }


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
        int bitIndex = 0;
        bitIndex = addFileData(bs,bitIndex,input,HuffmanTable);
        /**
         * say f has code 000
         * and end of text had ....fffff
         * so i wont set any bit in bitset so that would lead to less number of bytes given by the BitSet
         * set last bitindex i reached to ensure that won't happen.
         */
        bs.set(bitIndex);

        byte[] fileData=bs.toByteArray();
        if(isFile==false){

            int nBytes=headerInfo.size()+fileData.length;
            headerInfo.set(1,(byte)(nBytes>>24));
            headerInfo.set(2,(byte)(nBytes>>16));
            headerInfo.set(3,(byte)(nBytes>>8));
            headerInfo.set(4,(byte)(nBytes));
        }


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
        return max>=8;
    }
    private static boolean isCompressable(int inputLen,int fileNameLen,HashMap<Character,String>  HuffmanTable,boolean doubleBytes){
        //TODO IMPROVE THIS
        return inputLen-(7+(HuffmanTable.size()*(doubleBytes?3:2))+fileNameLen) >0;
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
    private static int addFileData(BitSet bs,int bitIndex,byte[] input,HashMap<Character,String> HuffmanTable){
        for (int i = 0; i < input.length; i++) {
            String binaryValue=HuffmanTable.get((char)(input[i]&0xff));
            for(int j=0;j<binaryValue.length();j++){
                if(binaryValue.charAt(j)=='1')
                    bs.set(bitIndex);
                bitIndex++;
            }

        }
        return bitIndex;
    }

}

