package model;

import model.Compression.CompressedFileInfo;
import model.Decompression.DecompressedFileInfo;

import java.io.*;
import java.util.ArrayList;

public class FileManager{

    public static synchronized void CreateFolder(String destination){
        new File(destination).mkdirs();
    }
    public static synchronized void ClearFile(String destination){
        try {
            new PrintWriter(destination).close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static synchronized void AppendCompressedFile(byte[] data,String destination){
        File file = new File(destination);
        try {
            FileOutputStream fout=new FileOutputStream(file,true);
            for(byte b:data){
                fout.write((b&0xff));
            }
            fout.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static synchronized void AppendCompressedFile(CompressedFileInfo CFI,String destination){
        File file = new File(destination);
        try {
            FileOutputStream fout=new FileOutputStream(file,true);
            for(int i=0;i<CFI.headerData.size();i++){
                fout.write((CFI.headerData.get(i)&0xff));
            }
            for(byte b:CFI.fileData){
                fout.write((b&0xff));
            }
            fout.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static synchronized void WriteCompressedFile(CompressedFileInfo CFI,String destination){
        File file = new File(destination);
        try {
            FileOutputStream fout=new FileOutputStream(file);
            for(int i=0;i<CFI.headerData.size();i++){
                fout.write((CFI.headerData.get(i)&0xff));
            }
            for(byte b:CFI.fileData){
                fout.write((b&0xff));
            }
            fout.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static synchronized void WriteDecompressedFile(DecompressedFileInfo DFI,String path){
        File file = new File(path+DFI.fileName);
        try {
            FileOutputStream fout=new FileOutputStream(file);
            for(int i=0;i<DFI.fileData.length();i++){
                fout.write((DFI.fileData.charAt(i)&0xff));
            }
            fout.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
        //Reads file in binary and store it in Byte array
        public static synchronized byte[] ReadBinaryFile(String filePath){
            ArrayList<Byte> output=new ArrayList<>();
            File file = new File(filePath);
            try {

                FileInputStream fin=new FileInputStream(file);
                int data;
                while((data=fin.read())!=-1){
                    output.add((byte)data);
                }
                fin.close();
            }catch (Exception e) {
                e.printStackTrace();

            }
            //using byte arrays to make it easier to convert to other datatypes like BitSet
            byte[] arr=new byte[output.size()];
            for(int i=0;i<output.size();i++){
                arr[i]=output.get(i);
            }
            return arr;
        }

}

