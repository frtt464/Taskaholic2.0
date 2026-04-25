module com.taskaholic {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires com.google.gson;

    opens com.taskaholic to com.google.gson, javafx.fxml;
    exports com.taskaholic;
}