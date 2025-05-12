module org.example.catanboardgameapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires jdk.xml.dom;

    //requires org.controlsfx.controls;
    //requires org.kordamp.bootstrapfx.core;

    opens org.example.catanboardgameapp to javafx.fxml;
    exports org.example.catanboardgameapp;
    exports org.example.catanboardgameviews;
    opens org.example.catanboardgameviews to javafx.fxml;
    exports org.example.controller; // ✅ Add this line to fix the error

}