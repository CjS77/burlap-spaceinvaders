package za.co.nimbus.game.heuristics;

import burlap.behavior.singleagent.vfa.*;
import burlap.debugtools.DPrint;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility class to read and write VFA weights to and from the disk
 */
public class VFAFile {
    public static final int DEBUG_CODE = 770618018;

    /**
     * Load the VFA weghts from a file in binary format
     * @param vfa the {@link burlap.behavior.singleagent.vfa.ValueFunctionApproximation} object to assign the weights to
     * @param filename the filename of the VFA weight file (binary format)
     * @return whether the file loaded successfully or not
     */
    public static boolean loadFromFile(ValueFunctionApproximation vfa, String filename) {
        DataInputStream fis;
        try {
            fis = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
        } catch (FileNotFoundException e) {
            return false;
        }
        try {
            vfa.resetWeights();
            int len = fis.readInt();
            for (int i=0; i<len; i++) {
                int id = fis.readInt();
                double val = fis.readDouble();
                vfa.setWeight(id, val);
            }
            fis.close();
            DPrint.cl(DEBUG_CODE, "Read in VFA from " + filename);
            return true;
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Extracts the StateFeature IDs from the Feature database
     */
    private static List<Integer> getWeights(Domain d, State s, FeatureDatabase fd) {
        List<Action> actions = d.getActions();
        List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(actions, s);
        List<ActionFeaturesQuery> afs = fd.getActionFeaturesSets(s, gas);
        List<Integer> result = new ArrayList<>();
        for (ActionFeaturesQuery af : afs) {
            result.addAll(af.features.stream().map(feature -> feature.id).collect(Collectors.toList()));
        }
        return result;
    }
    /**
     * Saves the VFA weights to disk in binary format
     * @param vfa the {@link burlap.behavior.singleagent.vfa.ValueFunctionApproximation} instance who's weights will be persisted
     * @param filename the filename to save the weights to
     * @param s a representative state (Just used to get the grounded actions list)
     * @param d the problem domain (provides the action list)
     * @param fd the feature database (provides all the StateFeature ids)
     * @return true if file was successfully saved, false if there was an error
     */
    public static boolean saveVFAToFile(ValueFunctionApproximation vfa, String filename, State s, Domain d, FeatureDatabase fd) {
        try {
            DataOutputStream of = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)));
            List<Integer> ids = getWeights(d, s, fd);
            of.writeInt(ids.size());
            for (Integer id : ids) {
                of.writeInt(id);
                of.writeDouble(vfa.getFunctionWeight(id).weightValue());
            }
            of.close();
            System.out.printf("VFA weights saved in %s\n", filename);
            return true;
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }

    /**
     * Saves a set of VFA weights in a more human readable format.  Parameters are the same as for {@link #saveVFAToFile}
     */
    public static boolean saveVFAToASCIIFile(ValueFunctionApproximation vfa, String filename, State s, Domain d, FeatureDatabase fd) {
        try {
            BufferedWriter of = new  BufferedWriter(new FileWriter(filename));
            List<Action> actions = d.getActions();
            List<GroundedAction> gas = Action.getAllApplicableGroundedActionsFromActionList(actions, s);
            List<ActionFeaturesQuery> afs = fd.getActionFeaturesSets(s, gas);
            for (ActionFeaturesQuery af : afs) {
                of.write(af.queryAction.actionName() + "\n");
                for (StateFeature feature : af.features) {
                    FunctionWeight fw = vfa.getFunctionWeight(feature.id);
                    of.write(String.format(Locale.US, "%12d: %12.4f\n", feature.id, fw.weightValue()));
                }
            }
            of.close();
            System.out.printf("VFA weight description saved in %s\n", filename);
            return true;
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
    }
}
