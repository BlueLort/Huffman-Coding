package model.Decompression;

import java.util.ArrayList;

public class DecompressedFileInfo {
    public String fileName;
    public String fileData;
    public int maxReach;
    public DecompressedFileInfo(String fileName, String fileData,int maxReach){
        this.fileName=fileName;
        this.fileData=fileData;
        this.maxReach=maxReach;
    }

}
