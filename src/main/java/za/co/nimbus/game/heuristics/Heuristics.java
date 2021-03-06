package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.ActionFeaturesQuery;
import burlap.behavior.singleagent.vfa.FeatureDatabase;
import burlap.behavior.singleagent.vfa.StateFeature;
import burlap.behavior.singleagent.vfa.ValueFunctionApproximation;
import burlap.behavior.singleagent.vfa.common.LinearVFA;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An abstract class that allows a user to define a set of heuristics that will help a learning agent 'make sense'
 * of a given state for each action. The {@link burlap.behavior.singleagent.vfa.FeatureDatabase#getStateFeatures(State)} function
 * should be implemented, but only as a helper function to define the global state features.
 * <p/>
 * What you should then do is implement {@link burlap.behavior.singleagent.vfa.FeatureDatabase#getActionFeaturesSets(State, List)}
 * and return a vector of relevant features, or combination of features (in the map) for each action. The ids for each
 * action-feature combination should be unique if using {@link burlap.behavior.singleagent.vfa.common.LinearVFA}, because
 * that class maps feature ids to weights on a 1-1 basis.
 */
public abstract class Heuristics implements FeatureDatabase {
    public static final int DEBUG_CODE = 770618011;
    private String vfaWeightfile = null;
    private double defaultWeightValue;
    protected final HashMap<Integer, StateFeature> stateFeatureMap;
    protected final List<StateFeature> stateFeatures;

    public Heuristics() {
        this(null, 0.0);
    }

    public Heuristics(String filename, double defaultWeightValue) {
        this.vfaWeightfile = filename;
        this.defaultWeightValue = defaultWeightValue;
        stateFeatureMap = new HashMap<>();
        stateFeatures = new ArrayList<>();
    }

    protected final StateFeature higherOrderFeature(int actionCode, int... featureIDs) {
        int id = actionCode;
        double val = 1.0;
        for (int i=0; i< featureIDs.length; i++) {
            id += ipower(50, i)*featureIDs[i];
            val *= getFeature(featureIDs[i]).value;
        }
        return new StateFeature(id, val);
    }

    protected final StateFeature stateToActionFeature(int actionCode, int featureId) {
        return new StateFeature(actionCode + featureId, getFeature(featureId).value);
    }

    protected final StateFeature getFeature(int id) {
        StateFeature result = stateFeatureMap.get(id);
        if (result == null) {
            result = stateFeatures.stream().filter(
                    f -> f.id == id
            ).findFirst().get();
            stateFeatureMap.put(id, result);
        }
        return result;
    }

    protected final void addStateActionFeatures(List<StateFeature> actionFeatures, int actionOffset, int... ids) {
        for (int id : ids) {
            actionFeatures.add(stateToActionFeature(actionOffset, id));
        }
    }

    /**
     * Calulates b^x
     */
    protected static int ipower(int b, int x) {
        int result = 1;
        for (int i=0; i<x; i++) {
            result *= b;
        }
        return result;
    }

    /**
     * Creates and returns a linear VFA object over this feature database.
     * @return a linear VFA object over this feature database.
     */
    public ValueFunctionApproximation generateVFA() {
        long t0 = System.currentTimeMillis();
        LinearVFA vfa = new LinearVFA(this, defaultWeightValue);
        setInitialWeights(vfa, createInitialWeightMap());
        if (getVFAWeightFile() == null) return vfa;
        File f = new File(getVFAWeightFile());
        if (f.exists()) {
            if (!VFAFile.loadFromFile(vfa, getVFAWeightFile())) DPrint.cl(DEBUG_CODE, "Could not load VFA coefficients. Default values will be used");
            long t1 = System.currentTimeMillis();
            DPrint.cl(DEBUG_CODE, "Loading coefficients took " + (t1 - t0) + "ms\n");
        }
        return vfa;
    }

    /**
     * Sets the initial weight value for the VFA. It uses the implementation of createInitialWeightMap to provide
     * the weight values
     */
    protected void setInitialWeights(ValueFunctionApproximation vfa, Map<Integer, Double> w0) {
        if (w0 == null) return;
        for (Integer id : w0.keySet()) {
            vfa.setWeight(id, w0.get(id));
        }
    }

    /**
     * Create a set of initial weight values for the VFA. If you return an empty Map or null, then the default constant
     * value will be used (but this is dependent on the implemntaiton of the VFA employed
     * @return a Map of StateFeature ids to the initial VFA weight value approximation
     */
    protected abstract Map<Integer, Double> createInitialWeightMap();

    @Override
    public void freezeDatabaseState(boolean toggle) {}

    public String getVFAWeightFile() {
        return vfaWeightfile;
    }

    public void setVFAWeightFile(String filename) {
        this.vfaWeightfile = filename;
    }

    public double getDefaultWeightValue() {
        return defaultWeightValue;
    }

    public void setDefaultWeightValue(double defaultWeightValue) {
        this.defaultWeightValue = defaultWeightValue;
    }

}
