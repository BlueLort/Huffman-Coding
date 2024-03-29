package viewController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.*;
import model.Compression.CompressedFileInfo;
import model.Compression.CompressedFolderInfo;
import model.Compression.CompressionHandler;
import model.Decompression.DecompressedFileInfo;
import model.Decompression.DecompressedFolderInfo;
import model.Decompression.DecompressionHandler;
import javafx.concurrent.Task;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;


public class MainSceneController implements Initializable {


    private  Stage window;
    @FXML
    private AnchorPane ap;
    @FXML
    private Button bCompress;
    @FXML
    private Button bDecompress;
    @FXML
    private Button bChooseFile;
    @FXML
    private Button bChooseFolder;

    @FXML
    private Label lmessage;

    @FXML
    ProgressBar bar;

    @FXML
    private CheckBox cbEdit;
    @FXML
    private TextField tfFilePath;
    private long startTime;
    public void setStage(Stage PrimaryStage){
        window = PrimaryStage;
    }
    @FXML
    private void editOnAction(){
        tfFilePath.setDisable(!tfFilePath.isDisable());
    }
    @FXML
    private void chooseFileOnAction(){
        //Just Make a File Chooser to get Absolute path and set with it the TextField
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("All Files", "*.*");
        fileChooser.getExtensionFilters().add(extFilter);
        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
        fileChooser.setInitialDirectory(new File(currentPath));
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
           tfFilePath.setText(file.getAbsolutePath());
        }
    }
    @FXML
    private void chooseFolderOnAction(){
        //Just Make a File Chooser to get Absolute path and set with it the TextField
        DirectoryChooser dirChooser=new DirectoryChooser();
        String currentPath = Paths.get(".").toAbsolutePath().normalize().toString();
        dirChooser.setInitialDirectory(new File(currentPath));
        File file = dirChooser.showDialog(window);
        if (file != null) {
            tfFilePath.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void compressOnAction(){
        boolean exists=exists(tfFilePath.getText());
        boolean isFile=isFile(tfFilePath.getText());
        lmessage.setText("");
        if(exists) {
            success=false;
            String destination=getCompressedFilePath(tfFilePath.getText());
            if(isFile) {
                compressFileTask(destination);
            }else{
                compressFolderTask(destination);
            }
        }else{
            lmessage.setText("Compression Failed !!");
        }

    }
    private boolean didCompress=true;
    private InfoHandler IH;
    private boolean success=false;
    private void compressFileTask(String destination){
        preventUse();
        Task task = new Task<Void>() {
            @Override public Void call() {
                String fileName=getFileName(tfFilePath.getText());
                byte[] data=FileManager.ReadBinaryFile(tfFilePath.getText());
                CompressionInfo CI= HuffmanCompressor.Compress(FrequencyChecker.GetFrequency(data),fileName);
                CI.compressionTest();
                didCompress=CI.isCompressible;
                IH=new InfoHandler(CI);
                if(didCompress) {
                    CompressedFileInfo CFI = CompressionHandler.getCompressedFile(
                            CI
                            , data
                    );
                    FileManager.WriteCompressedFile(CFI,destination);
                }
                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                success=true;
                grantUse();
                if(didCompress==false){
                    lmessage.setText("Compression Rejected !!");
                }
            }
            @Override protected void failed() {
                super.failed();
                grantUse();
                if(didCompress==false){
                    lmessage.setText("Compression Failed !!");
                }
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }
    private void compressFolderTask(String destination){
        preventUse();
        Task task = new Task<Void>() {
            @Override public Void call() {
                ArrayList<String> filePaths =new ArrayList<>();
                ArrayList<String> folderPaths =new ArrayList<>();
                getFilePaths(tfFilePath.getText(),filePaths,folderPaths);
                byte[] data=FileManager.ReadBinaryFiles(filePaths);
                String folderName=getFileName(tfFilePath.getText());
                CompressionInfo CI= HuffmanCompressor.Compress(FrequencyChecker.GetFrequency(data),folderName);
                FolderCompressionInfo FCI = new FolderCompressionInfo(CI);
                for(String s:folderPaths){
                    FCI.folderNames.add(getFileName(s));
                }
                for(String s:filePaths){
                    FCI.fileNames.add(getFileName(s));
                }
                FCI.compressionTest();
                CI.nMaxBits=FCI.nMaxBits;
                CI.nMinBits=FCI.nMinBits;
                CI.compressionRatio=FCI.compressionRatio;
                IH=new InfoHandler(CI);
                didCompress=FCI.isCompressible;
                if(didCompress) {
                    CompressedFolderInfo CFOLDI = getFolderStructure(tfFilePath.getText(),FCI);
                    FileManager.ClearFile(destination);
                    writeHuffmanTree(FCI,destination);
                    writeFolder(CFOLDI,destination);
                }
                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                success=true;
                grantUse();
                if(didCompress==false){
                    lmessage.setText("Compression Rejected !!");
                }

            }
            @Override protected void failed() {
                super.failed();
                grantUse();
                if(didCompress==false){
                    lmessage.setText("Compression Failed !!");
                }

            }
        };
        bar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }
    @FXML
    private void decompressOnAction(){

        lmessage.setText("");
        boolean exists=exists(tfFilePath.getText());
        boolean isFile=isFile(tfFilePath.getText());
        if(exists&&isFile) {
            success=false;
            preventUse();
            Task task = new Task<Void>() {
                @Override
                public Void call() {
                    byte[] data = FileManager.ReadBinaryFile(tfFilePath.getText());
                    if ((data[0] & 0xff) != 0x0f0) {
                        BitSet bs = BitSet.valueOf(data);
                        DecompressedFileInfo DFI = DecompressionHandler.DecompressFile(data, bs);
                        CompressionInfo CI = getCIFromReversedMap(DFI.decompressedMap);
                        CI.fileName = getFileName(tfFilePath.getText());
                        IH = new InfoHandler(CI);
                        String path = getFilePath(tfFilePath.getText());
                        FileManager.WriteDecompressedFile(DFI, path);

                    } else {
                        byte[] huffmanData = getHuffmanData(data);
                        HashMap<String, Character> huffmanTable = DecompressionHandler.GetFolderDecompressionTable(huffmanData, BitSet.valueOf(huffmanData));
                        CompressionInfo CI = getCIFromReversedMap(huffmanTable);
                        CI.fileName = getFileName(tfFilePath.getText());
                        IH = new InfoHandler(CI);
                        DecompressedFolderInfo DFOLDI = DecompressionHandler.DecompressFolder(data, huffmanData.length, huffmanTable);
                        writeDecompressedFolder(DFOLDI, getFilePath(tfFilePath.getText()));

                    }
                    return null;
                }

                @Override
                protected void succeeded() {
                    super.succeeded();
                    success=true;
                    grantUse();
                }
                @Override
                protected void failed() {
                    super.failed();
                    grantUse();
                    lmessage.setText("Deompression Failed !!");
                }
            };
            bar.progressProperty().bind(task.progressProperty());
            new Thread(task).start();
        }else{
            lmessage.setText("Decompression Failed !!");
        }
    }
    private static byte[] getHuffmanData(byte[] data){
        int codeFormat=(data[1]&0xff)-0x0f9;
        int nChars=(data[2]&0xff)+1;
        byte[] arr=new byte[3+(codeFormat+1)*nChars];
        for(int i=0;i<arr.length;i++){
            arr[i]=data[i];
        }
        return arr;
    }

    private void writeDecompressedFolder(DecompressedFolderInfo DFOLDI,String destination){
        String currentPath=destination+DFOLDI.folderName+'/';
        FileManager.CreateFolder(currentPath);
        for(DecompressedFileInfo DFI:DFOLDI.DFIs){
            FileManager.WriteDecompressedFile(DFI,currentPath);
        }
        for(DecompressedFolderInfo D:DFOLDI.DFOLDIs){
            writeDecompressedFolder(D,currentPath);
        }

    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }


    private void getFilePaths(String path, ArrayList<String> filePaths,ArrayList<String> folderPaths){
        File folder = new File(path);
        folderPaths.add(path);
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
               getFilePaths(fileEntry.getAbsolutePath(),filePaths,folderPaths);
            } else {
                filePaths.add(fileEntry.getAbsolutePath());
            }
        }
    }
    private CompressedFolderInfo getFolderStructure(String path,FolderCompressionInfo FCI) {
        File folder = new File(path);
        CompressedFolderInfo CFOLDI=new CompressedFolderInfo();
        CFOLDI.folderName=getFileName(path);
        ArrayList<Thread> tArr=new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                CFOLDI.CFOLDIs.add(getFolderStructure(fileEntry.getAbsolutePath(),FCI));
            } else {
                Thread t=startFileCompression(fileEntry,CFOLDI,FCI);
                tArr.add(t);
            }
        }
        for(Thread t:tArr){
            try{
                t.join();
            }catch (Exception e){
                System.out.println(e.getStackTrace());
            }

        }
        return CFOLDI;

    }
    private Thread startFileCompression(final File fileEntry,CompressedFolderInfo CFOLDI,FolderCompressionInfo FCI){
        Task task = new Task<Void>() {
            @Override public Void call() {
                String fileName=getFileName(fileEntry.getAbsolutePath());
                byte[] data=FileManager.ReadBinaryFile(fileEntry.getAbsolutePath());
                CompressedFileInfo CFI=CompressionHandler.getCompressedFile(FCI.huffmanCodes,data,fileName);
                CFOLDI.CFIs.add(CFI);
                return null;
            }
        };
        Thread t= new Thread(task);
        t.start();
        return t;
    }

    private void writeHuffmanTree(FolderCompressionInfo FCI,String destination){
        byte[] headerData=new byte[3+FCI.huffmanCodes.size()*(FCI.codeFormat+1)];
        int idx=0;
        headerData[idx++]=(byte)0xf0;
        headerData[idx++]=(byte)((FCI.codeFormat&0xff)+0x0f9);
        headerData[idx++]=(byte)((FCI.huffmanCodes.size()&0xff)-1);
        for(Map.Entry<Character,String> e:FCI.huffmanCodes.entrySet()){
            headerData[idx++]=(byte)(e.getKey()&0xff);
            long val;
            val=Long.parseLong(e.getValue(),2);
            val |=(2<<(e.getValue().length()-1));
            for(int i=FCI.codeFormat-1;i>=0;i--){
                headerData[idx++]=((byte)((val>>(i*8))&0xff));
            }
        }
        FileManager.AppendCompressedFile(headerData,destination);
    }
    private void writeFolder(CompressedFolderInfo  CFOLDI,String destination){
        byte[] folderHeader=getFolderHeader(CFOLDI);
        FileManager.AppendCompressedFile(folderHeader,destination);
        for(int i=0;i<CFOLDI.CFIs.size();i++){
            FileManager.AppendCompressedFile(CFOLDI.CFIs.get(i),destination);
        }
        for(int i=0;i<CFOLDI.CFOLDIs.size();i++){
            writeFolder(CFOLDI.CFOLDIs.get(i),destination);
        }
    }
    private byte[] getFolderHeader(CompressedFolderInfo  CFOLDI){
        byte[] folderHeader=new byte[4+CFOLDI.folderName.length()];
        folderHeader[0]=0x0f;
        int len=CFOLDI.CFIs.size()+CFOLDI.CFOLDIs.size();
        folderHeader[1]=(byte)(len>>8);
        folderHeader[2]=(byte)(len);
        folderHeader[3]=(byte)(CFOLDI.folderName.length());
        for(int i=0;i<CFOLDI.folderName.length();i++){
            folderHeader[i+4]=(byte)(CFOLDI.folderName.charAt(i)&0xff);
        }
        return folderHeader;
    }
    private String getCompressedFilePath(String path){
        String destination=path;
        if(path.contains(".")){
            destination= path.substring(0, path.lastIndexOf('.'));
        }
        destination+=".OH";
        return destination;
    }
    private void preventUse(){
        bar.setVisible(true);
        bCompress.setDisable(true);
        bDecompress.setDisable(true);
        bChooseFile.setDisable(true);
        bChooseFolder.setDisable(true);
        cbEdit.setDisable(true);
        startTime=System.nanoTime();
    }
    private void grantUse(){
        bar.setVisible(false);
        bCompress.setDisable(false);
        bDecompress.setDisable(false);
        bChooseFile.setDisable(false);
        bChooseFolder.setDisable(false);
        cbEdit.setDisable(false);
        if(success)IH.DisplayInfo((System.nanoTime()-startTime)/1000000000.0);
    }

    private String getFileName(String path){
        String fileName="";
        if(path.contains("/")){
            fileName= path.substring(path.lastIndexOf('/')+1,path.length());
        }else{
            fileName= path.substring( path.lastIndexOf('\\')+1,path.length());
        }
        return fileName;
    }
    private String getFilePath(String path){
        String fileName="";
        if(tfFilePath.getText().contains("/")){
            fileName= path.substring(0,path.lastIndexOf('/')+1);
        }else{
            fileName= path.substring( 0,path.lastIndexOf('\\')+1);
        }
        return fileName;
    }

    private boolean isFile(String fpath){
        File file = new File(fpath);
        return file.isFile();
    }
    private boolean exists(String fpath){
        File file = new File(fpath);
        return file.exists();
    }

    private CompressionInfo getCIFromReversedMap( HashMap<String,Character> decompressedMap){
        HashMap<Character,String> huffmanTable=new HashMap<>();
        int min=0x7fffffff;
        int max=-1;
        for(Map.Entry<String,Character> e:decompressedMap.entrySet()){
            huffmanTable.put(e.getValue(),e.getKey());
            if(max<e.getKey().length()){
                max=e.getKey().length();
            }
            if(min>e.getKey().length()){
                min=e.getKey().length();
            }
        }

        CompressionInfo CI=new CompressionInfo(huffmanTable);
        CI.nMinBits=min;
        CI.nMaxBits=max;
        CI.compressionRatio=-1;

        return CI;

    }
}
