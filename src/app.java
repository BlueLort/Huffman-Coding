import viewController.MainSceneController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class app extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("res/FXML/mainScene.fxml"));

        Parent root=loader.load();
        primaryStage.setTitle("Huffman Compression");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
       // primaryStage.setResizable(false);
        MainSceneController controller = loader.getController();
        controller.setStage(primaryStage);
    }


    public static void main(String[] args) {
        launch(args);
    }
}
