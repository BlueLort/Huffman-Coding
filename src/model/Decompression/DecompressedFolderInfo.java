package model.Decompression;

import model.Compression.CompressedFileInfo;
import model.Compression.CompressedFolderInfo;

import java.util.ArrayList;

public class DecompressedFolderInfo {
    public ArrayList<DecompressedFileInfo> DFIs;
    public ArrayList<DecompressedFolderInfo> DFOLDIs;
    public String folderName;
    public int maxReach;
    public DecompressedFolderInfo(){
        this.DFIs=new ArrayList<>();
        this.DFOLDIs=new ArrayList<>();

    }
}
