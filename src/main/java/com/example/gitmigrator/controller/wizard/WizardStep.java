package com.example.gitmigrator.controller.wizard;

import javafx.scene.Node;

/**
 * Base interface for all wizard steps.
 * Each step represents a phase in the migration workflow.
 */
public interface WizardStep {
    
    /**
     * Get the title of this wizard step.
     */
    String getStepTitle();
    
    /**
     * Get the description of this wizard step.
     */
    String getStepDescription();
    
    /**
     * Get the JavaFX node that represents the UI content for this step.
     */
    Node getStepContent();
    
    /**
     * Called when the user enters this step.
     * Use this method to initialize UI components and load data.
     */
    void onStepEnter();
    
    /**
     * Called when the user leaves this step.
     * Use this method to save data and cleanup resources.
     */
    void onStepExit();
    
    /**
     * Validate that all required data for this step is complete.
     * @return true if the step is valid and user can proceed, false otherwise
     */
    boolean validateStep();
    
    /**
     * Set navigation callbacks for this step.
     */
    void setWizardNavigation(Runnable nextCallback, Runnable previousCallback, Runnable finishCallback);
    
    /**
     * Check if this step can be skipped based on wizard state.
     */
    default boolean canSkipStep() {
        return false;
    }
    
    /**
     * Get the relative progress percentage for this step (0.0 to 1.0).
     */
    default double getStepProgress() {
        return 0.0;
    }
    
    /**
     * Called when the wizard is being reset.
     */
    default void onWizardReset() {
        // Override if cleanup is needed
    }
}