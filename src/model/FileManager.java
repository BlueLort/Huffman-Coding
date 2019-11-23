package model;

import java.io.*;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class FileManager{
    //nothing special here just normal ascii file reading
        public static String ReadFile(String filePath){
            File file = new File(filePath);
            try {

                BufferedReader br = new BufferedReader(new java.io.FileReader(file));
                String st="";
                String line;
                while((line=br.readLine())!=null){
                    st+=line;
                }
                br.close();
                return st;
            }catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }
        //same as reading i just print text to file.
         public static void WriteFile(String out,String filePath){
        // like \test as \t (ie. as a escape sequence)
        File file = new File(filePath);
        try {
            BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(file));
            bw.write(out);
            bw.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
        }
        //Reads file in binary and store it in Byte array
        public static ArrayList<Byte> ReadBinaryFile(String filePath){
            ArrayList<Byte> output=new ArrayList<>();
            File file = new File(filePath);
            try {

                FileInputStream fin=new FileInputStream(file);
                int data;
                while((data=fin.read())!=-1){
                    output.add((byte)data);
                }
                fin.close();
            }catch (Exception e) {
                e.printStackTrace();

            }
            return output;
        }

        //Decompression algorithm goes here
        public static void DecompressFile(ArrayList<Byte> fileData,String destination){
                //it might be 0xff or 0x80 depends if its using 2Bytes for table content or 1Byte
                //Otherwise its not compressed
                int isCompressed=fileData.get(0);
                String result="";//The Output String
                if(isCompressed==(byte)0xff||isCompressed==(byte)0x80){
                    boolean doubleByte=isCompressed==0xff;
                    byte nChars=fileData.get(1);
                    int fileLength=(fileData.get(2)<<24 | fileData.get(3)<<16 | fileData.get(4)<<8 | fileData.get(5));
                    HashMap<Integer,Character> huffmanTable =GetDecompressionTable(fileData,nChars,doubleByte);
                    int charCounter=0;
                    int keyContainer=0;
                    //make the start of data depends if using Double or Single Byte data for the huffmanTable
                    int start=doubleByte?(nChars*3)+5+1:(nChars*2)+5+1;
                    for(int i=start;i<fileData.size();i++){
                        int buffer=ReverseBitsByte(fileData.get(i),8);
                        int bitIndex=0;
                        while(bitIndex<8) {
                            keyContainer =( keyContainer << 1 )|( (buffer&(1<<7))>>7);
                            buffer = buffer << 1;
                            bitIndex++;
                            if (huffmanTable.containsKey(keyContainer)) {
                                result += huffmanTable.get(keyContainer);
                                keyContainer = 0;
                                charCounter++;
                            }
                            if (charCounter == fileLength) break;
                        }
                    }


                }else{
                    for(int i=1;i<fileData.size();i++){
                        result+=(char)((byte)fileData.get(i));
                    }
                }
            WriteFile(result,destination);


        }
        private static HashMap<Integer,Character> GetDecompressionTable(ArrayList<Byte> fileData,int nChars,boolean doubleByte){
            HashMap<Integer,Character> huffmanTable=new HashMap<>();
            //make the end of data depends if using Double or Single Byte data for the huffmanTable
            int endLoc=doubleByte?(nChars*3)+5:(nChars*2)+5;
            int step=doubleByte?3:2;
            for(int i=6;i<endLoc;i+=step){
                if(doubleByte){
                    huffmanTable.put((fileData.get(i+1)<<8)|(fileData.get(i+2)),Character.toChars(fileData.get(i))[0]);
                }else{
                    huffmanTable.put(((int)fileData.get(i+1)),Character.toChars(fileData.get(i))[0]);
                }

            }

            return huffmanTable;
        }
        public static void WriteCompressedFile(HashMap<Character,Integer> HuffmanTable,String input,String path){
            File file=new File(path);
            try {
                //HEADER DATA             8BITS                8BITS             32BITS
                ///contain is  (compressed&dictionarySize) + numberOfChars + File Length
                //THEN 8BITS CHAR 8BITS/16BITS BINARY_VALUE -> Depends if single or double byte data
                //if compressed left most bit is 1
                //if not compressed left most bit is 0
                FileOutputStream fout=new FileOutputStream(file);
                ArrayList<Byte> output=new ArrayList<>();
                int lenDiff = input.length()-(1+1+4+HuffmanTable.size()*3);
                if(lenDiff<0){
                    int isCompressed = 0x00;
                    output.add((byte) isCompressed);
                    for (Byte b:input.getBytes()){
                        output.add(b);
                    }
                }else {
                    int isCompressed = 128;
                    boolean doubleByte=false;
                    if(NeedMoreBytes(HuffmanTable)){
                        isCompressed=0xff;//0xff only if it use 2 bytes for tables.
                    }

                    output.add((byte) isCompressed);
                    int numChars = HuffmanTable.size();
                    output.add((byte) numChars);
                    int len = input.length();
                    output.add((byte) (len >> 24));
                    output.add((byte) (len >> 16));
                    output.add((byte) (len >> 8));
                    output.add((byte) (len));

                    for (Map.Entry<Character, Integer> ite : HuffmanTable.entrySet()) {
                        //  8BITS CHARACTER          8/16BITS VALUE
                        output.add(ite.getKey().toString().getBytes()[0]);
                        if(doubleByte)output.add((byte) (ite.getValue() >> 8));
                        output.add(ite.getValue().byteValue());
                    }

                    int buffer = 0;
                    int bitIndex = 0;
                    for (int i = 0; i < len; i++) {
                        int biVal = HuffmanTable.get(input.charAt(i)).byteValue();
                        int nBits = GetIntNBits(HuffmanTable.get(input.charAt(i)));
                        biVal = ReverseBitsByte((byte) biVal, nBits);
                        while (nBits > 0) {
                            buffer = (buffer) | ((biVal & (1)) << (bitIndex));
                            biVal >>= 1;
                            nBits--;
                            bitIndex++;
                            if (bitIndex == 8) {
                                output.add((byte) buffer);
                                buffer >>= 8;
                                bitIndex = 0;
                            }
                        }
                    }
                    if (bitIndex > 0) {
                        output.add((byte) buffer);
                        buffer = 0;
                        bitIndex = 0;
                    }
                }
                    for (Byte b : output) {
                        fout.write(b);
                    }



            }catch (Exception e){
                e.printStackTrace();
            }
        }
        private static int GetIntNBits(int num){
             int counter=1;
             while(num>>1!=0){
                 counter++;
                 num=num>>1;
             }
             return counter;
        }
         private static byte ReverseBitsByte(byte num,int amount) {
             byte result=0;
             int shiftCounter=0;
             while(shiftCounter<amount){
                 result=(byte)((result<<1)|(num&1));
                 num = (byte)(num >> 1);
                 shiftCounter++;
            }
         return result;
         }
         private static boolean NeedMoreBytes(HashMap<Character,Integer> HuffmanTable){
             int max=-1;

             for(Map.Entry ite:HuffmanTable.entrySet()){
                 if(max<(int)ite.getValue()){
                     max=(int)ite.getValue();
                 }
             }

             int maxBits=GetIntNBits(max);
             return maxBits>8;
         }


}

