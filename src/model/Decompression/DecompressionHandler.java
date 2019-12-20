package model.Decompression;

import javafx.concurrent.Task;
import model.Compression.CompressedFileInfo;
import model.Compression.CompressedFolderInfo;
import model.Compression.CompressionHandler;
import model.FileManager;
import model.FrequencyChecker;
import model.HuffmanCompressor;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class DecompressionHandler {
    public static HashMap<Character,String> debug;
    public static DecompressedFolderInfo DecompressFolder(byte[] fileData,int start){
        DecompressedFolderInfo DFOLDI=new DecompressedFolderInfo();
        int nFiles=(((int)fileData[1+start]<<8&0xff00)|(fileData[2+start]&0xff));
        DFOLDI.folderName=extractFolderName(fileData,start);
        int offset=4+DFOLDI.folderName.length()+start;
        ArrayList<Thread> tArr=new ArrayList<>();

        for(int i=0;i<nFiles;i++){
            byte nextFile=fileData[offset];
            if(nextFile!=0x0f){
                int nBytes=(int)((long)((fileData[1+offset]&0xff)<<24) | (long)((fileData[2+offset]&0xff)<<16) | (long)((fileData[3+offset]&0xff)<<8) | (long)(fileData[4+offset]&0xff));
                byte[] data=new byte[nBytes];
                for(int j=0;j<nBytes;j++){
                    data[j]=fileData[j+offset];
                }
                offset+=nBytes;
                Task task = new Task<Void>() {
                    @Override public Void call() {
                        DecompressedFileInfo DFI=DecompressFile(data,BitSet.valueOf(data),false);
                        DFOLDI.DFIs.add(DFI);
                        return null;
                    }
                };
                Thread t= new Thread(task);
                t.start();
                tArr.add(t);
            }else{
                DFOLDI.DFOLDIs.add(DecompressFolder(fileData,offset));
                offset=DFOLDI.DFOLDIs.get(DFOLDI.DFOLDIs.size()-1).maxReach;
            }
        }

        for(Thread t:tArr){
            try{
                t.join();
            }catch (Exception e){
                System.out.println(e.getStackTrace());
            }

        }
        DFOLDI.maxReach=offset;
        return DFOLDI;
    }
    private static int byteReached=0;
    public static DecompressedFileInfo DecompressFile(byte[] fileData,BitSet bs,boolean isFile){
        //Note &<hexNumber> is to cast byte values avoiding the negative values.
        int isCompressed=fileData[0]&0xff;
        byteReached=0;
        String data="";//The Output String
        String fileName="";
        int start=0;
        if(isFile==false)start=4;
        if(isCompressed!=0){
            boolean doubleByte=(isCompressed==0xff);
            short nChars=(short)((fileData[1+start]&0xff)+1);
            long fileLength=((long)((fileData[2+start]&0xff)<<24) | (long)((fileData[3+start]&0xff)<<16) | (long)((fileData[4+start]&0xff)<<8) | (long)(fileData[5+start]&0xff));
            short fileNameLen=(short)((fileData[6+start]&0xff)+1);
            fileName=getFileName(fileData,fileNameLen,start);

            HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,fileNameLen,bs,nChars,doubleByte,start);
       /*     for(Map.Entry e:huffmanTable.entrySet()){
                System.out.printf("%s %s %s\n",e.getValue(),e.getKey(),debug.get(e.getValue()));
            }
            */
            System.out.println(huffmanTable);
            System.out.printf("Number of Leaves:%d\n",huffmanTable.size());

            int startLoc=((doubleByte?(nChars*3)+7+fileNameLen:(nChars*2)+7+fileNameLen)+start)*8;
            int endLoc=fileData.length*8;//i can just read until i find file chars
            data=getDecompressedData(bs,huffmanTable,doubleByte,nChars,fileLength,startLoc,endLoc);
        }else{
            int len=(fileData[1+start]&0xff)+1;
            for(int i=2+start,offset=0;offset<len;offset++){
                fileName+=(char)(fileData[i+offset]&0xff);
            }
            for(int i=3+len+start;i<fileData.length;i++){
                data+=(char)(fileData[i]);
            }
            byteReached=4+len+fileData.length+start;
        }
        System.out.println(fileName);
      //  System.out.println(data);
        return new DecompressedFileInfo(fileName,data,byteReached);
    }
    private static String getFileName(byte[] fileData,int fileNameLen,int start){
        String res="";
        for(int i=0;i<fileNameLen;i++){
            res+=(char)(fileData[7+i+start]&0xff);
        }
        return res;
    }
    private static String extractFolderName(byte[] fileData,int idx){
        String res="";
        int len=(fileData[3+idx]&0xff);
        for(int i=4+idx,offset=0;offset<len;offset++){
            res+=(char)(fileData[i+offset]&0xff);
        }
        return res;
    }
    private static HashMap<String,Character> GetDecompressionTable(byte[] fileData,int fileNameLen,BitSet fileDataBits,int nChars,boolean doubleByte,int start){
        HashMap<String,Character> huffmanTable=new HashMap<>();
        //make the end of data depends if using Double or Single Byte data for the huffmanTable
        int endLoc=(doubleByte?(nChars*3):(nChars*2))+6+start+fileNameLen;
        int step=doubleByte?3:2;

        for(int i=7+fileNameLen+start;i<endLoc;i+=step){
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
    private static String getDecompressedData(BitSet bs, HashMap<String,Character>  huffmanTable,boolean doubleByte,short nChars,long fileLength,int startLoc,int endLoc){
        String res="";
        long charCounter=0;
        String codeBuffer="";
        for(int i=startLoc;i<endLoc;i++){
            codeBuffer+=bs.get(i)?"1":"0";
            if (huffmanTable.containsKey(codeBuffer)) {
                res += huffmanTable.get(codeBuffer);
                codeBuffer="";
                charCounter++;
                if (charCounter == fileLength){
                    byteReached=(int)((i/8.0)+0.99999999);
                    break;
                }
            }
        }
    return res;
    }
}
