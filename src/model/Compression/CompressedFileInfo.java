package model.Compression;
import java.util.ArrayList;

public class CompressedFileInfo{
    public ArrayList<Byte> headerData;
    public byte[] fileData;
    public CompressedFileInfo(){
    }
    public CompressedFileInfo(ArrayList<Byte> headerData, byte[] fileData){
        this.headerData=headerData;
        this.fileData=fileData;
    }

}