package model.Compression;

import javafx.util.Pair;

import java.util.ArrayList;

public class CompressedFolderInfo {
    public ArrayList<CompressedFileInfo> CFIs;
    public ArrayList<CompressedFolderInfo> CFOLDIs;
    public String folderName;
    public CompressedFolderInfo(){
        this.CFIs=new ArrayList<>();
        this.CFOLDIs=new ArrayList<>();

    }
}
