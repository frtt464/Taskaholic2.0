package com.taskaholic;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.InputStream;

public class TaskaholicApp extends Application {
    private final List<User> users = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private int nextTaskId = 1;

    private Stage primaryStage;
    private User currentUser;
    private static boolean isDarkMode = false;
    private boolean pendingRestoreFullScreen = false;
    private boolean pendingRestoreMaximized = false;
    private Timeline liveSyncTimeline;

    private final ObservableList<Task> taskObservable = FXCollections.observableArrayList();
    private final ObservableList<Task> draftObservable = FXCollections.observableArrayList();
    private final ObservableList<Task> myBookingObservable = FXCollections.observableArrayList();
    private TableView<Task> draftTableView;
    private TableView<Task> publishedTableView;
    private TableView<Task> availableTableView;
    private TableView<Task> myBookingTableView;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        loadData();
        showLoginScene();
        stage.setTitle("Taskaholic");
        applyAppIcon(stage);
        stage.show();
    }

    private void applyAppIcon(Stage stage) {
        // Prefer PNG for JavaFX stage icons; ICO is primarily for native launcher packaging.
        String[] candidates = {"/taskaholic-logo.png", "/taskaholic.png", "/logo.png", "/taskaholic-logo.ico"};
        for (String path : candidates) {
            try (InputStream stream = getClass().getResourceAsStream(path)) {
                if (stream != null) {
                    Image image = new Image(stream);
                    if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                        stage.getIcons().add(image);
                        return;
                    }
                }
            } catch (Exception ignored) {
                // Ignore and try next candidate.
            }
        }
    }

    private void loadData() {
        users.clear();
        tasks.clear();
        users.addAll(DataStore.loadUsers());
        tasks.addAll(DataStore.loadTasks());

        if (users.isEmpty()) {
            users.add(new User("TaskMaster0677", "pw", "TASKMASTER"));
            users.add(new User("TaskMaster1234", "pw", "TASKMASTER"));
            users.add(new User("TaskerA", "pw", "TASKER"));
            users.add(new User("TaskerB", "pw", "TASKER"));
            DataStore.saveUsers(users);
        }

        if (tasks.isEmpty()) {
            tasks.add(new Task(1, "Pick up child from school", "Phnom Penh", 8.00, "TaskMaster0677", TaskStatus.PUBLISHED));
            tasks.add(new Task(2, "Buy groceries at nearest supermarket", "Phnom Penh", 12.50, "TaskMaster1234", TaskStatus.PUBLISHED));
            DataStore.saveTasks(tasks);
        }

        nextTaskId = tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    private void syncTasksFromDisk() {
        tasks.clear();
        tasks.addAll(DataStore.loadTasks());
        nextTaskId = tasks.stream().mapToInt(Task::getId).max().orElse(0) + 1;
    }

    private void startLiveTaskSync() {
        stopLiveTaskSync();
        liveSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(1.5), e -> {
            if (currentUser != null) {
                refreshVisibleTables();
            }
        }));
        liveSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        liveSyncTimeline.play();
    }

    private void stopLiveTaskSync() {
        if (liveSyncTimeline != null) {
            liveSyncTimeline.stop();
            liveSyncTimeline = null;
        }
    }

    private void refreshTaskLists() {
        syncTasksFromDisk();
        taskObservable.setAll(tasks.stream().filter(Task::isVisibleToTasker).toList());
        draftObservable.setAll(tasks.stream()
                .filter(t -> currentUser != null
                        && currentUser.getRole() == Role.TASKMASTER
                        && currentUser.getUsername().equals(t.getCreatedBy())
                        && t.getStatus() == TaskStatus.DRAFT)
                .toList());
        myBookingObservable.setAll(tasks.stream()
                .filter(t -> currentUser != null
                        && currentUser.getUsername().equals(t.getBookedBy())
                        && (t.getStatus() == TaskStatus.ACCEPTED
                        || t.getStatus() == TaskStatus.PERFORMING
                        || t.getStatus() == TaskStatus.COMPLETED))
                .toList());
    }

    private void restoreSelectionById(TableView<Task> table, int selectedTaskId) {
        if (table == null || selectedTaskId < 0) {
            return;
        }
        for (Task task : table.getItems()) {
            if (task.getId() == selectedTaskId) {
                table.getSelectionModel().select(task);
                table.scrollTo(task);
                return;
            }
        }
    }

    private void refreshVisibleTables() {
        if (currentUser == null) {
            refreshTaskLists();
            return;
        }

        int draftSelectedId = draftTableView == null || draftTableView.getSelectionModel().getSelectedItem() == null
                ? -1
                : draftTableView.getSelectionModel().getSelectedItem().getId();
        int publishedSelectedId = publishedTableView == null || publishedTableView.getSelectionModel().getSelectedItem() == null
                ? -1
                : publishedTableView.getSelectionModel().getSelectedItem().getId();
        int availableSelectedId = availableTableView == null || availableTableView.getSelectionModel().getSelectedItem() == null
                ? -1
                : availableTableView.getSelectionModel().getSelectedItem().getId();
        int bookingSelectedId = myBookingTableView == null || myBookingTableView.getSelectionModel().getSelectedItem() == null
                ? -1
                : myBookingTableView.getSelectionModel().getSelectedItem().getId();

        refreshTaskLists();

        if (publishedTableView != null) {
            publishedTableView.setItems(FXCollections.observableArrayList(
                    tasks.stream().filter(t -> t.getCreatedBy().equals(currentUser.getUsername())).toList()
            ));
            publishedTableView.refresh();
            autoSizeLocationColumn(publishedTableView);
            restoreSelectionById(publishedTableView, publishedSelectedId);
        }

        if (draftTableView != null) {
            draftTableView.refresh();
            autoSizeLocationColumn(draftTableView);
            restoreSelectionById(draftTableView, draftSelectedId);
        }

        if (availableTableView != null) {
            availableTableView.refresh();
            autoSizeLocationColumn(availableTableView);
            restoreSelectionById(availableTableView, availableSelectedId);
        }

        if (myBookingTableView != null) {
            myBookingTableView.refresh();
            autoSizeLocationColumn(myBookingTableView);
            restoreSelectionById(myBookingTableView, bookingSelectedId);
        }
    }

    private void autoSizeLocationColumn(TableView<Task> table) {
        if (table == null) {
            return;
        }

        TableColumn<Task, ?> locationColumn = table.getColumns().stream()
                .filter(column -> "Location".equals(column.getText()))
                .findFirst()
                .orElse(null);
        if (locationColumn == null) {
            return;
        }

        Text textProbe = new Text("Location");
        textProbe.setFont(Font.font(12));
        double widestLocationWidth = textProbe.getLayoutBounds().getWidth();

        for (Task task : table.getItems()) {
            if (task == null || task.getLocation() == null) {
                continue;
            }
            String locationText = task.getLocation().trim();
            if (locationText.isEmpty()) {
                continue;
            }
            textProbe.setText(locationText);
            widestLocationWidth = Math.max(widestLocationWidth, textProbe.getLayoutBounds().getWidth());
        }

        double horizontalPadding = 46;
        double targetWidth = widestLocationWidth + horizontalPadding;
        double minWidth = Math.max(100, locationColumn.getMinWidth());
        targetWidth = Math.max(minWidth, targetWidth);
        locationColumn.setPrefWidth(targetWidth);
    }

    private <S> void bindLastColumnToTableWidth(TableView<S> table, TableColumn<S, ?> lastColumn) {
        Runnable resizeLastColumn = () -> {
            if (table.getWidth() <= 0) {
                return;
            }

            double otherColumnsWidth = table.getColumns().stream()
                    .filter(column -> column != lastColumn)
                    .mapToDouble(TableColumn::getWidth)
                    .sum();

            double padding = 32;
            double targetWidth = table.getWidth() - otherColumnsWidth - padding;
            if (targetWidth < lastColumn.getMinWidth()) {
                targetWidth = lastColumn.getMinWidth();
            }

            lastColumn.setPrefWidth(targetWidth);
        };

        table.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeLastColumn.run());
        table.getColumns().addListener((javafx.collections.ListChangeListener<TableColumn<S, ?>>) change -> resizeLastColumn.run());
        Platform.runLater(resizeLastColumn);
    }

    private void applyStatusColumnStyle(TableColumn<Task, TaskStatus> statusColumn) {
        statusColumn.setStyle("-fx-alignment: CENTER;");
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TaskStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-alignment: CENTER;");
                    return;
                }

                setText(item.toString());
                String color;
                switch (item) {
                    case DRAFT -> color = isDarkMode ? "#ffcc80" : "#ef6c00";
                    case PUBLISHED -> color = isDarkMode ? "#90caf9" : "#1976d2";
                    case ACCEPTED -> color = isDarkMode ? "#80deea" : "#00838f";
                    case PERFORMING -> color = isDarkMode ? "#ffe082" : "#f9a825";
                    case COMPLETED -> color = isDarkMode ? "#a5d6a7" : "#2e7d32";
                    case CANCELLED -> color = isDarkMode ? "#ef9a9a" : "#c62828";
                    default -> color = isDarkMode ? "#e0e0e0" : "#424242";
                }

                setStyle("-fx-alignment: CENTER; -fx-font-weight: 700; -fx-text-fill: " + color + ";");
            }
        });
    }

    private void applyTableTypography(TableView<?> table) {
        table.setStyle("-fx-font-size: 12px;");
        Platform.runLater(() -> table.lookupAll(".column-header .label")
                .forEach(node -> node.setStyle("-fx-font-size: 15px; -fx-font-weight: 700;")));
    }

    private void saveAll() {
        DataStore.saveUsers(users);
        DataStore.saveTasks(tasks);
    }

    private void applyModernButtonStyle(ButtonBase button, String baseColor) {
        String normal = String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 18; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.14), 8, 0, 0, 4);", baseColor);
        String hover = String.format("-fx-background-color: derive(%s, -12%%); -fx-text-fill: white; -fx-background-radius: 14; -fx-border-radius: 14; -fx-padding: 10 18; -fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 13px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.22), 10, 0, 0, 6);", baseColor);
        button.setStyle(normal);
        button.setOnMouseEntered(e -> button.setStyle(hover));
        button.setOnMouseExited(e -> button.setStyle(normal));
        button.setFont(Font.font("Segoe UI Emoji", FontWeight.SEMI_BOLD, 13));
        button.setCursor(Cursor.HAND);
        if (button instanceof Labeled) {
            Labeled labeled = (Labeled) button;
            labeled.setWrapText(true);
            labeled.setTextAlignment(TextAlignment.CENTER);
            labeled.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private void showLoginScene() {
        stopLiveTaskSync();

        Label asciiArt = new Label(
                 " ██████╗  █████╗  ██████╗██╗  ██╗ █████╗ ██╗  ██╗ ██████╗ ██╗     ██╗ ██████╗\n"
                + " ╚══██╔══╝██╔══██╗██╔════╝██║ ██╔╝██╔══██╗██║  ██║██╔═══██╗██║     ██║██╔════╝\n"
                + "    ██║   ███████║███████╗█████╔╝ ███████║███████║██║   ██║██║     ██║██║     \n"
                + "    ██║   ██╔══██║╚════██║██╔═██╗ ██╔══██║██╔══██║██║   ██║██║     ██║██║     \n"
                + "    ██║   ██║  ██║███████║██║  ██╗██║  ██║██║  ██║╚██████╔╝███████╗██║╚██████╗\n"
                + "    ╚═╝   ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝ ╚═════╝\n"
                );
        asciiArt.setFont(Font.font("Consolas", FontWeight.NORMAL, 14));
        asciiArt.setStyle(isDarkMode ? "-fx-text-fill: #e0e7ff;" : "-fx-text-fill: #1a237e;");
        asciiArt.setWrapText(false);
        asciiArt.setTextAlignment(TextAlignment.CENTER);
        asciiArt.setAlignment(Pos.CENTER);
        asciiArt.setMaxWidth(920);
        asciiArt.setMaxHeight(220);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setTooltip(new Tooltip("Enter your registered username"));
        usernameField.setMaxWidth(340);
        usernameField.setStyle(isDarkMode ? "-fx-background-color: #353535; -fx-text-fill: white; -fx-prompt-text-fill: #9e9e9e; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: transparent;" : "-fx-background-color: white; -fx-text-fill: #212121; -fx-prompt-text-fill: #9e9e9e; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #e0e0e0;");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setTooltip(new Tooltip("Enter your password"));
        passwordField.setMaxWidth(340);
        passwordField.setStyle(isDarkMode ? "-fx-background-color: #353535; -fx-text-fill: white; -fx-prompt-text-fill: #9e9e9e; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: transparent;" : "-fx-background-color: white; -fx-text-fill: #212121; -fx-prompt-text-fill: #9e9e9e; -fx-background-radius: 14; -fx-border-radius: 14; -fx-border-color: #e0e0e0;");

        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("TASKMASTER", "TASKER");
        roleBox.setPromptText("Select role");
        roleBox.setTooltip(new Tooltip("Choose your role: TASKMASTER to create tasks, TASKER to perform tasks"));
        roleBox.setMaxWidth(340);
        String comboStyle = isDarkMode
                ? "-fx-background-color: #353535; -fx-text-fill: white; -fx-prompt-text-fill: #c7c7c7; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: transparent; -fx-background-insets: 0; -fx-border-insets: 0; -fx-cursor: hand;"
                : "-fx-background-color: white; -fx-text-fill: #212121; -fx-prompt-text-fill: #9e9e9e; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: #e0e0e0; -fx-background-insets: 0; -fx-border-insets: 0; -fx-cursor: hand;";
        String comboHover = isDarkMode
                ? "-fx-background-color: #414750; -fx-text-fill: white; -fx-prompt-text-fill: #d1d5db; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: rgba(255,255,255,0.14); -fx-background-insets: 0; -fx-border-insets: 0; -fx-cursor: hand;"
                : "-fx-background-color: #f1f5f9; -fx-text-fill: #212121; -fx-prompt-text-fill: #8f9bb3; -fx-background-radius: 0; -fx-border-radius: 0; -fx-border-color: #b0bec5; -fx-background-insets: 0; -fx-border-insets: 0; -fx-cursor: hand;";
        String comboCellStyle = isDarkMode
                ? "-fx-background-color: #353535; -fx-text-fill: white; -fx-background-radius: 0; -fx-border-radius: 0;"
                : "-fx-background-color: white; -fx-text-fill: #212121; -fx-background-radius: 0; -fx-border-radius: 0;";
        roleBox.setStyle(comboStyle);
        roleBox.setOnMouseEntered(e -> roleBox.setStyle(comboHover));
        roleBox.setOnMouseExited(e -> {
            if (!roleBox.isShowing()) {
                roleBox.setStyle(comboStyle);
            }
        });
        roleBox.focusedProperty().addListener((obs, oldVal, newVal) -> roleBox.setStyle(newVal ? comboHover : comboStyle));
        roleBox.showingProperty().addListener((obs, oldVal, newVal) -> roleBox.setStyle(newVal ? comboHover : comboStyle));
        roleBox.setButtonCell(new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(comboStyle + " " + comboCellStyle + " -fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle(comboStyle + " " + comboCellStyle + " -fx-background-color: " + (isDarkMode ? "#353535;" : "white;"));
                }
            }
        });
        roleBox.setCellFactory(listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle(comboStyle + " " + comboCellStyle + " -fx-background-color: transparent;");
                } else {
                    setText(item);
                    setStyle(comboStyle + " " + comboCellStyle + " -fx-background-color: " + (isDarkMode ? "#353535;" : "white;"));
                }
            }
        });

        Button loginBtn = new Button("🔐 Login");
        loginBtn.setTooltip(new Tooltip("Login with your credentials"));
        applyModernButtonStyle(loginBtn, "#4CAF50");
        loginBtn.setMinWidth(100);
        loginBtn.setDefaultButton(true);

        Button registerBtn = new Button("👤 Register");
        registerBtn.setTooltip(new Tooltip("Create a new account"));
        applyModernButtonStyle(registerBtn, "#2196F3");
        registerBtn.setMinWidth(110);

        ToggleButton darkModeBtn = new ToggleButton(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
        darkModeBtn.setTooltip(new Tooltip("Toggle between light and dark themes"));
        darkModeBtn.setSelected(isDarkMode);
        applyModernButtonStyle(darkModeBtn, "#9E9E9E");
        darkModeBtn.setMinWidth(110);

        Label message = new Label();
        message.setStyle(isDarkMode ? "-fx-text-fill: #ff6b6b;" : "-fx-text-fill: red;");

        loginBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            String selectedRole = roleBox.getValue();

            if (selectedRole == null) {
                message.setText("Please select a role to login.");
                return;
            }

            Optional<User> match = users.stream()
                    .filter(user -> user.getUsername().equals(u)
                            && user.getPassword().equals(p)
                            && user.getRole() == Role.valueOf(selectedRole))
                    .findFirst();

            if (match.isPresent()) {
                currentUser = match.get();
                if (currentUser.getRole() == Role.TASKMASTER) {
                    showTaskMasterScene();
                } else {
                    showTaskerScene();
                }
            } else {
                message.setText("Username, password, or selected role is incorrect.");
            }
        });

        registerBtn.setOnAction(e -> {
            String u = usernameField.getText().trim();
            String p = passwordField.getText().trim();
            String role = roleBox.getValue();

            if (u.isEmpty() || p.isEmpty() || role == null) {
                message.setText("Please fill all fields to register.");
                return;
            }

            boolean exists = users.stream().anyMatch(user -> user.getUsername().equals(u));
            if (exists) {
                message.setText("Username already exists. Choose a different one.");
                return;
            }

            users.add(new User(u, p, role));
            saveAll();
            message.setText("Account created successfully! You can now login.");
            message.setStyle(isDarkMode ? "-fx-text-fill: #4caf50;" : "-fx-text-fill: green;");
        });

        darkModeBtn.setOnAction(e -> {
            isDarkMode = darkModeBtn.isSelected();
            darkModeBtn.setText(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
            showLoginScene(); // Refresh scene with new theme
        });

        HBox buttonBox = new HBox(12, loginBtn, registerBtn, darkModeBtn);
        buttonBox.setAlignment(Pos.CENTER);

        VBox headerBox = new VBox(4, asciiArt);
        headerBox.setAlignment(Pos.CENTER);

        VBox card = new VBox(18, headerBox, usernameField, passwordField, roleBox, buttonBox, message);
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(860);
        card.setPadding(new Insets(28));
        card.setStyle(isDarkMode ? "-fx-background-color: rgba(28,28,28,0.94); -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.24), 18, 0, 0, 10);" : "-fx-background-color: rgba(255,255,255,0.95); -fx-background-radius: 24; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.12), 18, 0, 0, 10);" );

        VBox root = new VBox(card);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));
        root.setStyle(isDarkMode ? "-fx-background-color: linear-gradient(to bottom, #0f1720, #1f2937);" : "-fx-background-color: linear-gradient(to bottom, #eef5ff, #f7fbff);");

        Scene scene = createAdaptiveScene(root, 940, 620);
        applyScenePreservingWindowState(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(360), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showTaskMasterScene() {
        startLiveTaskSync();
        refreshVisibleTables();

        Label title = new Label("🚀 TaskMaster Dashboard");
        title.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 26));
        title.setStyle(isDarkMode ? "-fx-text-fill: #81d4fa;" : "-fx-text-fill: #1e3a8a;");

        Label accountLabel = new Label("👤 Logged in as " + currentUser.getUsername());
        accountLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        accountLabel.setStyle(isDarkMode ? "-fx-text-fill: #cfd8dc;" : "-fx-text-fill: #37474f;");

        draftTableView = new TableView<>(draftObservable);
        draftTableView.setPlaceholder(new Label("No draft tasks available."));
        TableColumn<Task, String> draftTitleCol = new TableColumn<>("Title");
        draftTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        draftTitleCol.setPrefWidth(240);
        draftTitleCol.setMinWidth(200);
        TableColumn<Task, String> draftLocationCol = new TableColumn<>("Location");
        draftLocationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        draftLocationCol.setPrefWidth(100);
        draftLocationCol.setMinWidth(100);
        TableColumn<Task, String> draftPriceCol = new TableColumn<>("Fee");
        draftPriceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatFee(cell.getValue().getPrice())));
        draftPriceCol.setPrefWidth(80);
        draftPriceCol.setMinWidth(80);
        draftPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        draftTableView.getColumns().addAll(draftTitleCol, draftLocationCol, draftPriceCol);
        draftTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        draftTableView.setMinWidth(0);
        draftTableView.setMaxWidth(Double.MAX_VALUE);
        bindLastColumnToTableWidth(draftTableView, draftTitleCol);
        applyTableTypography(draftTableView);
        autoSizeLocationColumn(draftTableView);

        publishedTableView = new TableView<>(FXCollections.observableArrayList(
            tasks.stream().filter(t -> t.getCreatedBy().equals(currentUser.getUsername())).toList()
        ));
        publishedTableView.setPlaceholder(new Label("No published tasks."));
        TableColumn<Task, String> pubTitleCol = new TableColumn<>("Title");
        pubTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        pubTitleCol.setPrefWidth(240);
        pubTitleCol.setMinWidth(200);
        TableColumn<Task, String> pubLocationCol = new TableColumn<>("Location");
        pubLocationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        pubLocationCol.setPrefWidth(100);
        pubLocationCol.setMinWidth(100);
        TableColumn<Task, String> pubPriceCol = new TableColumn<>("Fee");
        pubPriceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatFee(cell.getValue().getPrice())));
        pubPriceCol.setPrefWidth(80);
        pubPriceCol.setMinWidth(80);
        pubPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Task, TaskStatus> pubStatusCol = new TableColumn<>("Status");
        pubStatusCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        pubStatusCol.setPrefWidth(100);
        pubStatusCol.setMinWidth(100);
        applyStatusColumnStyle(pubStatusCol);
        publishedTableView.getColumns().addAll(pubTitleCol, pubLocationCol, pubPriceCol, pubStatusCol);
        publishedTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        publishedTableView.setMinWidth(0);
        publishedTableView.setMaxWidth(Double.MAX_VALUE);
        bindLastColumnToTableWidth(publishedTableView, pubTitleCol);
        applyTableTypography(publishedTableView);
        autoSizeLocationColumn(publishedTableView);

        draftTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                publishedTableView.getSelectionModel().clearSelection();
            }
        });
        publishedTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                draftTableView.getSelectionModel().clearSelection();
            }
        });

        Button createBtn = new Button("➕ Create Draft");
        createBtn.setTooltip(new Tooltip("Create a new task draft"));
        applyModernButtonStyle(createBtn, "#4CAF50");
        createBtn.setMinWidth(140);
        createBtn.setPrefWidth(150);

        Button editBtn = new Button("✏️ Edit ");
        editBtn.setTooltip(new Tooltip("Edit the selected draft task"));
        applyModernButtonStyle(editBtn, "#FF9800");
        editBtn.setMinWidth(140);
        editBtn.setPrefWidth(150);

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setTooltip(new Tooltip("Delete the selected draft or published task"));
        applyModernButtonStyle(deleteBtn, "#F44336");
        deleteBtn.setMinWidth(150);
        deleteBtn.setPrefWidth(160);

        Button publishBtn = new Button("📤 Publish ");
        publishBtn.setTooltip(new Tooltip("Publish the selected draft tas   k"));
        applyModernButtonStyle(publishBtn, "#2196F3");
        publishBtn.setMinWidth(150);
        publishBtn.setPrefWidth(160);

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.setTooltip(new Tooltip("Logout and return to login"));
        applyModernButtonStyle(logoutBtn, isDarkMode ? "#6f6f6f" : "#9E9E9E");
        logoutBtn.setMinWidth(90);

        ToggleButton darkModeBtn = new ToggleButton(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
        darkModeBtn.setTooltip(new Tooltip("Toggle between light and dark themes"));
        darkModeBtn.setSelected(isDarkMode);
        applyModernButtonStyle(darkModeBtn, isDarkMode ? "#6f6f6f" : "#9E9E9E");
        darkModeBtn.setMinWidth(110);

        createBtn.setOnAction(e -> {
            Task t = showTaskDialog(null);
            if (t != null) {
                t.setId(nextTaskId++);
                t.setCreatedBy(currentUser.getUsername());
                t.setStatus(TaskStatus.DRAFT);
                tasks.add(t);
                saveAll();
                refreshVisibleTables();
            }
        });

        editBtn.setOnAction(e -> {
            Task selected = draftTableView.isFocused()
                    ? draftTableView.getSelectionModel().getSelectedItem()
                    : draftTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a draft task to edit.");
                return;
            }
            int selectedTaskId = selected.getId();
            Task updated = showTaskDialog(selected);
            if (updated != null) {
                Task taskToUpdate = tasks.stream()
                        .filter(task -> task.getId() == selectedTaskId)
                        .findFirst()
                        .orElse(null);
                if (taskToUpdate == null) {
                    showAlert(Alert.AlertType.WARNING, "Task Not Found", "The selected draft no longer exists.");
                    refreshVisibleTables();
                    return;
                }

                taskToUpdate.setTitle(updated.getTitle());
                taskToUpdate.setLocation(updated.getLocation());
                taskToUpdate.setPrice(updated.getPrice());
                saveAll();
                refreshVisibleTables();
            }
        });

        deleteBtn.setOnAction(e -> {
            boolean draftFocused = draftTableView.isFocused();
            boolean publishedFocused = publishedTableView.isFocused();

            Task selected = null;
            final boolean isDraftSource;

            if (draftFocused) {
                selected = draftTableView.getSelectionModel().getSelectedItem();
                isDraftSource = true;
            } else if (publishedFocused) {
                selected = publishedTableView.getSelectionModel().getSelectedItem();
                isDraftSource = false;
            } else if (publishedTableView.getSelectionModel().getSelectedItem() != null) {
                selected = publishedTableView.getSelectionModel().getSelectedItem();
                isDraftSource = false;
            } else {
                selected = draftTableView.getSelectionModel().getSelectedItem();
                isDraftSource = true;
            }

            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a draft or published task to delete.");
                return;
            }

            final Task finalSelected = selected;
            if (!isDraftSource && selected.getStatus() == TaskStatus.COMPLETED) {
                showAlert(Alert.AlertType.WARNING, "Cannot Delete", "Completed tasks cannot be deleted.");
                return;
            }

            String message = isDraftSource
                    ? "Are you sure you want to delete this draft task?"
                    : "Are you sure you want to delete this published task? This will remove it even if a tasker has acquired it and it has not yet completed.";
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
            showAndWaitPreservingWindowState(confirm).ifPresent(response -> {
                if (response == ButtonType.YES) {
                    tasks.removeIf(task -> task.getId() == finalSelected.getId());
                    saveAll();
                    refreshVisibleTables();
                }
            });
        });

        publishBtn.setOnAction(e -> {
            Task selected = draftTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a draft task to publish.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Publish this task? It will be visible to taskers.", ButtonType.YES, ButtonType.NO);
            showAndWaitPreservingWindowState(confirm).ifPresent(response -> {
                if (response == ButtonType.YES) {
                    selected.setStatus(TaskStatus.PUBLISHED);
                    saveAll();
                    refreshVisibleTables();
                }
            });
        });

        logoutBtn.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        darkModeBtn.setOnAction(e -> {
            isDarkMode = darkModeBtn.isSelected();
            darkModeBtn.setText(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
            showTaskMasterScene(); // Refresh scene with new theme
        });

        Label draftLabel = new Label("📝 Draft Tasks");
        draftLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        draftLabel.setStyle(isDarkMode ? "-fx-text-fill: #ffcc80;" : "-fx-text-fill: #fb8c00;");

        Label publishedLabel = new Label("📋 All My Tasks");
        publishedLabel.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        publishedLabel.setStyle(isDarkMode ? "-fx-text-fill: #80e3ff;" : "-fx-text-fill: #1e88e5;");

        FlowPane actionButtons = new FlowPane(10, 10, createBtn, editBtn, deleteBtn, publishBtn);
        actionButtons.setAlignment(Pos.CENTER);
        actionButtons.setPrefWrapLength(520);
        actionButtons.setMaxWidth(Double.MAX_VALUE);

        VBox left = new VBox(10, draftLabel, draftTableView, actionButtons);
        left.setPadding(new Insets(16));
        left.setStyle(isDarkMode ? "-fx-background-color: #1f2937; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;" : "-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(0,0,0,0.08); -fx-border-width: 1;");
        left.setMinWidth(0);
        left.setMaxWidth(Double.MAX_VALUE);

        VBox right = new VBox(10, publishedLabel, publishedTableView);
        right.setPadding(new Insets(16));
        right.setStyle(isDarkMode ? "-fx-background-color: #1f2937; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(255,255,255,0.08); -fx-border-width: 1;" : "-fx-background-color: #ffffff; -fx-background-radius: 18; -fx-border-radius: 18; -fx-border-color: rgba(0,0,0,0.08); -fx-border-width: 1;");
        right.setMinWidth(0);
        right.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox content = new HBox(15, left, right);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(Double.MAX_VALUE);

        HBox bottom = new HBox(10, logoutBtn, darkModeBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(10));

        VBox root = new VBox(16, title, accountLabel, content, bottom);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setStyle(isDarkMode ? "-fx-background-color: linear-gradient(to bottom, #0f1720, #111827);" : "-fx-background-color: linear-gradient(to bottom, #f8fafc, #eff6ff);");

        Scene scene = createAdaptiveScene(root, 1000, 600);
        applyScenePreservingWindowState(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(330), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private void showTaskerScene() {
        startLiveTaskSync();
        refreshVisibleTables();

        Label title = new Label("⚡ Tasker Dashboard");
        title.setFont(Font.font("Inter", FontWeight.EXTRA_BOLD, 24));
        title.setStyle(isDarkMode ? "-fx-text-fill: #38bdf8;" : "-fx-text-fill: #0f4c81;");

        Label accountLabel = new Label("👤 Logged in as " + currentUser.getUsername());
        accountLabel.setFont(Font.font("Inter", FontWeight.NORMAL, 14));
        accountLabel.setStyle(isDarkMode ? "-fx-text-fill: #cbd5e1;" : "-fx-text-fill: #334155;");

        availableTableView = new TableView<>(taskObservable);
        availableTableView.setPlaceholder(new Label("No available tasks."));
        TableColumn<Task, String> availTitleCol = new TableColumn<>("Title");
        availTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        availTitleCol.setPrefWidth(280);
        availTitleCol.setMinWidth(220);
        TableColumn<Task, String> availLocationCol = new TableColumn<>("Location");
        availLocationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        availLocationCol.setPrefWidth(150);
        availLocationCol.setMinWidth(150);
        TableColumn<Task, String> availPriceCol = new TableColumn<>("Fee");
        availPriceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatFee(cell.getValue().getPrice())));
        availPriceCol.setPrefWidth(80);
        availPriceCol.setMinWidth(80);
        availPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Task, String> availCreatorCol = new TableColumn<>("Created By");
        availCreatorCol.setCellValueFactory(new PropertyValueFactory<>("createdBy"));
        availCreatorCol.setPrefWidth(100);
        availCreatorCol.setMinWidth(100);
        availableTableView.getColumns().addAll(availTitleCol, availLocationCol, availPriceCol, availCreatorCol);
        availableTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        bindLastColumnToTableWidth(availableTableView, availTitleCol);
        applyTableTypography(availableTableView);
        autoSizeLocationColumn(availableTableView);

        myBookingTableView = new TableView<>(myBookingObservable);
        myBookingTableView.setPlaceholder(new Label("No booked tasks."));
        TableColumn<Task, String> myTitleCol = new TableColumn<>("Title");
        myTitleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        myTitleCol.setPrefWidth(280);
        myTitleCol.setMinWidth(220);
        TableColumn<Task, String> myLocationCol = new TableColumn<>("Location");
        myLocationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        myLocationCol.setPrefWidth(150);
        myLocationCol.setMinWidth(150);
        TableColumn<Task, String> myPriceCol = new TableColumn<>("Fee");
        myPriceCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(formatFee(cell.getValue().getPrice())));
        myPriceCol.setPrefWidth(80);
        myPriceCol.setMinWidth(80);
        myPriceCol.setStyle("-fx-alignment: CENTER-RIGHT;");
        TableColumn<Task, TaskStatus> myStatusCol = new TableColumn<>("Status");
        myStatusCol.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getStatus()));
        myStatusCol.setPrefWidth(100);
        myStatusCol.setMinWidth(100);
        applyStatusColumnStyle(myStatusCol);
        myBookingTableView.getColumns().addAll(myTitleCol, myLocationCol, myPriceCol, myStatusCol);
        myBookingTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        bindLastColumnToTableWidth(myBookingTableView, myTitleCol);
        applyTableTypography(myBookingTableView);
        autoSizeLocationColumn(myBookingTableView);

        Runnable refreshTaskerTables = () -> {
            refreshVisibleTables();
        };

        Button bookBtn = new Button("📝 Book Selected Task");
        bookBtn.setTooltip(new Tooltip("Book the selected available task"));
        applyModernButtonStyle(bookBtn, "#4CAF50");
        bookBtn.setMinWidth(140);

        Button startBtn = new Button("▶️ Start Task");
        startBtn.setTooltip(new Tooltip("Start working on the selected booked task"));
        applyModernButtonStyle(startBtn, "#FF9800");
        startBtn.setMinWidth(100);

        Button completeBtn = new Button("✅ Complete Task");
        completeBtn.setTooltip(new Tooltip("Mark the selected task as completed"));
        applyModernButtonStyle(completeBtn, "#2196F3");
        completeBtn.setMinWidth(120);

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.setTooltip(new Tooltip("Logout and return to login"));
        applyModernButtonStyle(logoutBtn, isDarkMode ? "#6f6f6f" : "#9E9E9E");
        logoutBtn.setMinWidth(90);

        ToggleButton darkModeBtn = new ToggleButton(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
        darkModeBtn.setTooltip(new Tooltip("Toggle between light and dark themes"));
        darkModeBtn.setSelected(isDarkMode);
        applyModernButtonStyle(darkModeBtn, isDarkMode ? "#6f6f6f" : "#9E9E9E");
        darkModeBtn.setMinWidth(110);

        bookBtn.setOnAction(e -> {
            Task selected = availableTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an available task to book.");
                return;
            }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Book this task?", ButtonType.YES, ButtonType.NO);
            showAndWaitPreservingWindowState(confirm).ifPresent(response -> {
                if (response == ButtonType.YES) {
                    selected.setBookedBy(currentUser.getUsername());
                    selected.setStatus(TaskStatus.ACCEPTED);
                    saveAll();
                    refreshTaskerTables.run();
                }
            });
        });

        startBtn.setOnAction(e -> {
            Task selected = myBookingTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a booked task to start.");
                return;
            }
            if (selected.getStatus() != TaskStatus.ACCEPTED) {
                showAlert(Alert.AlertType.WARNING, "Invalid Action", "Only accepted tasks can be started.");
                return;
            }
            selected.setStatus(TaskStatus.PERFORMING);
            saveAll();
            refreshTaskerTables.run();
        });

        completeBtn.setOnAction(e -> {
            Task selected = myBookingTableView.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a performing task to complete.");
                return;
            }
            if (selected.getStatus() != TaskStatus.PERFORMING) {
                showAlert(Alert.AlertType.WARNING, "Invalid Action", "Only performing tasks can be completed.");
                return;
            }
            selected.setStatus(TaskStatus.COMPLETED);
            saveAll();
            refreshTaskerTables.run();
        });

        logoutBtn.setOnAction(e -> {
            currentUser = null;
            showLoginScene();
        });

        darkModeBtn.setOnAction(e -> {
            isDarkMode = darkModeBtn.isSelected();
            darkModeBtn.setText(isDarkMode ? "☀ Light Mode" : "🌙 Dark Mode");
            showTaskerScene(); // Refresh scene with new theme
        });

        Label availableLabel = new Label("🔍 Available Tasks");
        availableLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        availableLabel.setStyle(isDarkMode ? "-fx-text-fill: #4fc3f7;" : "-fx-text-fill: #2196F3;");

        Label myLabel = new Label("📅 My Bookings");
        myLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        myLabel.setStyle(isDarkMode ? "-fx-text-fill: #ff8a65;" : "-fx-text-fill: #FF5722;");

        VBox left = new VBox(12, availableLabel, availableTableView, bookBtn);
        left.setPadding(new Insets(18));
        left.setStyle(isDarkMode ? "-fx-background-color: rgba(15, 23, 42, 0.8); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1;" : "-fx-background-color: rgba(255, 255, 255, 0.92); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(15,23,42,0.08); -fx-border-width: 1;");
        left.setMaxWidth(Double.MAX_VALUE);

        HBox taskButtons = new HBox(10, startBtn, completeBtn);
        taskButtons.setAlignment(Pos.CENTER);

        VBox right = new VBox(12, myLabel, myBookingTableView, taskButtons);
        right.setPadding(new Insets(18));
        right.setStyle(isDarkMode ? "-fx-background-color: rgba(15, 23, 42, 0.8); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(255,255,255,0.12); -fx-border-width: 1;" : "-fx-background-color: rgba(255, 255, 255, 0.92); -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: rgba(15,23,42,0.08); -fx-border-width: 1;");
        right.setMaxWidth(Double.MAX_VALUE);

        HBox content = new HBox(15, left, right);
        content.setAlignment(Pos.CENTER);
        content.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);

        HBox bottom = new HBox(10, logoutBtn, darkModeBtn);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(12));

        VBox root = new VBox(14, title, accountLabel, content, bottom);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(25));
        root.setStyle(isDarkMode ? "-fx-background-color: linear-gradient(to bottom, #020617, #09101c);" : "-fx-background-color: linear-gradient(to bottom, #eef2ff, #f8fafc);");

        Scene scene = createAdaptiveScene(root, 1000, 600);
        applyScenePreservingWindowState(scene);

        FadeTransition fade = new FadeTransition(Duration.millis(330), root);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }

    private Task showTaskDialog(Task existing) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Create New Task" : "Edit Task");
        dialog.setHeaderText(existing == null ? "Enter task details" : "Modify task details");

        TextField titleField = new TextField(existing == null ? "" : existing.getTitle());
        titleField.setPromptText("Enter task title");
        titleField.setTooltip(new Tooltip("Brief description of the task"));

        TextField locationField = new TextField(existing == null ? "" : existing.getLocation());
        locationField.setPromptText("Enter location");
        locationField.setTooltip(new Tooltip("Where the task needs to be performed"));

        TextField priceField = new TextField(existing == null ? "" : String.format(java.util.Locale.US, "%.2f", existing.getPrice()));
        priceField.setPromptText("Enter fee in dollars (e.g., 15.00)");
        priceField.setTooltip(new Tooltip("Fee amount for completing the task"));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Location:"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Fee:"), 0, 2);
        grid.add(priceField, 1, 2);

        dialog.getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        final Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.setDisable(true);

        // Validation: enable save only if all fields are valid
        Runnable validate = () -> {
            boolean valid = !titleField.getText().trim().isEmpty() &&
                            !locationField.getText().trim().isEmpty() &&
                            isValidPrice(priceField.getText().trim());
            saveButton.setDisable(!valid);
        };

        titleField.textProperty().addListener((obs, old, newVal) -> validate.run());
        locationField.textProperty().addListener((obs, old, newVal) -> validate.run());
        priceField.textProperty().addListener((obs, old, newVal) -> validate.run());

        validate.run(); // Initial validation

        dialog.setResultConverter(btn -> {
            if (btn == saveType) {
                try {
                    String title = titleField.getText().trim();
                    String location = locationField.getText().trim();
                    double price = Double.parseDouble(priceField.getText().trim());

                    if (title.isEmpty() || location.isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Invalid Input", "Title and location cannot be empty.");
                        return null;
                    }

                    return new Task(
                            existing == null ? 0 : existing.getId(),
                            title,
                            location,
                            price,
                            currentUser.getUsername(),
                            existing == null ? TaskStatus.DRAFT : existing.getStatus()
                    );
                } catch (NumberFormatException ex) {
                    showAlert(Alert.AlertType.ERROR, "Invalid Fee", "Fee must be a valid number.");
                    return null;
                }
            }
            return null;
        });

        return showAndWaitPreservingWindowState(dialog).orElse(null);
    }

    private boolean isValidPrice(String priceStr) {
        try {
            double price = Double.parseDouble(priceStr.trim());
            return price >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String formatFee(double fee) {
        return String.format(java.util.Locale.US, "%.2f $", fee);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        showAndWaitPreservingWindowState(alert);
    }

    private Scene createAdaptiveScene(Parent root, double defaultWidth, double defaultHeight) {
        if (primaryStage != null && primaryStage.getScene() != null
                && (primaryStage.isMaximized() || primaryStage.isFullScreen())) {
            return new Scene(root, primaryStage.getScene().getWidth(), primaryStage.getScene().getHeight());
        }
        return new Scene(root, defaultWidth, defaultHeight);
    }

    private void applyScenePreservingWindowState(Scene scene) {
        boolean wasFullScreen = pendingRestoreFullScreen || (primaryStage != null && primaryStage.isFullScreen());
        boolean wasMaximized = pendingRestoreMaximized || (primaryStage != null && primaryStage.isMaximized());
        pendingRestoreFullScreen = false;
        pendingRestoreMaximized = false;
        primaryStage.setScene(scene);
        if (wasFullScreen && !primaryStage.isFullScreen()) {
            primaryStage.setFullScreen(true);
        } else if (!wasFullScreen && wasMaximized && !primaryStage.isMaximized()) {
            primaryStage.setMaximized(true);
        }
    }

    private <T> Optional<T> showAndWaitPreservingWindowState(Dialog<T> dialog) {
        if (primaryStage != null && dialog.getOwner() == null) {
            dialog.initOwner(primaryStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }
        boolean resumeLiveSyncAfterDialog = liveSyncTimeline != null
                && liveSyncTimeline.getStatus() == Animation.Status.RUNNING;
        if (resumeLiveSyncAfterDialog) {
            liveSyncTimeline.pause();
        }
        boolean wasFullScreen = primaryStage != null && primaryStage.isFullScreen();
        boolean wasMaximized = primaryStage != null && primaryStage.isMaximized();
        pendingRestoreFullScreen = pendingRestoreFullScreen || wasFullScreen;
        pendingRestoreMaximized = pendingRestoreMaximized || wasMaximized;
        Optional<T> result = dialog.showAndWait();
        if (resumeLiveSyncAfterDialog && liveSyncTimeline != null) {
            liveSyncTimeline.play();
        }
        restoreWindowStateAsync(wasFullScreen, wasMaximized);
        return result;
    }

    private void restoreWindowStateAsync(boolean shouldRestoreFullScreen, boolean shouldRestoreMaximized) {
        if (primaryStage == null) {
            return;
        }
        pendingRestoreFullScreen = pendingRestoreFullScreen || shouldRestoreFullScreen;
        pendingRestoreMaximized = pendingRestoreMaximized || shouldRestoreMaximized;
        Platform.runLater(() -> {
            boolean restoreFullScreen = pendingRestoreFullScreen;
            boolean restoreMaximized = pendingRestoreMaximized;
            pendingRestoreFullScreen = false;
            pendingRestoreMaximized = false;

            Runnable applyState = () -> {
                if (restoreFullScreen && !primaryStage.isFullScreen()) {
                    primaryStage.setFullScreen(true);
                } else if (!restoreFullScreen && restoreMaximized && !primaryStage.isMaximized()) {
                    primaryStage.setMaximized(true);
                }
            };

            applyState.run();
            Platform.runLater(applyState);
        });
    }

    public static void main(String[] args) {
        launch();
    }
}