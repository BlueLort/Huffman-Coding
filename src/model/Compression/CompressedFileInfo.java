package model.Compression;
import java.util.ArrayList;

public class CompressedFileInfo{
    public boolean isCompressed;
    public ArrayList<Byte> headerData;
    public byte[] fileData;
    public CompressedFileInfo(){
        isCompressed=false;
    }
    public CompressedFileInfo(ArrayList<Byte> headerData, byte[] fileData){
        this.headerData=headerData;
        this.fileData=fileData;
        isCompressed=true;
    }

}