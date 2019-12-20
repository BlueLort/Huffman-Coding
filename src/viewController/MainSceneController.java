package viewController;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import model.Compression.CompressedFileInfo;
import model.Compression.CompressedFolderInfo;
import model.Compression.CompressionHandler;
import model.Decompression.DecompressedFileInfo;
import model.Decompression.DecompressedFolderInfo;
import model.Decompression.DecompressionHandler;
import model.FileManager;
import model.FrequencyChecker;
import model.HuffmanCompressor;
import javafx.concurrent.Task;

import javax.print.attribute.standard.Finishings;
import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.ResourceBundle;


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
    private int nFiles=0;
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
            String destination=getCompressedFilePath(tfFilePath.getText());
            if(isFile) {
                compressFileTask(destination);
            }else{
                nFiles=0;
                compressFolderTask(destination);
            }
        }

    }
    private boolean didCompress=true;
    private void compressFileTask(String destination){
        preventUse();
        Task task = new Task<Void>() {
            @Override public Void call() {
                String fileName=getFileName(tfFilePath.getText());
                byte[] data=FileManager.ReadBinaryFile(tfFilePath.getText());
                CompressedFileInfo CFI = CompressionHandler.getCompressedFile(
                        HuffmanCompressor.Compress(FrequencyChecker.GetFrequency(data))
                        , data
                        , fileName
                        ,true
                );
                didCompress=CFI.isCompressed;
                if(didCompress) {
                    FileManager.WriteCompressedFile(CFI, destination);
                }
                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                grantUse();
                if(didCompress==false){
                    lmessage.setText("Compression Rejected !!");
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
                CompressedFolderInfo CFOLDI = getFolderStructure(tfFilePath.getText());

                //TODO UPDATE PROGRESS OF TASK

                FileManager.ClearFile(destination);
                writeFolder(CFOLDI,destination);

                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                grantUse();
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }
    @FXML
    private void decompressOnAction(){
        lmessage.setText("");
        preventUse();
        Task task = new Task<Void>() {
            @Override public Void call() {
                byte[] data=FileManager.ReadBinaryFile(tfFilePath.getText());
                if(data[0]!=0x0f) {
                    BitSet bs=BitSet.valueOf(data);
                    DecompressedFileInfo DFI = DecompressionHandler.DecompressFile(data,bs,true);
                    String path = getFilePath(tfFilePath.getText());
                    FileManager.WriteDecompressedFile(DFI, path);
                }else{
                  DecompressedFolderInfo DFOLDI = DecompressionHandler.DecompressFolder(data,0);
                  writeDecompressedFolder(DFOLDI,getFilePath(tfFilePath.getText()));

                }
                return null;
            }
            @Override protected void succeeded() {
                super.succeeded();
                grantUse();
            }
        };
        bar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
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
    private CompressedFolderInfo getFolderStructure(String path) {
        File folder = new File(path);
        CompressedFolderInfo CFOLDI=new CompressedFolderInfo();
        CFOLDI.folderName=getFileName(path);
        ArrayList<Thread> tArr=new ArrayList<>();
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                CFOLDI.CFOLDIs.add(getFolderStructure(fileEntry.getAbsolutePath()));
            } else {
                nFiles++;

                Task task = new Task<Void>() {
                    @Override public Void call() {
                        String fileName=getFileName(fileEntry.getAbsolutePath());
                        byte[] data=FileManager.ReadBinaryFile(fileEntry.getAbsolutePath());
                        CompressedFileInfo CFI = CompressionHandler.getCompressedFile(
                                HuffmanCompressor.Compress(FrequencyChecker.GetFrequency(data))
                                , data
                                , fileName
                                ,false
                        );
                        CFOLDI.CFIs.add(CFI);
                        return null;
                    }
                };
                Thread t= new Thread(task);
                t.start();
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
    }
    private void grantUse(){
        bar.setVisible(false);
        bCompress.setDisable(false);
        bDecompress.setDisable(false);
        bChooseFile.setDisable(false);
        bChooseFolder.setDisable(false);
        cbEdit.setDisable(false);
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
}
