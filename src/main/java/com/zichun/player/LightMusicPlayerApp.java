package com.zichun.player;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.StageStyle;




public class LightMusicPlayerApp extends Application {

    //topPane的坐标记录
    private double oldStageX;
    private double oldStageY;
    private double oldScreenX;
    private double oldScreenY;


    public LightMusicPlayerApp() {
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/player.fxml"));
        Scene scene = new Scene(root,1280,800);
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setFullScreenExitHint("");//消除全屏时的提示
        primaryStage.show();

        scene.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                oldStageX = primaryStage.getX();
                oldStageY = primaryStage.getY();
                oldScreenX = event.getScreenX();
                oldScreenY = event.getScreenY();
            }
        });

        //鼠标拖拽
        scene.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //新位置
                //拖拽前后的鼠标差值加上原始窗体坐标值
                primaryStage.setX(event.getScreenX() - oldScreenX + oldStageX);
                primaryStage.setY(event.getScreenY() - oldScreenY + oldStageY);
            }
        });


    }

    public static void main(String[] args) {
        launch(args);
    }
}
