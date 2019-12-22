package model.Decompression;

import javafx.concurrent.Task;
import model.Compression.CompressedFileInfo;
import model.Compression.CompressedFolderInfo;
import model.Compression.CompressionHandler;
import model.FileManager;
import model.FrequencyChecker;
import model.HuffmanCompressor;
import model.InfoHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

public class DecompressionHandler {
    public static HashMap<Character,String> debug;
    public static DecompressedFolderInfo DecompressFolder(byte[] fileData,int start,HashMap<String,Character> huffmanTable){
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
                Thread t=instantiateDecompression(data,DFOLDI,huffmanTable);
                tArr.add(t);
            }else{
                DFOLDI.DFOLDIs.add(DecompressFolder(fileData,offset,huffmanTable));
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


    private static Thread instantiateDecompression(byte[] data,DecompressedFolderInfo DFOLDI,HashMap<String,Character> huffmanTable) {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                DecompressedFileInfo DFI = DecompressFile(data, BitSet.valueOf(data),huffmanTable);
                DFOLDI.DFIs.add(DFI);
                return null;
            }
        };
        Thread t = new Thread(task);
        t.start();
        return t;
    }
    public static DecompressedFileInfo DecompressFile(byte[] fileData,BitSet bs,HashMap<String,Character> huffmanTable){
        int compressionFormat=fileData[0]&0xff;
        StringBuilder data;
        String fileName="";

        long fileLength=((long)((fileData[5]&0xff)<<24) | (long)((fileData[6]&0xff)<<16) | (long)((fileData[7]&0xff)<<8) | (long)(fileData[8]&0xff));

        short fileNameLen=(short)((fileData[9]&0xff)+1);
        fileName=getFileName(fileData,fileNameLen,10);

        int startLoc=(10+fileNameLen)*8;
        int endLoc=fileData.length*8;//i can just read until i find file chars

        if(fileLength>0) {
            data = getDecompressedData(bs, huffmanTable, fileLength, startLoc, endLoc);
        }else{
            data=new StringBuilder();
        }
        return new DecompressedFileInfo(fileName,data.toString());
    }
    public static DecompressedFileInfo DecompressFile(byte[] fileData,BitSet bs){
        //Note &<hexNumber> is to cast byte values avoiding the negative values.
        int compressionFormat=fileData[0]&0xff;
        StringBuilder data;
        String fileName="";
        int codeFormat=compressionFormat-0x0f9;//get Code fromat as 1,2,3 or 4 [Starts from 0xaf

        short nChars=(short)((fileData[1]&0xff)+1);

        long fileLength=((long)((fileData[2]&0xff)<<24) | (long)((fileData[3]&0xff)<<16) | (long)((fileData[4]&0xff)<<8) | (long)(fileData[5]&0xff));

        short fileNameLen=(short)((fileData[6]&0xff)+1);
        fileName=getFileName(fileData,fileNameLen,7);

        HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,fileNameLen,bs,nChars,codeFormat);

        /* for debugging -> must uncomment debug equality in HuffmanCompressor
        for(Map.Entry<String,Character> e:huffmanTable.entrySet()){
            System.out.printf("%s %s %s\n",e.getValue(),e.getKey(),debug.get(e.getValue()));
        }
        */
        int startLoc=((nChars*(codeFormat+1))+7+fileNameLen)*8;
        int endLoc=fileData.length*8;//i can just read until i find file chars

        if(fileLength>0) {
            data = getDecompressedData(bs, huffmanTable, fileLength, startLoc, endLoc);
        }else{
            data=new StringBuilder();
        }
        return new DecompressedFileInfo(fileName,data.toString());
    }

    private static String getFileName(byte[] fileData,int fileNameLen,int start){
        String res="";
        for(int i=0;i<fileNameLen;i++){
            res+=(char)(fileData[start+i]&0xff);
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
    public static HashMap<String,Character> GetFolderDecompressionTable(byte[] fileData,BitSet fileDataBits){
        int codeFormat=(fileData[1]&0xff)-0x0f9;
        HashMap<String,Character> huffmanTable=new HashMap<>();
        //make the end of data depends if using Double or Single Byte data for the huffmanTable
        int endLoc=fileData.length;
        int step=codeFormat+1;
        StringBuilder code;
        for(int i=3;i<endLoc;i+=step){
            int idx=1;
            do{
                code=getCode(fileDataBits,(i+idx)*8,8);
                idx++;
            }while(code.toString().equals("NO"));

            while(idx<=codeFormat){
                code.append(getCodeDirect(fileDataBits,(i+idx)*8,8).toString());
                idx++;
            }
            huffmanTable.put(code.toString(),Character.toChars(fileData[i]&0xff)[0]);
        }
        return huffmanTable;
    }
    private static HashMap<String,Character> GetDecompressionTable(byte[] fileData,int fileNameLen,BitSet fileDataBits,int nChars,int codeFormat){
        HashMap<String,Character> huffmanTable=new HashMap<>();
        //make the end of data depends if using Double or Single Byte data for the huffmanTable
        int endLoc=(nChars*(codeFormat+1))+7+fileNameLen;
        int step=codeFormat+1;
        StringBuilder code;
        for(int i=7+fileNameLen;i<endLoc;i+=step){
            int idx=1;
            do{
                code=getCode(fileDataBits,(i+idx)*8,8);
                idx++;
            }while(code.toString().equals("NO"));
            while(idx<=codeFormat){
                code.append(getCodeDirect(fileDataBits,(i+idx)*8,8).toString());
                idx++;
            }
            huffmanTable.put(code.toString(),Character.toChars(fileData[i]&0xff)[0]);
        }
        return huffmanTable;
    }
    private static StringBuilder getCode(BitSet bs,int start,int range) {
        StringBuilder code = new StringBuilder();
        int startLoc = -1;
        for (int j = start+range-1, offset = 0; offset < range;offset++) {
            if (bs.get(j - offset)) {
                startLoc = j-offset-1;
                range-=(offset+1);
                break;
            }
        }
        if(startLoc==-1)return new StringBuilder("NO");
        for (int j = startLoc, offset = 0; offset < range;offset++) {
            code.append(bs.get(j - offset)?"1":"0");
        }
        return code;
    }
    private static StringBuilder getCodeDirect(BitSet bs,int start,int range) {
        StringBuilder code = new StringBuilder();
        for (int j = start+range-1, offset = 0; offset < range;offset++) {
            code.append(bs.get(j-offset)?"1":"0");
        }
        return code;
    }
    private static StringBuilder getDecompressedData(BitSet bs, HashMap<String,Character>  huffmanTable,long fileLength,int startLoc,int endLoc){
        StringBuilder res=new StringBuilder();
        long charCounter=0;
        StringBuilder codeBuffer=new StringBuilder();
        for(int i=startLoc;i<endLoc;i++){
            codeBuffer.append(bs.get(i)?"1":"0");
            if (huffmanTable.containsKey(codeBuffer.toString())) {
                res.append(huffmanTable.get(codeBuffer.toString()));
                codeBuffer.setLength(0);
                charCounter++;
                if (charCounter == fileLength){
                    break;
                }
            }
        }
    return res;
    }
}
