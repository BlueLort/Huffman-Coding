package model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
//TODO USE ONLY BYTE ARRAYS
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
            FileOutputStream fout=new FileOutputStream(file);
            for(int i=0;i<out.length();i++){
                fout.write(out.charAt(i)&0xff);
            }
            fout.close();
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
                //Note &<hexNumber> is to cast byte values avoiding the negative values.
                int isCompressed=fileData[0]&0xff;
                String result="";//The Output String
                if(isCompressed==0xff||isCompressed==0x80){
                    boolean doubleByte=isCompressed==0xff;
                    short nChars=(short)((fileData[1]&0xff)+1);
                    long fileLength=((short)(fileData[2]<<24&0xff000000) | (short)(fileData[3]<<16&0xff0000) | (short)(fileData[4]<<8&0xff00) | (short)(fileData[5]&0xff));
                    BitSet bs=BitSet.valueOf(fileData);
                    HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,bs,nChars,doubleByte);
                    System.out.println(huffmanTable);
                    System.out.print("Number of leaves:");
                    System.out.println(huffmanTable.size());
                    long charCounter=0;
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
                            if (charCounter == fileLength) break;
                        }
                    }
                }else{
                    for(int i=1;i<fileData.length;i++){
                        result+=(char)(fileData[i]);
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
                    String code=getCode(fileDataBits,(i+1)*8,8);
                    if(code.equals("NO")){
                        code=getCode(fileDataBits,(i+2)*8,8);
                    }else{
                        code+=getCodeDirect(fileDataBits,(i+2)*8,8);
                    }
                    huffmanTable.put(code,Character.toChars(fileData[i]&0xff)[0]);
                }else{
                    String code=getCode(fileDataBits,(i+1)*8,8);
                    huffmanTable.put(code,Character.toChars(fileData[i]&0xff)[0]);
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
                if(startLoc==-1)return "NO";
                for (int j = startLoc, offset = 0; offset < range;offset++) {
                     code+=bs.get(j - offset)?"1":"0";
                }
                return code;
        }
    private static String getCodeDirect(BitSet bs,int start,int range) {
        String code = "";
        for (int j = start+range-1, offset = 0; offset < range;offset++) {
            code+=bs.get(j-offset)?"1":"0";
        }
        return code;
    }
        public static void WriteCompressedFile(HashMap<Character,String> HuffmanTable,byte[] input,String path){
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
                int lenDiff = input.length-(1+1+4+HuffmanTable.size()*(doubleBytes?3:2));
                if(lenDiff<0){
                    int isCompressed = 0x00;
                    output.add((byte) isCompressed);
                    for (Byte b:input){
                        output.add(b);
                    }
                }else {
                    int isCompressed = 128;
                    if(doubleBytes)isCompressed=0xff;//0xff only if it use 2 bytes for tables.
                    output.add((byte) isCompressed);
                    int numChars = HuffmanTable.size()-1;
                    output.add((byte) numChars);
                    int len = input.length;
                    output.add((byte) (len >> 24));
                    output.add((byte) (len >> 16));
                    output.add((byte) (len >> 8));
                    output.add((byte) (len));


                    for (Map.Entry<Character,String> ite : HuffmanTable.entrySet()) {
                        //  8BITS CHARACTER          8/16BITS VALUE
                        output.add((byte)(ite.getKey()&0xff));
                        if(doubleBytes){
                            short val=Short.parseShort(ite.getValue(),2);
                            val |=(2<<ite.getValue().length()-1);
                            output.add((byte)((val&0xff00)>>8));
                            output.add((byte)(val&0xff));

                        }else{
                            short val=Short.parseShort(ite.getValue(),2);
                            val |=(2<<ite.getValue().length()-1);
                            output.add((byte)val);
                        }

                    }

                    BitSet bs=new BitSet();
                    int bitIndex = 0;
                    for (int i = 0; i < len; i++) {
                        String binaryValue=HuffmanTable.get((char)(input[i]&0xff));
                        for(int j=0;j<binaryValue.length();j++){
                            if(binaryValue.charAt(j)=='1')
                                bs.set(bitIndex);
                            bitIndex++;
                        }

                    }
                    bs.set(bitIndex);//if end of bytes had only 0s code
                    byte[] bytes=bs.toByteArray();
                    for(byte b:bytes){
                        output.add(b);
                    }
                }
                    for (Byte b : output) {
                        fout.write(b);
                    }

                    fout.close();
            }catch (Exception e){
                e.printStackTrace();
            }
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


}

