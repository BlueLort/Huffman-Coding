package model.Compression;

import javafx.util.Pair;
import model.CompressionInfo;
import model.FolderCompressionInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;


public class CompressionHandler {
    /**Header DATA -> File
     * -------------------------------------
     * 8 BITS COMPRESSION TYPE
     *   0xfd COMPRESSED 4 BYTES FOR CODES
     *   0xfc COMPRESSED 3 BYTES FOR CODES
     *   0xfb COMPRESSED 2 BYTES FOR CODES
     *   0xfa COMPRESSED 1 BYTE FOR CODES
     *
     *
     * 8 BITS NUMBER OF CHARS IN DICTIONARY [let this be nc]
     *
     * 32 BITS ORIGINAL FILE LENGTH
     *
     * 8 BITS FILE NAME LENGTH
     *
     * FILE NAME
     *
     * RE FOR $(nc) 8BITS <Character> CodeFormat BYTES <Code>
     *
     * DATA COMPRESSED
     *
     */
    /**Header DATA -> Folder
     * -------------------------------------
     * 8 BITS 0xf0 HUFFMAN TREE
     * 8 BITS HUFFMAN CODING FORMAT
     * 8 BITS HUFFMAN SIZE
     *
     * HUFFMAN DATA
     *
     * 8 BITS COMPRESSION TYPE
     *   0x0f COMPRESSED AS FOLDER
     *   0xf0 COMPRESSED AS FILE [FOLDER COMPRESSION]
     *
     *if FILE
     *  32 BITS NUMBER OF BYTES ALLOCATED [ASSUMING MAX FILE IS 2^32 BITS] 0.5GB file
     *end if
     *
     * 32 BITS ORIGINAL FILE LENGTH [BIG ENDIAN]
     *
     * 8 BITS FILE NAME LENGTH
     *
     * FILE NAME
     *
     * DATA COMPRESSED
     *
     */
    public static  CompressedFileInfo getCompressedFile(
            HashMap<Character,String> HuffmanTable
            ,byte[] input
            , String fileName
    ){
        CompressedFileInfo CFI;
        CFI=getFileCompressed(HuffmanTable,input,fileName);
        return CFI;

    }
    private static CompressedFileInfo getFileCompressed(
            HashMap<Character,String> HuffmanTable
            ,byte[] input
            , String fileName
    ) {
        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int compressionFormat = 0x0f0;//0xfa to 0xfd
        headerInfo.add((byte)compressionFormat);
        //4 bytes to store number of bytes added.
        headerInfo.add((byte) (0));
        headerInfo.add((byte) (0));
        headerInfo.add((byte) (0));
        headerInfo.add((byte) (0));

        int len = input.length;
        headerInfo.add((byte) (len >> 24));
        headerInfo.add((byte) (len >> 16));
        headerInfo.add((byte) (len >> 8));
        headerInfo.add((byte) (len));

        headerInfo.add((byte)((fileName.length()-1)&0xff));
        addFileNameData(headerInfo,fileName);

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

        long byteReached=headerInfo.size()+fileData.length;

        headerInfo.set(1,(byte) (byteReached >> 24));
        headerInfo.set(2,(byte) (byteReached >> 16));
        headerInfo.set(3,(byte) (byteReached >> 8));
        headerInfo.set(4,(byte) (byteReached));

        CFI=new CompressedFileInfo(headerInfo,fileData);

        return CFI;
    }

    public static  CompressedFileInfo getCompressedFile(
            CompressionInfo CI
            , byte[] input
            ){
            CompressedFileInfo CFI;
            CFI=getFileCompressed(CI.huffmanCodes,input,CI.fileName,CI.codeFormat);

        return CFI;

    }

    private static CompressedFileInfo getFileCompressed(
            HashMap<Character,String> HuffmanTable
            ,byte[] input
            , String fileName
            ,int codeFormat
    ) {

        ArrayList<Byte> headerInfo=new ArrayList<>();
        CompressedFileInfo CFI;
        int compressionFormat = 0x0f9+codeFormat;//0xfa to 0xfd

        headerInfo.add((byte)compressionFormat);
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

        addDicationary(headerInfo,HuffmanTable,codeFormat);

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

        CFI=new CompressedFileInfo(headerInfo,fileData);
        return CFI;
    }
    private static void addDicationary(ArrayList<Byte> out,HashMap<Character,String>  HuffmanTable,int codeFormat){
        for (Map.Entry<Character,String> ite : HuffmanTable.entrySet()) {
            out.add((byte)(ite.getKey()&0xff));
            long val;
            val=Long.parseLong(ite.getValue(),2);
            val |=(2<<(ite.getValue().length()-1));
            for(int i=codeFormat-1;i>=0;i--){
                out.add((byte)((val>>(i*8))&0xff));
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

