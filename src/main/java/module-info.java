module LightMusic {
    requires javafx.fxml;
    requires javafx.media;
    requires javafx.controls;
    requires rxcontrols;
    requires jaudiotagger;
    requires java.desktop;
    requires javafx.swing;


    exports com.zichun.player;
    opens com.zichun.player to javafx.fxml;
}