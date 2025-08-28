package com.example.gitmigrator.controller;

import com.example.gitmigrator.controller.wizard.*;
import com.example.gitmigrator.model.*;
import com.example.gitmigrator.service.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.*;

/**
 * Main wizard controller that orchestrates the entire migration process.
 * Provides a step-by-step guided experience for repository migration.
 */
public class MigrationWizardController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MigrationWizardController.class);
    
    @FXML private VBox wizardContainer;
    @FXML private HBox navigationContainer;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button finishButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label stepLabel;
    @FXML private VBox stepContainer;
    
    // Wizard steps
    private List<WizardStep> wizardSteps;
    private IntegerProperty currentStepIndex = new SimpleIntegerProperty(0);
    
    // Wizard data model - shared across all steps
    private MigrationWizardModel wizardModel;
    
    // Services
    private GitApiService gitApiService;
    private TransformationService transformationService;
    private TemplateManagementService templateService;
    private ValidationService validationService;
    private GitOperationService gitOperationService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeServices();
        initializeWizardModel();
        initializeWizardSteps();
        setupNavigation();
        showStep(0);
    }
    
    /**
     * Initialize all required services for the wizard.
     */
    private void initializeServices() {
        // Initialize services (normally injected via DI)
        gitApiService = new GitApiService();
        transformationService = new TransformationService();
        templateService = new TemplateManagementServiceImpl();
        validationService = new ValidationServiceImpl();
        gitOperationService = new GitOperationService();
    }
    
    /**
     * Initialize the wizard data model.
     */
    private void initializeWizardModel() {
        wizardModel = new MigrationWizardModel();
    }
    
    /**
     * Initialize all wizard steps.
     */
    private void initializeWizardSteps() {
        wizardSteps = Arrays.asList(
            new RepositorySelectionStep(wizardModel, gitApiService, transformationService),
            new FrameworkDetectionStep(wizardModel, transformationService),
            new MigrationTypeSelectionStep(wizardModel),
            new TemplateSelectionStep(wizardModel, templateService),
            new PreviewAndValidationStep(wizardModel, validationService),
            new ExecutionStep(wizardModel, gitOperationService, transformationService)
        );
        
        // Setup navigation between steps
        for (int i = 0; i < wizardSteps.size(); i++) {
            WizardStep step = wizardSteps.get(i);
            step.setWizardNavigation(this::goToNextStep, this::goToPreviousStep, this::finishWizard);
        }
    }
    
    /**
     * Setup navigation controls.
     */
    private void setupNavigation() {
        backButton.setOnAction(e -> goToPreviousStep());
        nextButton.setOnAction(e -> goToNextStep());
        finishButton.setOnAction(e -> finishWizard());
        cancelButton.setOnAction(e -> cancelWizard());
        
        // Bind navigation button states
        backButton.disableProperty().bind(currentStepIndex.isEqualTo(0));
        nextButton.disableProperty().bind(currentStepIndex.isEqualTo(wizardSteps.size() - 1));
        finishButton.visibleProperty().bind(currentStepIndex.isEqualTo(wizardSteps.size() - 1));
        nextButton.visibleProperty().bind(finishButton.visibleProperty().not());
        
        // Update progress
        currentStepIndex.addListener((obs, oldVal, newVal) -> updateProgress());
    }
    
    /**
     * Show a specific wizard step.
     */
    private void showStep(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= wizardSteps.size()) {
            return;
        }
        
        currentStepIndex.set(stepIndex);
        WizardStep step = wizardSteps.get(stepIndex);
        
        // Update UI
        stepLabel.setText(String.format("Step %d of %d: %s", 
            stepIndex + 1, wizardSteps.size(), step.getStepTitle()));
        
        // Load step content
        stepContainer.getChildren().clear();
        stepContainer.getChildren().add(step.getStepContent());
        
        // Activate the step
        step.onStepEnter();
        
        logger.info("Showing wizard step {}: {}", stepIndex + 1, step.getStepTitle());
    }
    
    /**
     * Go to the next wizard step.
     */
    public void goToNextStep() {
        WizardStep currentStep = wizardSteps.get(currentStepIndex.get());
        
        // Validate current step before proceeding
        if (!currentStep.validateStep()) {
            showError("Please complete all required fields before proceeding.");
            return;
        }
        
        // Execute step completion logic
        currentStep.onStepExit();
        
        // Move to next step
        int nextIndex = currentStepIndex.get() + 1;
        if (nextIndex < wizardSteps.size()) {
            showStep(nextIndex);
        }
    }
    
    /**
     * Go to the previous wizard step.
     */
    public void goToPreviousStep() {
        WizardStep currentStep = wizardSteps.get(currentStepIndex.get());
        currentStep.onStepExit();
        
        int previousIndex = currentStepIndex.get() - 1;
        if (previousIndex >= 0) {
            showStep(previousIndex);
        }
    }
    
    /**
     * Finish the wizard and execute final actions.
     */
    public void finishWizard() {
        WizardStep finalStep = wizardSteps.get(currentStepIndex.get());
        
        if (!finalStep.validateStep()) {
            showError("Cannot complete migration. Please review any validation errors.");
            return;
        }
        
        // Execute final step
        finalStep.onStepExit();
        
        // Show completion message
        Alert completionAlert = new Alert(Alert.AlertType.INFORMATION);
        completionAlert.setTitle("Migration Complete");
        completionAlert.setHeaderText("Repository Migration Completed Successfully");
        completionAlert.setContentText(String.format(
            "Successfully migrated %d repositories.\nChanges have been applied and committed to the repositories.",
            wizardModel.getSelectedRepositories().size()
        ));
        completionAlert.showAndWait();
        
        // Close wizard or reset for new migration
        resetWizard();
    }
    
    /**
     * Cancel the wizard and cleanup.
     */
    public void cancelWizard() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Cancel Migration");
        confirmAlert.setHeaderText("Are you sure you want to cancel?");
        confirmAlert.setContentText("All progress will be lost.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            resetWizard();
        }
    }
    
    /**
     * Reset the wizard to start over.
     */
    private void resetWizard() {
        wizardModel.reset();
        showStep(0);
    }
    
    /**
     * Update the progress bar and step indicators.
     */
    private void updateProgress() {
        double progress = (double) (currentStepIndex.get() + 1) / wizardSteps.size();
        progressBar.setProgress(progress);
    }
    
    /**
     * Show an error message to the user.
     */
    private void showError(String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle("Wizard Error");
        errorAlert.setHeaderText("Cannot proceed to next step");
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }
    
    /**
     * Show a warning message to the user.
     */
    private void showWarning(String message) {
        Alert warningAlert = new Alert(Alert.AlertType.WARNING);
        warningAlert.setTitle("Wizard Warning");
        warningAlert.setContentText(message);
        warningAlert.showAndWait();
    }
    
    /**
     * Get the current wizard model.
     */
    public MigrationWizardModel getWizardModel() {
        return wizardModel;
    }
}