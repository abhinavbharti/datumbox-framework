/* 
 * Copyright (C) 2013-2015 Vasilis Vryniotis <bbriniotis at datumbox.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.datumbox.framework.machinelearning.common.bases.mlmodels;

import com.datumbox.framework.machinelearning.common.bases.validation.ModelValidation;
import com.datumbox.common.dataobjects.Dataset;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConfiguration;
import com.datumbox.framework.machinelearning.common.bases.BaseTrainable;
import com.datumbox.common.persistentstorage.interfaces.DatabaseConnector;
import com.datumbox.configuration.GeneralConfiguration;
import com.datumbox.framework.machinelearning.common.bases.dataobjects.BaseModelParameters;
import com.datumbox.framework.machinelearning.common.bases.dataobjects.BaseTrainingParameters;
import com.datumbox.framework.machinelearning.common.bases.dataobjects.BaseValidationMetrics;
import com.datumbox.framework.machinelearning.common.dataobjects.MLmodelKnowledgeBase;

/**
 * Abstract Class for a Machine Learning algorithm.
 * 
 * @author Vasilis Vryniotis <bbriniotis at datumbox.com>
 * @param <MP>
 * @param <TP>
 * @param <VM>
 */
public abstract class BaseMLmodel<MP extends BaseMLmodel.ModelParameters, TP extends BaseMLmodel.TrainingParameters, VM extends BaseMLmodel.ValidationMetrics> extends BaseTrainable<MP, TP, MLmodelKnowledgeBase<MP, TP, VM>> {

    //internal variables
    private final ModelValidation<MP, TP, VM> modelValidator;

    
    /**
     * Parameters/Weights of a trained model: For example in regression you have the weights of the parameters learned.
     */
    public static abstract class ModelParameters extends BaseModelParameters {

        public ModelParameters(DatabaseConnector dbc) {
            super(dbc);
        }
            
        //here goes the parameters of the Machine Learning model
    }
    
    /**
     * Training Parameters of an algorithm: For example in regression you have the number of total regressors
     */
    public static abstract class TrainingParameters extends BaseTrainingParameters {
        //here goes public fields that are used as initial training parameters
    } 

    /**
     * Validation metrics: For example in regression you have the likelihood, the R^2 etc
     */
    public static abstract class ValidationMetrics extends BaseValidationMetrics {        
        //here goes public fields that are generated by the validation algorithm
    }
    
    
    
    /*
        IMPORTANT METHODS FOR THE FUNCTIONALITY
    */
    protected BaseMLmodel(String dbName, DatabaseConfiguration dbConf, Class<MP> mpClass, Class<TP> tpClass, Class<VM> vmClass, ModelValidation<MP, TP, VM> modelValidator) {
        super(dbName, dbConf);
        
        knowledgeBase = new MLmodelKnowledgeBase<>(this.dbName, dbConf, mpClass, tpClass, vmClass);
        this.modelValidator = modelValidator;
    } 
    
    /**
     * Performs k-fold cross validation on the dataset and returns the ValidationMetrics
     * Object.
     * 
     * @param trainingData
     * @param trainingParameters
     * @param k
     * @return  
     */
    @SuppressWarnings("unchecked")
    public VM kFoldCrossValidation(Dataset trainingData, TP trainingParameters, int k) {
        if(GeneralConfiguration.DEBUG) {
            System.out.println("kFoldCrossValidation()");
        }
        initializeTrainingConfiguration(trainingParameters);
        
        return modelValidator.kFoldCrossValidation(trainingData, k, dbName, knowledgeBase.getDbConf(), this.getClass(), knowledgeBase.getTrainingParameters());
    }
    
    
    /**
     * Calculates the predictions for the newData and stores the predictions
     * inside the object.
     * 
     * @param newData 
     */
    public void predict(Dataset newData) { 
        
        if(GeneralConfiguration.DEBUG) {
            System.out.println("predict()");
        }
        
        knowledgeBase.load();
        
        predictDataset(newData);

    }
    
    /**
     * Validate the model against the testingData and returns the validationMetrics;
     * It does not update the validationMetrics.
     * 
     * @param testingData
     * @return 
     */
     public VM validate(Dataset testingData) {  
        
        if(GeneralConfiguration.DEBUG) {
            System.out.println("test()");
        }
        
        knowledgeBase.load();

        //validate the model with the testing data and update the validationMetrics
        VM validationMetrics = validateModel(testingData);
        
        return validationMetrics;
    }
    
    
    /**
     * Updates the ValidationMetrics of the algorithm. Usually used to set the
 metrics after running a validate() or when doing K-fold cross validation.
     * 
     * @param validationMetrics 
     */
    public void setValidationMetrics(VM validationMetrics) {
        knowledgeBase.setValidationMetrics(validationMetrics);
        
        if(GeneralConfiguration.DEBUG) {
            System.out.println("Updating model");
        }
        knowledgeBase.save();
    }
    
    public VM getValidationMetrics() {
        return knowledgeBase.getValidationMetrics();
    }
    
    protected abstract VM validateModel(Dataset validationData);
    
    protected abstract void predictDataset(Dataset newData);


}
