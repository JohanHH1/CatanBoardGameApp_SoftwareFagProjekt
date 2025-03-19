module org.example.catanboardgameapp {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens org.example.catanboardgameapp to javafx.fxml;
    exports org.example.catanboardgameapp;
    exports org.example.catanboardgameviews;
    opens org.example.catanboardgameviews to javafx.fxml;
}