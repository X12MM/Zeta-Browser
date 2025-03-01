package org.example.browser1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
 import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Pair;

public class HelloController {
    @FXML private TextField urlField;
    @FXML private VBox leftSidebar;
    @FXML private HBox windowHeader;
    @FXML private TabPane mainTabPane;
    @FXML private VBox foldersContainer;
    @FXML private VBox sidebarContent;
    @FXML private HBox pinnedTabsContainer;
    
    private String currentThemeColor = "#4339ca";
    private double xOffset = 0;
    private double yOffset = 0;
    private Map<String, VBox> folders = new HashMap<>();
    private ObservableList<Tab> pinnedTabs = FXCollections.observableArrayList();
    private Map<String, List<TabInfo>> folderContents = new HashMap<>();
    private Map<String, TreeItem<String>> folderItems = new HashMap<>();
    private TreeView<String> fileTreeView;
    private VBox tabListContainer;
    
    private class TabInfo {
        String title;
        String url;
        boolean isPinned;
        Tab tab;
        
        TabInfo(Tab tab) {
            this.tab = tab;
            this.title = tab.getText();
            WebView webView = (WebView) tab.getContent();
            this.url = webView.getEngine().getLocation();
            this.isPinned = pinnedTabs.contains(tab);
        }
    }

    @FXML
    public void initialize() {
        initializeSidebar();
        setupWindowDragging();
        setupTabContextMenus();
        setupUrlField();
        
        // Hide the tab header area
        mainTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Node tabHeader = mainTabPane.lookup(".tab-header-area");
        if (tabHeader != null) {
            tabHeader.setVisible(false);
            tabHeader.setManaged(false);
        }
        
        // Add listener for tab changes
        setupTabListListener();
    }

    private void initializeSidebar() {
        // Create main container for tabs
        tabListContainer = new VBox(5);
        tabListContainer.getStyleClass().add("tab-list");
        
        // Create "My Tabs" header
        Label headerLabel = new Label("My Tabs");
        headerLabel.getStyleClass().add("sidebar-header");
        
        // Create Folders section
        VBox foldersSection = new VBox(5);
        foldersSection.getStyleClass().add("folders-section");
        
        // Add New Folder button
        Button newFolderBtn = new Button("+ New Folder");
        newFolderBtn.getStyleClass().add("new-folder-button");
        newFolderBtn.setMaxWidth(Double.MAX_VALUE);
        newFolderBtn.setOnAction(e -> createNewFolder());

        // Add New Tab button
        Button newTabBtn = new Button("+ New Tab");
        newTabBtn.getStyleClass().add("new-tab-button");
        newTabBtn.setMaxWidth(Double.MAX_VALUE);
        newTabBtn.setOnAction(e -> createNewTab());

        // Setup the sidebar content
        sidebarContent.getChildren().addAll(headerLabel, foldersSection, tabListContainer, newFolderBtn, newTabBtn);
        VBox.setVgrow(tabListContainer, Priority.ALWAYS);

        // Setup drag and drop
        setupTabDragging();
    }

    private void setupTabDragging() {
        tabListContainer.setOnDragDetected(event -> {
            Node node = event.getPickResult().getIntersectedNode();
            if (node instanceof HBox) {
                HBox tabEntry = (HBox) node;
                Label tabLabel = (Label) tabEntry.getChildren().get(0);
                Tab tab = findTabByTitle(tabLabel.getText());
                if (tab != null) {
                    Dragboard db = tabEntry.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(tab.getText());
                    db.setContent(content);
                    event.consume();
                }
            }
        });

        foldersContainer.setOnDragOver(event -> {
            if (event.getGestureSource() != foldersContainer && 
                event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        foldersContainer.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                Tab tab = findTabByTitle(db.getString());
                if (tab != null) {
                    Node node = event.getPickResult().getIntersectedNode();
                    if (node instanceof VBox) {
                        VBox folderBox = (VBox) node;
                        String folderName = ((Label)((HBox)folderBox.getChildren().get(0)).getChildren().get(1)).getText();
                        addTabToFolder(tab, folderName);
                        success = true;
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void updateSelectedTabStyle(Tab selectedTab) {
        for (Node node : tabListContainer.getChildren()) {
            if (node instanceof HBox) {
                HBox tabEntry = (HBox) node;
                Label tabLabel = (Label) tabEntry.getChildren().get(0);
                if (tabLabel.getText().equals(selectedTab.getText())) {
                    tabEntry.getStyleClass().add("selected");
                } else {
                    tabEntry.getStyleClass().remove("selected");
                }
            }
        }
    }

    private void setupTabListListener() {
        mainTabPane.getTabs().addListener((ListChangeListener<Tab>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Tab tab : change.getAddedSubList()) {
                        createSidebarTabEntry(tab);
                    }
                }
                if (change.wasRemoved()) {
                    for (Tab tab : change.getRemoved()) {
                        removeSidebarTabEntry(tab);
                    }
                }
            }
        });
    }

    private void createSidebarTabEntry(Tab tab) {
        // Remove existing entry if it exists
        removeSidebarTabEntry(tab);
        
        HBox tabEntry = new HBox(5);
        tabEntry.getStyleClass().add("sidebar-tab-entry");
        
        Label tabLabel = new Label(tab.getText());
        tabLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tabLabel, Priority.ALWAYS);
        
        tabEntry.getChildren().add(tabLabel);
        tabListContainer.getChildren().add(tabEntry);
        
        // Update label when tab title changes
        tab.textProperty().addListener((obs, old, newTitle) -> tabLabel.setText(newTitle));
        
        // Click to select tab
        tabEntry.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                mainTabPane.getSelectionModel().select(tab);
                updateSelectedTabStyle(tab);
            }
        });
        
        // Update style if this is the selected tab
        if (mainTabPane.getSelectionModel().getSelectedItem() == tab) {
            tabEntry.getStyleClass().add("selected");
        }
    }

    private void removeSidebarTabEntry(Tab tab) {
        tabListContainer.getChildren().removeIf(node -> {
            if (node instanceof HBox) {
                HBox tabEntry = (HBox) node;
                Label tabLabel = (Label) tabEntry.getChildren().get(0);
                return tabLabel.getText().equals(tab.getText());
            }
            return false;
        });
    }

    private int findTabIndexByTitle(String title) {
        for (int i = 0; i < mainTabPane.getTabs().size(); i++) {
            if (mainTabPane.getTabs().get(i).getText().equals(title)) {
                return i;
            }
        }
        return -1;
    }

    private void reorderTabs(int sourceIndex, int targetIndex) {
        Tab tab = mainTabPane.getTabs().remove(sourceIndex);
        mainTabPane.getTabs().add(targetIndex, tab);
        
        // Reorder sidebar entries to match
        Node entry = tabListContainer.getChildren().remove(sourceIndex);
        tabListContainer.getChildren().add(targetIndex, entry);
    }

    private void addFolder(String name, String icon) {
        TreeItem<String> folderItem = new TreeItem<>(name);
        folderItem.setGraphic(createIcon(icon));
        fileTreeView.getRoot().getChildren().add(folderItem);
        folderItems.put(name, folderItem);
    }

    private Node createIcon(String type) {
        Label icon = new Label();
        icon.getStyleClass().add("folder-icon");
        
        switch (type) {
            case "📁":
                icon.setText("��");
                break;
            case "qt":
                icon.setText("Qt");
                icon.getStyleClass().add("qt-icon");
                break;
            case "cpp":
                icon.setText("C++");
                icon.getStyleClass().add("cpp-icon");
                break;
            case "🟢":
                icon.setText("🟢");
                break;
            default:
                icon.setText("📁");
        }
        
        return icon;
    }

    private void clearAllTabs() {
        mainTabPane.getTabs().clear();
        createNewTab(); // Keep at least one tab
    }

    private void setupWindowDragging() {
        windowHeader.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        windowHeader.setOnMouseDragged(event -> {
            Stage stage = (Stage) windowHeader.getScene().getWindow();
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });
    }

    @FXML
    private void createNewTab() {
        try {
            Tab tab = new Tab("New Tab");
            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();
            
            // Handle JavaScript errors
            webEngine.setOnAlert(event -> {
                showErrorAlert("JavaScript Alert", event.getData());
            });
            
            webEngine.setOnError(event -> {
                showErrorAlert("Page Load Error", event.getMessage());
            });
            
            // Load blank page
            webEngine.load("about:blank");
            
            // Update tab title when page title changes
            webEngine.titleProperty().addListener((obs, oldTitle, newTitle) -> {
                if (newTitle != null && !newTitle.isEmpty()) {
                    tab.setText(newTitle);
                }
            });
            
            tab.setContent(webView);
            mainTabPane.getTabs().add(tab);
            mainTabPane.getSelectionModel().select(tab);
            
            createSidebarTabEntry(tab);
        } catch (Exception e) {
            showErrorAlert("Tab Creation Error", "Failed to create new tab: " + e.getMessage());
        }
    }

    @FXML
    private void loadUrl() {
        Tab currentTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            WebView webView = (WebView) currentTab.getContent();
            WebEngine engine = webView.getEngine();
            String url = urlField.getText().trim();
            
            try {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    if (url.contains(".") && !url.contains(" ")) {
                        url = "https://" + url;
                    } else {
                        url = "https://www.google.com/search?q=" + url.replace(" ", "+");
                    }
                }
                engine.load(url);
            } catch (Exception e) {
                showErrorAlert("Invalid URL", "The URL you entered is not valid.");
            }
        }
    }

    // Window control methods
    @FXML private void minimizeWindow() {
        ((Stage) mainTabPane.getScene().getWindow()).setIconified(true);
    }

    @FXML private void maximizeWindow() {
        Stage stage = (Stage) mainTabPane.getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML private void closeWindow() {
        Platform.exit();
    }

    // Navigation methods
    @FXML private void goBack() {
        getCurrentEngine().executeScript("history.back()");
    }

    @FXML private void goForward() {
        getCurrentEngine().executeScript("history.forward()");
    }

    @FXML private void refresh() {
        getCurrentEngine().reload();
    }

    private WebEngine getCurrentEngine() {
        Tab currentTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            WebView webView = (WebView) currentTab.getContent();
            return webView.getEngine();
        }
        return null;
    }

    @FXML
    private void toggleSidebar() {
        leftSidebar.setVisible(!leftSidebar.isVisible());
    }

    @FXML
    private void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Enter folder name:");
        dialog.setContentText("Name:");

        dialog.showAndWait().ifPresent(folderName -> {
            if (!folderName.isEmpty() && !folders.containsKey(folderName)) {
                // Create folder container
                VBox folderBox = new VBox(5);
                folderBox.getStyleClass().add("folder-box");
                
                // Create folder header
                HBox header = new HBox(5);
                header.getStyleClass().add("folder-header");
                
                Label folderLabel = new Label(folderName);
                folderLabel.getStyleClass().add("folder-label");
                
                Button toggleButton = new Button("▼");
                toggleButton.getStyleClass().add("folder-toggle");
                toggleButton.setOnAction(e -> {
                    folderBox.getChildren().get(1).setVisible(!folderBox.getChildren().get(1).isVisible());
                    toggleButton.setText(folderBox.getChildren().get(1).isVisible() ? "▼" : "▶");
                });
                
                // Create content container
                VBox content = new VBox(5);
                content.getStyleClass().add("folder-content");
                content.setVisible(true);
                
                header.getChildren().addAll(toggleButton, folderLabel);
                folderBox.getChildren().addAll(header, content);
                
                // Add to folders map
                folders.put(folderName, folderBox);
                folderContents.put(folderName, new ArrayList<>());
                
                // Add to sidebar
                foldersContainer.getChildren().add(folderBox);
            }
        });
    }

    private void addTabToFolder(Tab tab, String folderName) {
        if (tab != null && folderName != null && folders.containsKey(folderName)) {
            // Create TabInfo for the tab
            TabInfo tabInfo = new TabInfo(tab);
            
            // Add to folder's content list
            folderContents.get(folderName).add(tabInfo);
            
            // Create visual representation in folder
            HBox tabEntry = createFolderTabEntry(tabInfo, folderName);
            ((VBox)folders.get(folderName).getChildren().get(1)).getChildren().add(tabEntry);
            
            // Remove from main tab list
            mainTabPane.getTabs().remove(tab);
            removeSidebarTabEntry(tab);
        }
    }

    private HBox createFolderTabEntry(TabInfo tabInfo, String folderName) {
        HBox entry = new HBox(5);
        entry.getStyleClass().add("folder-tab-entry");
        
        Label titleLabel = new Label(tabInfo.title);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);
        
        Button openButton = new Button("Open");
        openButton.getStyleClass().add("open-tab-button");
        openButton.setOnAction(e -> {
            mainTabPane.getTabs().add(tabInfo.tab);
            mainTabPane.getSelectionModel().select(tabInfo.tab);
            removeTabFromFolder(tabInfo, folderName);
        });
        
        Button removeButton = new Button("×");
        removeButton.getStyleClass().add("remove-tab-button");
        removeButton.setOnAction(e -> removeTabFromFolder(tabInfo, folderName));
        
        entry.getChildren().addAll(titleLabel, openButton, removeButton);
        return entry;
    }

    private void removeTabFromFolder(TabInfo tabInfo, String folderName) {
        VBox folderContent = (VBox) folders.get(folderName).getChildren().get(1);
        folderContent.getChildren().removeIf(node -> 
            node instanceof HBox && ((Label)((HBox)node).getChildren().get(0)).getText().equals(tabInfo.title)
        );
        folderContents.get(folderName).remove(tabInfo);
    }

    // Add this to your context menu creation in createNewTab method
    private void setupTabContextMenus() {
        // Add context menu to sidebar tab entries
        tabListContainer.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                Node node = event.getPickResult().getIntersectedNode();
                if (node instanceof HBox) {
                    HBox tabEntry = (HBox) node;
                    Label tabLabel = (Label) tabEntry.getChildren().get(0);
                    Tab tab = findTabByTitle(tabLabel.getText());
                    if (tab != null) {
                        ContextMenu menu = setupTabContextMenu(tab);
                        menu.show(tabEntry, event.getScreenX(), event.getScreenY());
                        event.consume();
                    }
                }
            }
        });
    }

    private ContextMenu setupTabContextMenu(Tab tab) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem pinItem = new MenuItem(pinnedTabs.contains(tab) ? "Unpin" : "Pin");
        MenuItem duplicateItem = new MenuItem("Duplicate");
        MenuItem closeItem = new MenuItem("Close");
        
        pinItem.setOnAction(e -> pinUnpinTab(tab));
        duplicateItem.setOnAction(e -> duplicateTab(tab));
        closeItem.setOnAction(e -> mainTabPane.getTabs().remove(tab));
        
        contextMenu.getItems().addAll(pinItem, duplicateItem, closeItem);
        tab.setContextMenu(contextMenu);
        
        // Add hover effect
        Node tabNode = mainTabPane.lookup(".tab-container[tab=\"" + tab.getText() + "\"]");
        if (tabNode != null) {
            tabNode.setOnMouseEntered(e -> tabNode.setStyle("-fx-background-color: #e9ecef;"));
            tabNode.setOnMouseExited(e -> {
                if (!pinnedTabs.contains(tab)) {
                    tabNode.setStyle("");
                }
            });
        }
        
        return contextMenu;
    }

    private void duplicateTab(Tab originalTab) {
        WebView originalWebView = (WebView) originalTab.getContent();
        String url = originalWebView.getEngine().getLocation();
        
        Tab newTab = new Tab(originalTab.getText());
        WebView newWebView = new WebView();
        newWebView.getEngine().load(url);
        newTab.setContent(newWebView);
        
        mainTabPane.getTabs().add(newTab);
        mainTabPane.getSelectionModel().select(newTab);
        createSidebarTabEntry(newTab);
    }

    // Shortcut methods
    @FXML private void loadGmail() { loadUrlSafely("https://gmail.com"); }
    @FXML private void loadCalendar() { loadUrlSafely("https://calendar.google.com"); }
    @FXML private void loadReddit() { loadUrlSafely("https://reddit.com"); }
    @FXML private void loadYoutube() { loadUrlSafely("https://youtube.com"); }
    @FXML private void loadUniversity() { loadUrlSafely("https://your-university-url.com"); }
    @FXML private void loadDev() { loadUrlSafely("https://dev.to"); }
    @FXML private void loadChat() { loadUrlSafely("https://chat.openai.com"); }

    // Sidebar action methods
    @FXML private void addShortcut() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Add Shortcut");
        dialog.setHeaderText("Add New Shortcut");

        ButtonType addButton = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField name = new TextField();
        name.setPromptText("Name");
        TextField url = new TextField();
        url.setPromptText("URL");

        grid.add(new Label("Name:"), 0, 0);
        grid.add(name, 1, 0);
        grid.add(new Label("URL:"), 0, 1);
        grid.add(url, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    @FXML private void openImportant() {
        showInfoAlert("Important Folder", "Opening Important folder...");
    }

    @FXML private void openLearning() {
        showInfoAlert("Learning Folder", "Opening Learning folder...");
    }

    @FXML private void clearData() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Browsing Data");
        alert.setHeaderText("Clear All Browsing Data");
        alert.setContentText("This will clear your browsing history, cookies, and cached files. Continue?");

        ButtonType clearButton = new ButtonType("Clear Data", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(clearButton, ButtonType.CANCEL);

        alert.showAndWait().ifPresent(response -> {
            if (response == clearButton) {
                getCurrentEngine().load("about:blank");
                // Clear cookies and cache
                java.net.CookieHandler.setDefault(new java.net.CookieManager());
                showInfoAlert("Data Cleared", "All browsing data has been cleared.");
            }
        });
    }

    @FXML private void openProfile() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Profile Settings");
        dialog.setHeaderText("User Profile");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        username.setPromptText("Username");
        ColorPicker themeColor = new ColorPicker(Color.web(currentThemeColor));

        grid.add(new Label("Username:"), 0, 0);
        grid.add(username, 1, 0);
        grid.add(new Label("Theme Color:"), 0, 1);
        grid.add(themeColor, 1, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.showAndWait();
    }

    @FXML private void openSettings() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Browser Settings");
        dialog.setHeaderText("Settings");

        ButtonType saveButton = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButton, ButtonType.CANCEL);

        TabPane settingsPane = new TabPane();
        settingsPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // General Settings Tab
        Tab generalTab = new Tab("General");
        VBox generalSettings = new VBox(10);
        generalSettings.setPadding(new Insets(10));
        CheckBox enableJavaScript = new CheckBox("Enable JavaScript");
        CheckBox enableImages = new CheckBox("Enable Images");
        enableJavaScript.setSelected(true);
        enableImages.setSelected(true);
        generalSettings.getChildren().addAll(enableJavaScript, enableImages);
        generalTab.setContent(generalSettings);

        // Privacy Settings Tab
        Tab privacyTab = new Tab("Privacy");
        VBox privacySettings = new VBox(10);
        privacySettings.setPadding(new Insets(10));
        CheckBox blockPopups = new CheckBox("Block Pop-ups");
        CheckBox doNotTrack = new CheckBox("Send Do Not Track request");
        blockPopups.setSelected(true);
        privacySettings.getChildren().addAll(blockPopups, doNotTrack);
        privacyTab.setContent(privacySettings);

        settingsPane.getTabs().addAll(generalTab, privacyTab);
        dialog.getDialogPane().setContent(settingsPane);
        dialog.showAndWait();
    }

    private void showInfoAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void loadUrlSafely(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        getCurrentEngine().load(url);
    }

    private void pinUnpinTab(Tab tab) {
        if (pinnedTabs.contains(tab)) {
            // Unpin the tab
            pinnedTabs.remove(tab);
            tab.setStyle("");
            
            // Move to end of unpinned tabs
            mainTabPane.getTabs().remove(tab);
            mainTabPane.getTabs().add(tab);
        } else {
            // Pin the tab
            pinnedTabs.add(tab);
            tab.setStyle("-fx-background-color: #e9ecef;");
            
            // Move to start with other pinned tabs
            mainTabPane.getTabs().remove(tab);
            mainTabPane.getTabs().add(pinnedTabs.size() - 1, tab);
        }
        updatePinnedTabsDisplay();
    }

    private void updatePinnedTabsDisplay() {
        pinnedTabsContainer.getChildren().clear();
        for (Tab tab : pinnedTabs) {
            Button tabButton = new Button(tab.getText());
            tabButton.getStyleClass().add("pinned-tab-button");
            tabButton.setOnAction(e -> mainTabPane.getSelectionModel().select(tab));
            pinnedTabsContainer.getChildren().add(tabButton);
        }
    }

    @FXML
    private void showMoveToFolderDialog() {
        Tab currentTab = mainTabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null) return;

        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Move to Folder");
        dialog.setHeaderText("Choose a folder");

        // Create folder list
        VBox content = new VBox(10);
        ComboBox<String> folderCombo = new ComboBox<>();
        folderCombo.getItems().addAll(folderContents.keySet());
        
        Button newFolderButton = new Button("New Folder");
        newFolderButton.setOnAction(e -> {
            createNewFolder();
            folderCombo.getItems().clear();
            folderCombo.getItems().addAll(folderContents.keySet());
        });

        content.getChildren().addAll(
            new Label("Select folder:"),
            folderCombo,
            newFolderButton
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return folderCombo.getValue();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(folderName -> {
            if (folderName != null) {
                addTabToFolder(currentTab, folderName);
            }
        });
    }

    private void setupUrlField() {
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.startsWith("http://") || newVal.startsWith("https://")) {
                urlField.setStyle("-fx-text-fill: #4339ca;");
            } else {
                urlField.setStyle("-fx-text-fill: #495057;");
            }
        });
    }

    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private Tab findTabByTitle(String title) {
        for (Tab tab : mainTabPane.getTabs()) {
            if (tab.getText().equals(title)) {
                return tab;
            }
        }
        return null;
    }
}