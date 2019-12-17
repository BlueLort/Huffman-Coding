package viewController;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.FileManager;
import model.FrequencyChecker;
import model.HuffmanCompressor;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
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
    private CheckBox cbEdit;
    @FXML
    private TextField tfFilePath;

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
    private void compressOnAction(){
        //Read the File and get the frequency of characters then send it to HuffmanCompression->generate codes
        //Then FileManager will just write the file again using the generated code to the file.
        String data=FileManager.ReadFile(tfFilePath.getText());
        FileManager.WriteCompressedFile(
                HuffmanCompressor.Compress(FrequencyChecker.GetFrequency(data))
                ,data
                ,tfFilePath.getText()+"OH"//OH -> OMAR HARRAZ
        );

    }
    @FXML
    private void decompressOnAction(){
        //Read the File in binary format -> first read the header information to get the file content
        //Destination is defaulted to same file with extension .OH
        String destination=tfFilePath.getText();
        if(tfFilePath.getText().contains(".")){
            destination= tfFilePath.getText().substring(0, tfFilePath.getText().lastIndexOf('.'));
        }
        destination+=".OH";
        FileManager.DecompressFile(FileManager.ReadBinaryFile(tfFilePath.getText()),destination);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }


}
