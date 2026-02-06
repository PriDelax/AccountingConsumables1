module com.example.industrialpracticeuchet {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.prefs;
    requires jdk.jfr;


    opens com.example.industrialpracticeuchet to javafx.fxml;
    exports com.example.industrialpracticeuchet;
}