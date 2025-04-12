package de.dfg.oc.otc.aid.algorithms;

import de.dfg.oc.otc.aid.AIDException;
import de.dfg.oc.otc.aid.algorithms.apid.APIDAlgorithm;
import de.dfg.oc.otc.aid.algorithms.apid.APIDAutoIncidentDetection;
import de.dfg.oc.otc.aid.algorithms.california.CaliforniaAlgorithm7;
import de.dfg.oc.otc.aid.algorithms.california.CaliforniaAlgorithm8;
import de.dfg.oc.otc.aid.algorithms.california.ECAAlgorithm;
import de.dfg.oc.otc.aid.algorithms.knn.FuzzykNN;
import de.dfg.oc.otc.aid.algorithms.knn.KNNAlgorithm;
import de.dfg.oc.otc.aid.algorithms.svm.SVM;
import de.dfg.oc.otc.aid.algorithms.xcsrUrban.XCSRUrban;
import de.dfg.oc.otc.aid.combinationStrategies.CombinationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * This factory class is used for creating instances of aid algorithms and their
 * corresponding calibrators. The default algorithm can be configured in the
 * file.xml file through parameter "de.dfg.oc.otc.aid.defaultAlgorithm".
 * <p>
 * Definition of available algorithm classes. Can be extended with further
 * algorithms and calibrators
 */
public enum AIDAlgorithmFactory {
    CALIFORNIA_7("CA7", CaliforniaAlgorithm7.class),
    CALIFORNIA_8("CA8", CaliforniaAlgorithm8.class),
    ECA("ECA", ECAAlgorithm.class),
    APID("APID", APIDAlgorithm.class),
    KNN("KNN", KNNAlgorithm.class),
    FUZZY_KNN("Fuzzy KNN", FuzzykNN.class),
    LASVM("SVM", SVM.class),
    XCSRURBAN("XCSRUrban", XCSRUrban.class),
    APIDAUTOINCIDENTDETECTION("APIDAutoIncidentDetection", APIDAutoIncidentDetection.class),
    /**
     * Weighted combination of all algorithms.
     */
    COMBINATION_STRATEGY("ENSEMBLE", CombinationStrategy.class);
    /**
     * Name by which the algorithm is identified.
     *
     * @see AbstractAIDAlgorithm#getName()
     */
    private final String algorithmName;

    private final Class<? extends AbstractAIDAlgorithm> algorithmClass;

    /**
     * Private enum constructor which is called by each enum value.
     *
     * @param algorithmName  Name of the algorithm
     * @param algorithmClass Class of the algorithm
     */
    AIDAlgorithmFactory(String algorithmName, Class<? extends AbstractAIDAlgorithm> algorithmClass) {
        this.algorithmName = algorithmName;
        this.algorithmClass = algorithmClass;
    }

    /**
     * Returns a new instance of the algorithm with the given name or null if no
     * matching algorithm class is found.
     *
     * @param algorithmName Name of the requested algorithm
     * @return Instance of the requested algorithm
     */
    public static AbstractAIDAlgorithm getAlgorithm(String algorithmName) {
        for (AIDAlgorithmFactory enumEntry : values()) {
            if (enumEntry.algorithmName.toUpperCase().equals(algorithmName.toUpperCase())) {
                try {
                    return enumEntry.algorithmClass.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new AIDException("AID Algorithm " + algorithmName + " could not be started.");
                }
            }
        }
        throw new AIDException("AID Algorithm '" + algorithmName + "' was not found!");
    }

    public static List<AbstractAIDAlgorithm> getAllAlgorithms() {
        List<AbstractAIDAlgorithm> algorithms = new ArrayList<>();

        for (AIDAlgorithmFactory enumEntry : values()) {
            try {
                algorithms.add(enumEntry.algorithmClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new AIDException("AID Algorithm " + enumEntry.algorithmName + " could not be started.");
            }
        }

        return algorithms;
    }
}
