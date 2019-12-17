package model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class FileManager{
    //nothing special here just normal ascii file reading
        public static String ReadFile(String filePath){
            File file = new File(filePath);
            try {

                StringBuilder stringBuffer = new StringBuilder();
                Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                char[] buff = new char[512];
                for (int charsRead; (charsRead = reader.read(buff)) != -1; ) {
                    stringBuffer.append(buff, 0, charsRead);
                }
                return stringBuffer.toString();
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
        public static byte[] ReadBinaryFile(String filePath){
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
            //using byte arrays to make it easier to convert to other datatypes
            byte[] arr=new byte[output.size()];
            for(int i=0;i<output.size();i++){
                arr[i]=output.get(i);
            }
            return arr;
        }

        //Decompression algorithm goes here
        public static void DecompressFile(byte[] fileData,String destination){
                //it might be 0xff or 0x80 depends if its using 2Bytes for table content or 1Byte
                //Otherwise its not compressed
                int isCompressed=fileData[0];
                String result="";//The Output String
                if(isCompressed==(byte)0xff||isCompressed==(byte)0x80){
                    boolean doubleByte=isCompressed==0xff;
                    byte nChars=fileData[1];
                    int fileLength=(fileData[2]<<24 | fileData[3]<<16 | fileData[4]<<8 | fileData[5]);
                    BitSet bs=BitSet.valueOf(fileData);
                    HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,bs,nChars,doubleByte);
                    int charCounter=0;
                    //make the start of data depends if using Double or Single Byte data for the huffmanTable
                    int start=doubleByte?(nChars*3)+5+1:(nChars*2)+5+1;
                    start*=8;
                    int end=fileData.length*8;
                    String codeBuffer="";

                    for(int i=start;i<end;i++){
                        codeBuffer+=bs.get(i)?"1":"0";
                        if (huffmanTable.containsKey(codeBuffer)) {
                            result += huffmanTable.get(codeBuffer);
                            codeBuffer="";
                            charCounter++;
                        }
                        if (charCounter == fileLength) break;
                    }
                }else{
                    for(int i=1;i<fileData.length;i++){
                        result+=(char)((byte)fileData[i]);
                    }
                }
                System.out.println(result);
            WriteFile(result,destination);
        }
        private static HashMap<String,Character> GetDecompressionTable(byte[] fileData,BitSet fileDataBits,int nChars,boolean doubleByte){
            HashMap<String,Character> huffmanTable=new HashMap<>();
            //make the end of data depends if using Double or Single Byte data for the huffmanTable
            int endLoc=doubleByte?(nChars*3)+5:(nChars*2)+5;
            int step=doubleByte?3:2;

            for(int i=6;i<endLoc;i+=step){
                if(doubleByte){
                    String code=getCode(fileDataBits,(i+1)*8,16);
                    huffmanTable.put(code,Character.toChars(fileData[i])[0]);
                }else{
                    String code=getCode(fileDataBits,(i+1)*8,8);
                    huffmanTable.put(code,Character.toChars(fileData[i])[0]);
                }

            }

            return huffmanTable;
        }
        private static String getCode(BitSet bs,int start,int range) {
            String code = "";
            int startLoc = -1;
                 for (int j = start+range-1, offset = 0; offset < range;offset++) {
                    if (bs.get(j - offset)) {
                        startLoc = j-offset-1;
                        range-=(offset+1);
                        break;
                    }
                }
                for (int j = startLoc, offset = 0; offset < range;offset++) {
                    if (bs.get(j - offset)) {
                        code += "1";
                    } else {
                        code += "0";
                    }
                }
                return code;
        }
        public static void WriteCompressedFile(HashMap<Character,String> HuffmanTable,String input,String path){
            File file=new File(path);
            try {
                //HEADER DATA             8BITS                8BITS             32BITS
                ///contain is  (compressed&dictionarySize) + numberOfChars + File Length
                //THEN 8BITS CHAR 8BITS/16BITS BINARY_VALUE -> Depends if single or double byte data
                //if compressed left most bit is 1
                //if not compressed left most bit is 0
                FileOutputStream fout=new FileOutputStream(file);
                ArrayList<Byte> output=new ArrayList<>();
                boolean doubleBytes=false;
                if(NeedMoreBytes(HuffmanTable)){
                    doubleBytes=true;
                }

                int lenDiff = input.length()-(1+1+4+HuffmanTable.size()*(doubleBytes?3:2));
                if(lenDiff<0){
                    int isCompressed = 0x00;
                    output.add((byte) isCompressed);
                    for (Byte b:input.getBytes()){
                        output.add(b);
                    }
                }else {
                    int isCompressed = 128;
                    if(doubleBytes)isCompressed=0xff;//0xff only if it use 2 bytes for tables.
                    output.add((byte) isCompressed);
                    int numChars = HuffmanTable.size();
                    output.add((byte) numChars);
                    int len = input.length();
                    output.add((byte) (len >> 24));
                    output.add((byte) (len >> 16));
                    output.add((byte) (len >> 8));
                    output.add((byte) (len));


                    for (Map.Entry<Character,String> ite : HuffmanTable.entrySet()) {
                        //  8BITS CHARACTER          8/16BITS VALUE
                        output.add(ite.getKey().toString().getBytes()[0]);
                        if(doubleBytes){
                            short val=(short)(Short.parseShort(ite.getValue(),2)>>8);
                            val |=(2<<ite.getValue().length()-1);
                            output.add((byte)val);
                            output.add((byte)Short.parseShort(ite.getValue(),2));
                        }else{
                            short val=Short.parseShort(ite.getValue(),2);
                            val |=(2<<ite.getValue().length()-1);
                            output.add((byte)val);
                        }

                    }

                    BitSet bs=new BitSet();
                    int bitIndex = 0;
                    for (int i = 0; i < len; i++) {
                        String binaryValue=HuffmanTable.get(input.charAt(i));
                        for(int j=binaryValue.length()-1;j>=0;j--){
                            if(binaryValue.charAt(j)=='1')
                                bs.set(bitIndex);
                            bitIndex++;
                        }

                    }
                    byte[] bytes=bs.toByteArray();
                    for(byte b:bytes){
                        output.add(b);
                    }
                }
                    for (Byte b : output) {
                        fout.write(b);
                    }



            }catch (Exception e){
                e.printStackTrace();
            }
        }
         private static boolean NeedMoreBytes(HashMap<Character,String> HuffmanTable){
             int max=-1;

             for(Map.Entry ite:HuffmanTable.entrySet()){
                 if(max<ite.getValue().toString().length()){
                     max=ite.getValue().toString().length();
                 }
             }

             return max>8;
         }


}

