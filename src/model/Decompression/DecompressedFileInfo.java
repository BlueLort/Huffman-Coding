package model.Decompression;

import java.util.ArrayList;
import java.util.HashMap;

public class DecompressedFileInfo {
    public String fileName;
    public String fileData;
    public HashMap<String,Character> decompressedMap;
    public DecompressedFileInfo(String fileName, String fileData){
        this.fileName=fileName;
        this.fileData=fileData;
    }

}
