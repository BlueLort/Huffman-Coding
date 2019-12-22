package model;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Pair;
import viewController.InfoSceneController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InfoHandler {
    private CompressionInfo CI;
    private double exeTime;
    public InfoHandler(CompressionInfo CI){
        this.CI=CI;
    }
    public void DisplayInfo(double exeTime){
        this.exeTime=exeTime;
        //CONSOLE
        System.out.print("=========================================\nFile Name: ");
        System.out.println(CI.fileName);
        System.out.println("=========================================");
        System.out.printf("Char\tByte\t  Code\t\t\tNew Code\n");
        for(Map.Entry<Character,String> e:CI.huffmanCodes.entrySet()){
            System.out.printf("%s\t\t%d\t\t%s\t\t   %s\n",e.getKey(),(int)e.getKey(),getBinary(e.getKey()),e.getValue());
        }
        System.out.printf("nBits min:%d , max:%d\n",CI.nMinBits,CI.nMaxBits);
        System.out.printf("Number of leaves:%d\n",CI.huffmanCodes.size());
        System.out.printf("Execution Time: ");
        System.out.print(exeTime);
        System.out.println(" Seconds");
        System.out.printf("Compression Ratio: ");
        System.out.println(CI.compressionRatio);
        System.out.println("========================================");
        //GUI
        try {
            showInfoGUI();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void showInfoGUI()throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("../res/FXML/infoScene.fxml"));
        Parent root=loader.load();
        Stage s=new Stage();
        s.setTitle("Huffman Compression");
        s.setScene(new Scene(root));
        s.show();
        s.setResizable(false);
        InfoSceneController controller = loader.getController();

        controller.setCR(CI.compressionRatio);
        controller.setTimeSeconds(exeTime);
        controller.setFileName(CI.fileName);
        controller.setnChars(CI.huffmanCodes.size());
        controller.setHuffmanTree(CI.huffmanCodes);
        controller.setnMaxBits(CI.nMaxBits);
        controller.setnMinBits(CI.nMinBits);

        controller.showData();


    }
    public static String getBinary(int x){
        String res=Integer.toBinaryString(x);
        String padding="";
        int diff=8-res.length();
        for(int i=0;i<diff;i++)padding+="0";
        return padding+res;
    }
}
