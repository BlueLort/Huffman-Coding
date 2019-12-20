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

public class DecompressionHandler {
    public static DecompressedFolderInfo DecompressFolder(byte[] fileData,int start){
        DecompressedFolderInfo DFOLDI=new DecompressedFolderInfo();
        int nFiles=(((int)fileData[1+start]<<8&0xff00)|(fileData[2+start]&0xff));
        DFOLDI.folderName=extractFolderName(fileData,start);
        int offset=4+DFOLDI.folderName.length()+start;
        for(int i=0;i<nFiles;i++){
            byte nextFile=fileData[offset];
            if(nextFile!=0x0f){
                byte[] data=new byte[fileData.length-offset];
                for(int j=0;j<data.length;j++){
                    data[j]=fileData[j+offset];
                }
                DecompressedFileInfo DFI=DecompressFile(data);
                offset=offset+DFI.maxReach;
                DFOLDI.DFIs.add(DFI);
            }else{
                DFOLDI.DFOLDIs.add(DecompressFolder(fileData,offset));
                offset=DFOLDI.DFOLDIs.get(DFOLDI.DFOLDIs.size()-1).maxReach;
            }
        }
        DFOLDI.maxReach=offset;
        return DFOLDI;
    }
    private static int byteReached=0;
    public static DecompressedFileInfo DecompressFile(byte[] fileData){
        //Note &<hexNumber> is to cast byte values avoiding the negative values.
        int isCompressed=fileData[0]&0xff;
        byteReached=0;
        String data="";//The Output String
        String fileName="";
        if(isCompressed!=0){
            boolean doubleByte=(isCompressed==0xff);
            short nChars=(short)((fileData[1]&0xff)+1);
            long fileLength=((short)(fileData[2]<<24&0xff000000) | (short)(fileData[3]<<16&0xff0000) | (short)(fileData[4]<<8&0xff00) | (short)(fileData[5]&0xff));
            short fileNameLen=(short)((fileData[6]&0xff)+1);
            fileName=getFileName(fileData,fileNameLen);

            BitSet bs=BitSet.valueOf(fileData);

            HashMap<String,Character> huffmanTable =GetDecompressionTable(fileData,fileNameLen,bs,nChars,doubleByte);
            System.out.println(huffmanTable);
            System.out.printf("Number of Leaves:%d\n",huffmanTable.size());

            int startLoc=((doubleByte?(nChars*3)+7+fileNameLen:(nChars*2)+7+fileNameLen))*8;
            int endLoc=fileData.length*8;//i can just read until i find file chars
            data=getDecompressedData(bs,huffmanTable,doubleByte,nChars,fileLength,startLoc,endLoc);
        }else{
            int fileLen=fileData[1];
            int len=fileData[2];
            for(int i=3,offset=0;offset<=len;offset++){
                fileName+=(char)(fileData[i+offset]&0xff);
            }
            for(int i=4+len,offset=0;offset<fileLen;offset++){
                data+=(char)(fileData[i+offset]);
            }
            byteReached=4+len+fileLen;
        }
        System.out.println(fileName);
        System.out.println(data);
        return new DecompressedFileInfo(fileName,data,byteReached);
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
    private static HashMap<String,Character> GetDecompressionTable(byte[] fileData,int fileNameLen,BitSet fileDataBits,int nChars,boolean doubleByte){
        HashMap<String,Character> huffmanTable=new HashMap<>();
        //make the end of data depends if using Double or Single Byte data for the huffmanTable
        int endLoc=(doubleByte?(nChars*3):(nChars*2))+6+fileNameLen;
        int step=doubleByte?3:2;

        for(int i=7+fileNameLen;i<endLoc;i+=step){
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
                    byteReached=(int)Math.ceil((double)i/8.0);
                    break;
                }
            }
        }
    return res;
    }
}
