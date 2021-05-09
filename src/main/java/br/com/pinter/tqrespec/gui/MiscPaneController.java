/*
 * Copyright (C) 2021 Emerson Pinter - All Rights Reserved
 */

package br.com.pinter.tqrespec.gui;

import br.com.pinter.tqrespec.core.MyEventHandler;
import br.com.pinter.tqrespec.core.MyTask;
import br.com.pinter.tqrespec.core.WorkerThread;
import br.com.pinter.tqrespec.save.Platform;
import br.com.pinter.tqrespec.save.player.Player;
import br.com.pinter.tqrespec.save.player.PlayerWriter;
import br.com.pinter.tqrespec.util.Util;
import com.google.inject.Inject;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;

public class MiscPaneController implements Initializable {
    private final BooleanProperty saveDisabled = new SimpleBooleanProperty();

    @FXML
    private Button copyButton;

    @FXML
    private TextField copyCharInput;

    @FXML
    private ComboBox<CopyTarget> copyTargetCombo;

    @Inject
    private Player player;

    @Inject
    private PlayerWriter playerWriter;

    public MainController mainController;

    private final BooleanProperty charNameBlankBlocked = new SimpleBooleanProperty(false);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        copyButton.setGraphic(Icon.FA_COPY.create());
        copyTargetCombo.setTooltip(Util.simpleTooltip(Util.getUIMessage("main.tooltipCopyTarget")));
        copyTargetCombo.setItems(FXCollections.observableList(Arrays.asList(CopyTarget.values())));
        copyTargetCombo.getSelectionModel().select(CopyTarget.WINDOWS);
        copyTargetCombo.setCellFactory(f -> new ListCell<>() {
            @Override
            protected void updateItem(CopyTarget copyTarget, boolean empty) {
                super.updateItem(copyTarget, empty);
                setText(empty ? "" : Util.getUIMessage("main.copyTarget." + copyTarget));
                setTooltip(Util.simpleTooltip(Util.getUIMessage("main.tooltipCopyTarget." + copyTarget)));
            }
        });
        copyTargetCombo.setButtonCell(new ListCell<>() {
            @Override
            public void updateIndex(int i) {
                super.updateIndex(i);
                CopyTarget platform = getListView().getItems().get(i);
                setText(Util.getUIMessage("main.copyTarget." + platform));
            }
        });
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public BooleanProperty saveDisabledProperty() {
        return saveDisabled;
    }

    public void setSaveDisabled(boolean saveDisabled) {
        this.saveDisabled.set(saveDisabled);
    }

    public void disableControls(boolean disable) {
        if (!disable) {
            copyButton.setDisable(copyCharInput.getText().isBlank() && charNameBlankBlocked.get());
        } else {
            copyButton.setDisable(true);
        }
        copyCharInput.setDisable(disable);
        copyTargetCombo.setDisable(disable);
    }

    public void loadCharEventHandler() {
        checkCopyTarget();
        if (charNameBlankBlocked.get()) {
            copyCharInput.setDisable(true);
        }
    }

    @FXML
    public void copyCharInputChanged() {
        if (!player.isCharacterLoaded()) {
            return;
        }

        String str = copyCharInput.getText();
        int caret = copyCharInput.getCaretPosition();
        StringBuilder newStr = new StringBuilder();

        for (char c : str.toCharArray()) {
            //all characters above 0xFF needs to have accents stripped
            if (c > 0xFF) {
                newStr.append(StringUtils.stripAccents(Character.toString(c)).toCharArray()[0]);
            } else {
                newStr.append(Character.toString(c).replaceAll("[\\\\/:*?\"<>|;]", ""));
            }
            if (newStr.length() == 14) {
                break;
            }
        }

        copyCharInput.setText(newStr.toString());
        copyCharInput.positionCaret(caret);
        if (copyCharInput.getText().isBlank()) {
            copyButton.setDisable(charNameBlankBlocked.get());
        } else {
            copyButton.setDisable(false);
        }
    }

    private void setAllControlsDisable(boolean disable) {
        mainController.setAllControlsDisable(disable);
    }

    @FXML
    public void copyChar() {
        if (mainController.gameRunningAlert()) {
            return;
        }
        String targetPlayerName;
        if (StringUtils.isBlank(copyCharInput.getText())) {
            if (charNameBlankBlocked.get()) {
                throw new IllegalStateException("character name can't be empty when target is same as origin");
            }
            targetPlayerName = player.getCharacterName();
        } else {
            targetPlayerName = copyCharInput.getText();
        }

        CopyTarget selectedTarget = copyTargetCombo.getSelectionModel().getSelectedItem();
        Platform current = player.getSaveData().getPlatform();
        Platform conversionTarget = null;
        File selectedFile = null;

        if (!selectedTarget.equals(CopyTarget.BACKUP) && !selectedTarget.getPlatform().equals(current)) {
            //needs conversion
            conversionTarget = selectedTarget.getPlatform();
        }

        if (selectedTarget.equals(CopyTarget.MOBILE) || selectedTarget.equals(CopyTarget.BACKUP)) {
            FileChooser zipChooser = new FileChooser();
            zipChooser.setTitle("title");
            zipChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("ZIP", "*.zip"));
            zipChooser.setInitialFileName(String.format("%s-%s-%s.zip",
                    targetPlayerName,
                    new SimpleDateFormat("yyyyMMdd").format(new Date()),
                    selectedTarget.name()
            ));
            selectedFile = zipChooser.showSaveDialog(copyCharInput.getScene().getWindow());

            if (selectedFile == null || selectedFile.exists()) {
                Util.showError("Error copying character", "Aborted");
                mainController.reset();
                return;
            }
        }

        Path zipPath = null;
        if (selectedFile != null) {
            zipPath = selectedFile.toPath();
        }

        setAllControlsDisable(true);

        final Path finalZipPath = zipPath;
        final Platform finalConversionTarget = conversionTarget;
        MyTask<Integer> copyCharTask = new MyTask<>() {
            @Override
            protected Integer call() {
                try {
                    //both conversionTarget and zipPath are never null at the same time
                    if (finalConversionTarget != null) {
                        playerWriter.copyCurrentSave(targetPlayerName, finalConversionTarget, finalZipPath);
                    } else if (finalZipPath != null) {
                        playerWriter.copyCurrentSave(targetPlayerName, null, finalZipPath);
                    } else {
                        playerWriter.copyCurrentSave(targetPlayerName);
                    }
                    return 2;
                } catch (FileAlreadyExistsException e) {
                    return 3;
                } catch (IOException e) {
                    return 0;
                }
            }
        };

        //noinspection Convert2Lambda
        copyCharTask.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, new MyEventHandler<>() {
            @Override
            public void handleEvent(WorkerStateEvent workerStateEvent) {
                if (copyCharTask.getValue() == 2) {
                    player.reset();
                    reset();
                    setAllControlsDisable(false);
                    mainController.addCharactersToCombo();
                    mainController.setCharacterCombo(targetPlayerName);
                } else if (copyCharTask.getValue() == 3) {
                    Util.showError("Target Directory already exists!",
                            String.format("The specified target directory already exists. Aborting the copy to character '%s'",
                                    targetPlayerName));
                } else {
                    Util.showError(Util.getUIMessage("alert.errorcopying_header"),
                            Util.getUIMessage("alert.errorcopying_content", targetPlayerName));
                }
                setAllControlsDisable(false);
            }
        });
        mainController.setCursorWaitOnTask(copyCharTask);
        new WorkerThread(copyCharTask).start();
    }

    public void copyTargetSelected() {
        if (!player.isCharacterLoaded()) {
            return;
        }
        checkCopyTarget();
    }

    private void checkCopyTarget() {
        CopyTarget copyTarget = copyTargetCombo.getSelectionModel().getSelectedItem();
        charNameBlankBlocked.set(copyTarget.getPlatform() != null && copyTarget.getPlatform().equals(player.getSaveData().getPlatform()));
        copyButton.setDisable(charNameBlankBlocked.get());
    }

    public void reset() {
        copyCharInput.clear();
        copyCharInput.setDisable(true);
        charNameBlankBlocked.set(false);
        if (copyTargetCombo != null && copyTargetCombo.getSelectionModel() != null) {
            copyTargetCombo.getSelectionModel().select(CopyTarget.WINDOWS);
        }
    }
}
