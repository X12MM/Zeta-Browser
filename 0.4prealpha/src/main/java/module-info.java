module org.example.browser1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.base;

    opens org.example.browser1 to javafx.fxml, javafx.graphics;
    exports org.example.browser1;
}