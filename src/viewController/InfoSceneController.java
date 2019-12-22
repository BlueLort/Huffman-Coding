package viewController;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.*;

public class InfoSceneController implements Initializable {

    @FXML
    private Label minBits;
    @FXML
    private Label maxBits;
    @FXML
    private Label fileName;
    @FXML
    private Label exeTime;
    @FXML
    private Label compressionRatio;
    @FXML
    private Label nLeaves;
    @FXML
    private Label crlabel;
    @FXML
    private TableView<InfoModel> dataTable ;
    @FXML
    private TableColumn charCol;
    @FXML
    private TableColumn byteCol;
    @FXML
    private TableColumn codeCol;
    @FXML
    private TableColumn newCodeCol;




    private HashMap<Character,String> huffmanTree;
    private String fName;
    private int nMaxBits;
    private int nMinBits;
    private int nChars;
    private double timeSeconds;
    private double CR;



    @Override
    public void initialize(URL url, ResourceBundle rb) {
        charCol.setCellValueFactory( new PropertyValueFactory<InfoModel, Character>("ch"));
        byteCol.setCellValueFactory( new PropertyValueFactory<InfoModel, Integer>("byteVal"));
        codeCol.setCellValueFactory( new PropertyValueFactory<InfoModel, String>("code"));
        newCodeCol.setCellValueFactory( new PropertyValueFactory<InfoModel, String>("newCode"));

    }
    public void showData() {
        fileName.setText(fName);
        maxBits.setText(Integer.toString(nMaxBits));
        minBits.setText(Integer.toString(nMinBits));
        nLeaves.setText(Integer.toString(nChars));
        exeTime.setText(Double.toString(timeSeconds)+" Seconds");
        ObservableList<InfoModel> data=FXCollections.observableArrayList(
        );
        for(Map.Entry<Character,String> e:huffmanTree.entrySet()){
            data.add(new InfoModel(e.getKey(),e.getKey()&0xff,InfoHandler.getBinary(e.getKey()),e.getValue()));
        }
        if(CR!=-1.0) {
            compressionRatio.setText(Double.toString(CR));
            crlabel.setVisible(true);
        }
        dataTable.setItems(data);


    }
    public void setHuffmanTree(HashMap<Character, String> huffmanTree) {
        this.huffmanTree = huffmanTree;
    }
    public void setFileName(String fileName) {
        fName = fileName;
    }
    public void setnMaxBits(int nMaxBits) {
        this.nMaxBits = nMaxBits;
    }
    public void setnMinBits(int nMinBits) {
        this.nMinBits = nMinBits;
    }
    public void setnChars(int nChars) {
        this.nChars = nChars;
    }
    public void setTimeSeconds(double timeSeconds) {
        this.timeSeconds = timeSeconds;
    }
    public void setCR(double CR) {
        this.CR = CR;
    }

    public class InfoModel {

        private final Character ch;
        private final Integer byteVal;
        private final String code;
        private final String newCode;

        private InfoModel(Character ch,Integer byteVal, String code,String newCode) {
            this.ch=ch;
            this.byteVal=byteVal;
            this.code=code;
            this.newCode=newCode;
        }

        public Character getCh() {
            return ch;
        }

        public Integer getByteVal() {
            return byteVal;
        }

        public String getCode() {
            return code;
        }

        public String getNewCode() {
            return newCode;
        }


    }
}
