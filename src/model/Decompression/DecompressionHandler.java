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
                Thread t=instantiateDecompression(data,DFOLDI);
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


    private static Thread instantiateDecompression(byte[] data,DecompressedFolderInfo DFOLDI) {
        Task task = new Task<Void>() {
            @Override
            public Void call() {
                DecompressedFileInfo DFI = DecompressFile(data, BitSet.valueOf(data));
                DFOLDI.DFIs.add(DFI);
                return null;
            }
        };
        Thread t = new Thread(task);
        t.start();
        return t;
    }
    public static DecompressedFileInfo DecompressFile(byte[] fileData,BitSet bs){
        //Note &<hexNumber> is to cast byte values avoiding the negative values.
        int compressionFormat=fileData[0]&0xff;
        String data="";//The Output String
        String fileName="";
        int codeFormat=compressionFormat-0x0f9;//get Code fromat as 1,2,3 or 4 [Starts from 0xaf

        short nChars=(short)((fileData[1]&0xff)+1);

        long fileLength=((long)((fileData[2]&0xff)<<24) | (long)((fileData[3]&0xff)<<16) | (long)((fileData[4]&0xff)<<8) | (long)(fileData[5]&0xff));

        short fileNameLen=(short)((fileData[6]&0xff)+1);
        fileName=getFileName(fileData,fileNameLen);

        HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,fileNameLen,bs,nChars,codeFormat);

        /* for debugging -> must uncomment debug equality in HuffmanCompressor
        for(Map.Entry<String,Character> e:huffmanTable.entrySet()){
            System.out.printf("%s %s %s\n",e.getValue(),e.getKey(),debug.get(e.getValue()));
        }
        */
        int startLoc=((nChars*(codeFormat+1))+7+fileNameLen)*8;
        int endLoc=fileData.length*8;//i can just read until i find file chars
        data=getDecompressedData(bs,huffmanTable,fileLength,startLoc,endLoc);
        return new DecompressedFileInfo(fileName,data);
    }

    private static String getFileName(byte[] fileData,int fileNameLen){
        String res="";
        for(int i=0;i<fileNameLen;i++){
            res+=(char)(fileData[7+i]&0xff);
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
    private static HashMap<String,Character> GetDecompressionTable(byte[] fileData,int fileNameLen,BitSet fileDataBits,int nChars,int codeFormat){
        HashMap<String,Character> huffmanTable=new HashMap<>();
        //make the end of data depends if using Double or Single Byte data for the huffmanTable
        int endLoc=(nChars*(codeFormat+1))+7+fileNameLen;
        int step=codeFormat+1;
        String code="";
        for(int i=7+fileNameLen;i<endLoc;i+=step){
            int idx=1;
            switch (codeFormat){
                case 1:
                    code=getCode(fileDataBits,(i+1)*8,8);
                    break;
                default:
                    do{
                        code=getCode(fileDataBits,(i+idx)*8,8);
                        idx++;
                    }while(code.equals("NO"));
                    while(idx<=codeFormat){
                        code+=getCodeDirect(fileDataBits,(i+idx)*8,8);
                        idx++;
                    }
                    break;
            }
            huffmanTable.put(code,Character.toChars(fileData[i]&0xff)[0]);
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
    private static String getDecompressedData(BitSet bs, HashMap<String,Character>  huffmanTable,long fileLength,int startLoc,int endLoc){
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
                    break;
                }
            }
        }
    return res;
    }
}
