/*
 * Copyright (c) 2015 by
 * Anthony Stein, M.Sc.
 * University of Augsburg
 * Department of Computer Science
 * Chair of Organic Computing
 * All rights reserved. Distribution without approval by the copyright holder is explicitly prohibited.
 * Sources are only for non-commercial and academic use
 * in the scope of student theses and courses of the University of Augsburg.
 *
 * THE SOFTWAREPARTS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package de.dfg.oc.otc.layer1.controller.xcsic;

import de.dfg.oc.otc.config.DefaultParams;
import de.dfg.oc.otc.layer0.tlc.TrafficLightControllerParameters;
import de.dfg.oc.otc.layer1.controller.AbstractTLCSelector;
import de.dfg.oc.otc.layer1.controller.ClassifierException;
import de.dfg.oc.otc.layer1.controller.LCSConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationComponent;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.InterpolationConstants;
import de.dfg.oc.otc.layer1.controller.xcsic.interpolation.Situation;
import de.dfg.oc.otc.layer1.observer.Attribute;
import de.dfg.oc.otc.layer2.OptimisationResult;
import de.dfg.oc.otc.manager.OTCManager;
import de.dfg.oc.otc.manager.OTCNode;
import de.dfg.oc.otc.manager.aimsun.SignalGroup;
import de.dfg.oc.otc.manager.aimsun.Turning;
import de.dfg.oc.otc.region.OTCNodeSynchronized;
import forecasting.DefaultForecastParameters;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Main part of the LCS-IC. Keeps a population of classifiers and provides methods
 * to select signal plans. This LSC is based on the XCST with an added Interpolation component, which interpolates
 * a signal plan based on the given situation. This Interpolated signal plan then influences the chosen action in
 * the predicition array.
 *
 * Currently only works for signal group based situations (!turningBasedSituation)
 *
 * @author hpr
 * @author rauhdomi
 */
//Used the sources from the XCS-T as a base
public class XCSIC extends AbstractTLCSelector {
    private static final Logger log = Logger.getLogger(XCSIC.class);

    private InterpolationComponent interpolationComponent;

    /**
     * Population with all classifiers.
     */
    private final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet population = new de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet();
    private de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet actionSet = new de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet();
    /**
     * Factor for demand adjustment in the presence of queues (1.0 to switch
     * off).
     */
    private float adjustToQueueFactor = 1;
    /**
     * Constants (learning rate, ...).
     */
    private LCSConstants constants;
    /**
     * Counts the number of EA activations.
     */
    private int optimisationCounter;
    /**
     * Previously selected action (for statistics only).
     */
    private TrafficLightControllerParameters previousSelectedAction;

    /**
     * Defines whether the flow forming the classifier conditions are based on
     * turnings or signal groups.
     */
    private boolean turningBasedSituation;

    /**
     * The most recent detected situation
     */
    private Situation latestSituation;

    /**
     * The most recent executed action
     */
    private TrafficLightControllerParameters latestAction;

    /**
     * Creates an LCS, the initial population is read from a text file. The
     * parameter defines which attribute (Level of Service, queue length , ...)
     * is handled by the LCS.
     *
     * @param attribute      the attribute handled by the LCS
     * @param populationFile the file name of the text file containing the initial
     *                       population
     */
    private XCSIC(final Attribute attribute, final String populationFile) {
        setAttribute(attribute);
        this.population.setUpdateIds(true);
        loadMappingFromFile(populationFile);
    }

    /**
     * Creates an LCS with an empty population. The parameter defines which
     * attribute (Level of Service, queue length, ...) is handled by the LCS.
     *
     * @param node      the intersection served by the LCS
     * @param attribute the attribute handled by the LCS
     */
    public XCSIC(final OTCNode node, final Attribute attribute) {
        this.managedNode = node;
        setAttribute(attribute);
        this.constants = LCSConstants.getInstance();
        // Population keeps track of classifier ids
        this.population.setUpdateIds(true);

        this.turningBasedSituation = DefaultParams.TURNING_BASED_SITUATION;

        boolean useForecasts = DefaultForecastParameters.IS_FORECAST_MODULE_ACTIVE;
        if (!useForecasts) {
            this.adjustToQueueFactor = DefaultParams.L1_ADJUST_TO_QUEUE_FACTOR;
        }

        //Initialize Interpolation component
        this.interpolationComponent = new InterpolationComponent();
        this.interpolationComponent.setCorrespondingXCSIC(this);
        this.interpolationComponent.setEvaluationFilePath(XCSICConstants.OUT_FILE_PATH
                + node.getJunction().getId()
                + XCSICConstants.OUT_FILE_EXTENSION);
        this.interpolationComponent.initInterpolationComponent();
    }

    /**
     * Creates a classifier from an {@code OptResult} created by Layer 2
     * and adds it to the population.
     *
     * @param result the {@code OptimisationResult} created by Layer 2
     */
    public final void addOptimisationResult(final OptimisationResult result) {
        float prediction = result.getValue();

        // Adapt prediction in case of minimization problem
        if (getAttribute().isInverted()) {
            prediction = getAttribute().mapEvaluationForLCS(result.getValue());
        }

        float[] situation;
        if (turningBasedSituation) {
            // Situation is based on turnings
            situation = result.getSituation();
        } else {
            // Situation is based on signal groups
            situation = getSituationBySignalGroups(result.getSituation());
        }

        final Classifier cl = new Classifier(situation, result.getParameters(), prediction);
        cl.setCreatedBy(Classifier.Creator.EvoAlg);

        if(afterWarmUp()) {
            this.population.add(cl);
        } else if(!afterWarmUp() && InterpolationConstants.WARM_UP_SAMPLING_POINTS){
            //Prepopulate interpolation component while warming up
            this.interpolationComponent.addNewSamplingPointIfUseful(
                    buildLatestInterpolationSituation(result.getSituation()), cl.getAction(), 0);
        }

        appendToLog("New classifier was received: " + cl);
    }

    /**
     * Adjusts the traffic situation for queued turnings by increasing the
     * respective flows.
     *
     * @param measuredSituation the traffic situation measured in the network
     * @return the adjusted traffic situation
     */
    private float[] adjustSituationToRisingQueues(final float[] measuredSituation) {
        final int size = measuredSituation.length;
        final float[] adjustedSituation = new float[size];

        for (int i = 0; i < size; i++) {
            adjustedSituation[i] = this.adjustToQueueFactor * measuredSituation[i];
        }

        return adjustedSituation;
    }

    /**
     * Determine the desired cycle time for the given situation. Basically, this
     * method performs a normal selection step and returns the cycle time of the
     * selected TLC, but Layer 2 is not triggered and the action set is not
     * stored.
     *
     * @param situation a traffic situation
     * @return the desired cycle time for the given situation
     */
    public final int determineDesiredCycleTime(final float[] situation) throws ClassifierException {
        final TrafficLightControllerParameters dtlc = determineDesiredTLC(situation, 0);
        return new Float(dtlc.getCycleTime()).intValue();
    }

    /**
     * Determine the desired TLC for the given situation. Basically, this method
     * performs a normal selection step and returns the selected TLC, but
     * neither Layer 2 is triggered nor the action set is stored.
     *
     * @param _situation      a traffic situation
     * @param cycleConstraint a cycle constraint ({@code 0} if none)
     * @return the desired TLC for the given situation
     * @throws de.dfg.oc.otc.layer1.controller.ClassifierException
     */
    public final TrafficLightControllerParameters determineDesiredTLC(final float[] _situation,
                                                                      final int cycleConstraint) throws ClassifierException {
        float[] situation = adjustSituationToRisingQueues(_situation);
        if (!this.turningBasedSituation) {
            // Situation based on signal groups
            situation = getSituationBySignalGroups(situation);
        }

        // Is situation invalid or population empty?
        if (!isSituationOk(situation) || this.population.isEmpty()) {
            // Return defaults
            return getDefaultTLCParams();
        }

        // Build match set
        final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet matchSet = this.population.buildMatchSet(situation);
        if (matchSet.isEmpty()) {
            final de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier = this.population.selectNonMatchingClassifier(situation);
            if (classifier != null) {
                matchSet.add(classifier);
            }
        }

        // Is match set empty?
        if (matchSet.isEmpty()) {
            return getDefaultTLCParams();
            // TODO trigger L2 with situation?
            // triggerL2(situation, 0);
        }

        // Keep in mind: "as != this.actionSet"
        final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet actionSet = matchSet.buildActionSet(interpolationComponent);
        final de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier = actionSet.get(0);
        final TrafficLightControllerParameters selectedAction = classifier.getAction();

        TrafficLightControllerParameters selectedActionWithConstraint = selectedAction;
        if (selectedActionWithConstraint.getCycleTime() != cycleConstraint && cycleConstraint != 0) {
            selectedActionWithConstraint = selectedAction.adaptCycleTime(cycleConstraint);
        }

        return selectedActionWithConstraint;
    }

    /**
     * Determine the prediction value for an action defined by the action hash
     * based on a situation and a cycle constraint.
     *
     * @param actionHash
     * @param _situation
     * @param cycleConstraint
     * @return prediction
     */
    public final float determinePredictionForAction(final int actionHash, final float[] _situation,
                                                    final int cycleConstraint) {
        float[] situation = adjustSituationToRisingQueues(_situation);
        if (!turningBasedSituation) {
            // Situation based on signal groups
            situation = getSituationBySignalGroups(situation);
        }

        // Is situation invalid or population empty?
        if (!isSituationOk(situation) || population.isEmpty()) {
            return -1;
        }

        de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet matchSet = new de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet();

        try {
            matchSet = population.buildMatchSet(situation);
            if (matchSet.isEmpty()) {
                final de.dfg.oc.otc.layer1.controller.xcsic.Classifier tmpCl = population.selectNonMatchingClassifier(situation);
                if (tmpCl != null) {
                    matchSet.add(tmpCl);
                }
            }
        } catch (ClassifierException e) {
            e.printStackTrace();
        }

        if (matchSet.isEmpty()) {
            return -1;
        }

        // Keep in mind: "as != this.actionSet"
        final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet actionSet = matchSet.buildActionSet(interpolationComponent);

        // Adapt cycle time to cycle constraint
        final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet tempActionSet = updateSetToCycleTime(actionSet, cycleConstraint);

        // Check, ob vorgegebene Aktion f�r aktuelle Situation gew�hlt
        // werden kann
        if (tempActionSet.isEmpty() || !tempActionSet.getDistinctActions().contains(actionHash)) {
            return -1;
        }

        // Bestimme prediction f�r bestimmte action bei aktueller Situation
        return tempActionSet.getPredictionForAction(actionHash);
    }

    /**
     * @param lastActionHash
     * @return prediction
     */
    public final float determinePredictionForActiveAction(final int lastActionHash) {
        return actionSet.getPredictionForAction(lastActionHash);
    }

    /**
     * Updates all classifiers in the action set depending on the obtained
     * reward. Mapping of the reward (if necessary for this evaluation) is done
     * within this method.
     *
     * @param reward the obtained reward (unmapped)
     */
    public final void distributeReward(final float reward) {

        //TODO remove try-catch
        try
        {
            if (Float.isNaN(reward)) { return; }

            float mappedReward = mapRewardIfNecessary(reward);

                appendToLog("Received reward " + reward + ", distributing " + mappedReward);

                // Füge der Interpolationskomponente eine neue Stützstelle hinzu,
                // wenn der letzte Exploit ausreichend belohnend war
                if (afterWarmUp() && (this.latestAction != null) && (this.latestSituation != null)) {
                    addNewSiteToInterpolationComponent(this.latestSituation, this.latestAction, mappedReward);
                }

                // Determine numerosity sum for the current action set
                int numerositySum = 0;
                for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl : actionSet) {
                    numerositySum += cl.getNumerosity();
                }

                // Update all classifier in the action set
                for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : actionSet) {
                    classifier.updateEvaluation(mappedReward, numerositySum);
                }
                updateFitnessForActionSet(mappedReward);
        }
        catch(Throwable t)
        {
            String msg = ExceptionUtils.getStackTrace(t);
            System.out.println(msg);
        }
    }

    private float mapRewardIfNecessary(final float reward)
    {
        // Map reward (if necessary)
        if (getAttribute().isInverted()) {
            return getAttribute().mapEvaluationForLCS(reward);
        } else {
            return reward;
        }
    }

    /**
     * Die Interpolationskomponenten entscheidet, ob der letzte
     * Exploit belohnend genug war, um dessen Condition als Stützstelle hinzuzufügen
     *
     * @param situation Die Situation in der die action ausgeführt und der reward erhalten wurde
     * @param reward Die erhaltene Belohnung
     * @param action Die ausgeführte Action
     */
    private void addNewSiteToInterpolationComponent(Situation situation,
                                                    TrafficLightControllerParameters action,
                                                    float reward)
    {
        interpolationComponent.putLastReceivedEnvironmentState(situation, action, reward);
        interpolationComponent.addNewSamplingPointIfUseful(situation, action, reward);
    }

    /**
     * Checks if at least one existing classifier in the population matches the
     * situation given as parameter. If this is not the case, a new matching
     * classifier is created by activating Layer 2.
     *
     * @param situation the situation the classifier should match
     */
    public final void generateMappingForSituation(float[] situation) {
        situation = adjustSituationToRisingQueues(situation);
        float[] situationBySignalGrps = situation;

        if (!turningBasedSituation) {
            // Situation is based on turnings
            situationBySignalGrps = getSituationBySignalGroups(situation);
        }

        try {
            // Check if situation contains NaN values
            if (afterWarmUp() && isSituationOk(situation)
                    && !population.containsMatchingClassifier(situationBySignalGrps)) {
                triggerL2(situation, 0);
            }
        } catch (ClassifierException e) {
            log.warn("A ClassifierException occurred.", e);
        }
    }

    @Override
    public final String getConfig() {
        return constants.getConfig();
    }

    /**
     * Checks if a traffic situation is ok, i.e. if it does not contain
     * {@code NaN} values or consists only of {@code 0} values.
     *
     * @param situation the situation that will be checked
     * @return {@code true} iff no {@code NaN} values are contained in
     * the situation
     */
    private boolean isSituationOk(final float[] situation) {
        boolean allZeros = true;

        for (float flow : situation) {
            // Check if situation contains NaN values
            if (Float.isNaN(flow)) {
                appendToLog("Situation " + Arrays.toString(situation)
                        + " contains NaN values, using default parameters (" + getDefaultTLCParams() + ").");
                return false;
            }

            // Check if situation contains traffic
            if (flow != 0) {
                allZeros = false;
            }
        }

        if (allZeros) {
            log.warn("Network queued: [0,...,0] for node " + this.managedNode.getId());
            return false;
        }
        return true;
    }

    /**
     * Adds classifiers from a file to the population. If the original
     * population of this LCS was empty, the classifier ids are obtained from
     * the file, otherwise new ids are generated.
     *
     * @param populationFile the file name of the text file containing classifiers to be
     *                       added to the population
     * @return {@code true} iff all classifier were created correctly
     */
    public final boolean loadMappingFromFile(final String populationFile) {
        // TODO Check for duplicate ids
        boolean allOk = true;

        try {
            final FileReader fr = new FileReader(populationFile);
            final BufferedReader br = new BufferedReader(fr);

            // If original population is empty, use classifier ids from file
            final boolean popWasEmpty = population.isEmpty();
            if (popWasEmpty) {
                population.setUpdateIds(false);
            }

            int maxId = 0;
            String clString;
            while ((clString = br.readLine()) != null) {
                final String[] clParts = clString.split(" --- ");

                try {
                    // Determine classifier id, creator, and #widenings
                    final String idCreator = clParts[0];
                    final String[] idCreatorParts = idCreator.split(" ");
                    final int clId = new Integer(idCreatorParts[0]);
                    if (clId > maxId) {
                        maxId = clId;
                    }

                    de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator clCreator;
                    int numberOfWidenings;

                    if (idCreatorParts[1].equals("EvoAlg")) {
                        clCreator = de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator.EvoAlg;
                        numberOfWidenings = 0;
                    }
                    else {
                        clCreator = de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator.LCS;
                        numberOfWidenings = new Integer(idCreatorParts[1].substring(3, 6));
                    }

                    // Create the classifier condition
                    final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierCondition clCond = new de.dfg.oc.otc.layer1.controller.xcsic.ClassifierCondition(clParts[1]);

                    // Create the classifier action
                    final TrafficLightControllerParameters tlcParams = new TrafficLightControllerParameters(clParts[2]);

                    // Get the evaluation of the classifier
                    final String clEval = clParts[3];
                    final String[] evalParts = clEval.split(", ");
                    final float prediction = new Float(evalParts[0]);
                    final float initialPrediction = new Float(evalParts[1]);
                    final float predError = new Float(evalParts[2]);
                    final float fitness = new Float(evalParts[3]);
                    final float actionSetSize = new Float(evalParts[4]);
                    final int num = new Integer(evalParts[5].trim());
                    final int experience = new Integer(evalParts[6].trim());

                    // Initial situation the classifier has been optimised
                    // for
                    String initialSitStr = clParts[4];
                    initialSitStr = initialSitStr.replace('[', ' ');
                    initialSitStr = initialSitStr.replace(']', ' ');
                    final String[] initialSitStrs = initialSitStr.split(", ");
                    final float[] initialSit = new float[initialSitStrs.length];

                    for (int i = 0; i < initialSitStrs.length; i++) {
                        initialSit[i] = new Float(initialSitStrs[i]);
                    }

                    // Classifier was restored correctly, create classifier
                    final de.dfg.oc.otc.layer1.controller.xcsic.Classifier cl = new de.dfg.oc.otc.layer1.controller.xcsic.Classifier(clCond, tlcParams, prediction);

                    // Id will be ignored if the original population was not
                    // empty
                    cl.setClassifierID(clId);
                    cl.setCreatedBy(clCreator);
                    cl.setNumberOfWidenings(numberOfWidenings);
                    cl.setPredictionError(predError);
                    cl.setFitness(fitness);
                    cl.setActionSetSize(actionSetSize);
                    cl.setNumerosity(num);
                    cl.setExperience(experience);
                    cl.setInitialPrediction(initialPrediction);
                    cl.setInitialSituation(initialSit);
                    cl.setMaxFlow(tlcParams.getMaxSignalGroupFlowForDegreeOfSaturation(1.0f));

                    this.population.add(cl);
                } catch (Exception e) {
                    log.warn("Classifier read from " + populationFile + " is malformed: " + clString);
                    allOk = false;
                }
            }

            // Set update ids to true again
            if (popWasEmpty) {
                this.population.setIdCounter(maxId);
                this.population.setUpdateIds(true);
            }

            br.close();
            fr.close();
        } catch (IOException e) {
            allOk = false;
        }
        return allOk;
    }

    /**
     * Logs the selection process for the LogFileAnalyzer.
     *
     * @param situation       a traffic situation
     * @param cycleConstraint a cycle constraint
     * @param matchSet        the match set
     * @param selectedAction  the selected action
     */
    private void logSelectAction(final float[] situation, final int cycleConstraint, final de.dfg.oc.otc.layer1.controller.xcsic.ClassifierSet matchSet,
                                 final TrafficLightControllerParameters selectedAction) {
        // Log time, situation, and cycle constraint
        String minOrMax = "";
        if (getAttribute().isInverted()) {
            minOrMax = " (prediction = " + getAttribute().getMaximalValue() + " - " + getAttribute().name() + ")";
        }

        appendToLog("SimTime " + OTCManager.getInstance().getTime() + ", situation " + Arrays.toString(situation)
                + ", cycle constraint " + cycleConstraint + ", criterium " + getAttribute().name() + minOrMax);

        appendToLog("Population" + System.getProperty("line.separator") + population);
        appendToLog("Population size: " + population.size() + " Number of EA activations: " + optimisationCounter);
        appendToLog("MatchSet" + System.getProperty("line.separator") + matchSet);
        appendToLog("ActionSet" + System.getProperty("line.separator") + actionSet);

        // Update separate situation-action-log
        logSituations(situation, selectedAction);
    }

    /**
     * Logs the situation given to the LCS and the action selected by the LCS to
     * a file. The file contains in each line the current time, the situation,
     * the action and an integer indicating if the action changed. Entries in
     * each line are separated by semicolons.
     *
     * @param situation the current situation
     * @param action    the action selected by the LCS
     */
    private void logSituations(final float[] situation, final TrafficLightControllerParameters action) {
        // Were new parameters selected?
        String newAction = "?";
        if (this.previousSelectedAction != null && action != null) {
            final long previousActionHash = previousSelectedAction.hashCode();
            final long currentActionHash = action.hashCode();
            newAction = previousActionHash == currentActionHash ? ";" : "1;";
        }

        if (DefaultParams.LOG_LCS_DATA) {
            try {
                final FileOutputStream situationsLogFile = new FileOutputStream("logs/"
                        + OTCManager.getInstance().getFilenamePrefix() + "_XCST_SituationAction_"
                        + this.managedNode.getId() + ".csv", true);
                final PrintStream situationsLog = new PrintStream(situationsLogFile);

                final Formatter f = new Formatter();
                situationsLog.println(f.format("%.2f; %d; %s; %s; %.0f; %s", OTCManager.getInstance().getTime(),
                        managedNode.getId(), Arrays.toString(situation), action.toString(), action.getCycleTime(),
                        newAction));

                situationsLog.close();
                situationsLogFile.close();
                f.close();
            } catch (FileNotFoundException e) {
                log.error("A FileNotFoundException occurred.", e);
            } catch (IOException e) {
                log.error("An IOException occurred.", e);
            }
        }

        // Store the current action
        this.previousSelectedAction = action;
    }

    /**
     * Save the population of this LCS in a text file. The file name is
     * specified as parameter.
     *
     * @param filename file name for the saved population
     */
    public final void saveMappingToFile(final String filename) {
        try {
            final FileOutputStream fout = new FileOutputStream(filename);
            new PrintStream(fout).print(this.population);
            fout.close();
        } catch (IOException e) {
            log.error("Unable to write to file " + filename, e);
        }

        /** (AST) Zu Auswertungszwecken für die Masterarbeit! */
        int numDistinctActions = this.population.getDistinctActions().size();
        log.info("Number of distinct actions in population set: " + numDistinctActions);
    }

    /**
     * Selects an action that best fits the situation given as parameter and
     * exhibits the cycle time given by the {@code _cycleConstraint}
     * parameter.
     *
     * @param situation       the situation for which a traffic light controller parameter
     *                        set is needed
     * @param cycleConstraint cycle time for the returned action
     * @return a traffic light controller parameter set that fits the given
     * situation and exhibits the given cycle time constraint
     * @throws de.dfg.oc.otc.layer1.controller.ClassifierException
     */
    public final TrafficLightControllerParameters selectAction(float[] situation, final int cycleConstraint)
            throws ClassifierException {

        //TODO remove try catch
        try
        {
            float[] situationForCondition;
            situation = adjustSituationToRisingQueues(situation);
            situationForCondition = buildLatestSituation(situation);
            this.latestSituation  = buildLatestInterpolationSituation(situation);

            //Pre-populate the interpolation component with optimised actions from layer 2 during warm-up
            if(!afterWarmUp() && InterpolationConstants.WARM_UP_SAMPLING_POINTS && isSituationOk(situation)) { triggerL2(situation, cycleConstraint); };
            if (!afterWarmUp()) { return getDefaultTLCParams(); }

            this.interpolationComponent.interpolate(this.latestSituation);

            ClassifierSet matchSet = new ClassifierSet();
            TrafficLightControllerParameters action =
                    selectActionBasedOnSituation(situation, situationForCondition, matchSet, cycleConstraint);

            action = handleCycleTimeConstraint(action, cycleConstraint);

            // Update desired cycle time for synchronized nodes
            updateDesiredCycleTime(action, cycleConstraint);

            logSelectAction(situationForCondition, cycleConstraint, matchSet, action);
            this.interpolationComponent.printPerformanceMeasures();

            this.latestAction = action;

        return action;

        }catch(Throwable t)
        {
            String msg = ExceptionUtils.getStackTrace(t);
            throw new ClassifierException("error: " + msg);
        }
    }

    /**
     * Activates the classifier generation in Layer 2 for the situation and
     * cycle time constraint given as parameter. (Here, only the observing
     * {@code OTCNode} is informed that a new classifier is needed.)
     *
     * @param situation       the situation for which a new classifier is needed
     * @param cycleConstraint cycle time constraint for the new classifier
     */
    private void triggerL2(final float[] situation, final int cycleConstraint) {
        if (OTCManager.getInstance().isLayer2Present()) {
            this.managedNode.triggerL2(situation, cycleConstraint);
            appendToLog("Layer 2 activated!");
            this.optimisationCounter++;
        } else {
            appendToLog("Layer 2 activation cancelled, Layer 2 is not present!");
        }
    }

    /**
     * Updates the desired cycle time for synchronized nodes.
     *
     * @param selectedAction  the selected action defining the new desired cycle time
     * @param cycleConstraint the cycle constraint used when selecting the action
     */
    private void updateDesiredCycleTime(final TrafficLightControllerParameters selectedAction, final int cycleConstraint) {
        if (managedNode instanceof OTCNodeSynchronized && cycleConstraint == 0) {
            final int cycleTime = new Float(selectedAction.getCycleTime()).intValue();
            ((OTCNodeSynchronized) managedNode).setDesiredCycleTime(cycleTime);
        }
    }

    /**
     * Updates the fitness value for all classifiers in the action set depending
     * on the obtained reward.
     *
     * @param reward the obtained reward
     */
    private void updateFitnessForActionSet(final float reward) {
        // Calculate accuracy sum
        float accuracySum = 0;
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : actionSet) {
            accuracySum += classifier.getKappa() * classifier.getNumerosity();
        }

        // Update fitness for each classifier
        for (de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier : actionSet) {
            float fitness = classifier.getFitness();
            fitness += constants.getBeta() * (classifier.getKappa() * classifier.getNumerosity() / accuracySum - fitness);
            classifier.setFitness(fitness);
        }
    }

    /**
     * Adapts the cycle time of all classifiers in the given set to match the
     * given cycle constraint. A new set with adapted classifiers is returned.
     *
     * @param classifiers     a set of classifiers
     * @param cycleConstraint cycle time (after update)
     * @return new set with adapted classifiers
     */
    private ClassifierSet updateSetToCycleTime(final ClassifierSet classifiers, final int cycleConstraint) {
        // Adapt cycle time
        final ClassifierSet classifierSet = new ClassifierSet();
        for (Classifier classifier : classifiers) {
            Classifier clonedClassifier = classifier.clone();

            // Set adapted cycle time (should exceed the cycle time of the
            // previous classifier)
            clonedClassifier.setAction(clonedClassifier.getAction().adaptCycleTime(cycleConstraint));

            // TODO Wirklich zurücksetzen?
            // Reset error, fitness, and experience
            clonedClassifier.setPredictionError(constants.getEpsilonInit());
            clonedClassifier.setFitness(constants.getFitnessInit());
            clonedClassifier.setExperience(constants.getExperienceInit());
            clonedClassifier.setNumerosity(1);

            clonedClassifier.setCreatedBy(de.dfg.oc.otc.layer1.controller.xcsic.Classifier.Creator.LCS);

            classifierSet.add(clonedClassifier);
        }
        return classifierSet;
    }

    private Situation buildLatestInterpolationSituation(float[] situation)
    {
        if (!turningBasedSituation)
        {
            // Situation based on signal groups
            return getInterpolationSituationBySignalGroups(situation);
        }
        else { return null; }
    }

    private float[] buildLatestSituation(float[] situation)
    {
        if(!turningBasedSituation)
        {
            // Situation based on signal groups
            return getSituationBySignalGroups(situation);
        }
        else { return situation; }
    }

    private TrafficLightControllerParameters selectActionBasedOnSituation(float[] situation,
                                                                          float[] situationForCondition,
                                                                          ClassifierSet matchSet,
                                                                          int cycleConstraint)
            throws ClassifierException
    {
        TrafficLightControllerParameters action = null;
        // Is situation valid?
        if (isSituationOk(situation)) {
            // Build match set
            matchSet = this.population.buildMatchSet(situationForCondition);

            if (matchSet.isEmpty()) {
                // Activate Layer 2 if there is no matching classifier
                // TODO cycleConstraint == 0?

                // Trigger Layer 2 with mix of current and predicted situation
                triggerL2(situation, 0);

                // Select best non matching classifier
                final de.dfg.oc.otc.layer1.controller.xcsic.Classifier classifier;

                //Use Interpolation based or normal (widening) covering
                if (InterpolationConstants.INTERPOLATION_BASED_COVERING) {
                    classifier = population.selectNonMatchingClassifierByInterpolation(situationForCondition,
                            this.interpolationComponent.getInterpolatedValue(),
                            this.interpolationComponent.getTrustLevel());
                } else {
                    classifier = population.selectNonMatchingClassifier(situationForCondition);
                }

                if (classifier != null) {
                    //Add covered classifier to the rule base
                    if(InterpolationConstants.ADD_COVERED_TO_POP) { this.population.add(classifier); }
                    matchSet.add(classifier);
                }
            }

            if (matchSet.isEmpty()) {
                // return defaults
                this.actionSet.clear();
                action = getDefaultTLCParams().adaptCycleTime(cycleConstraint);
            } else {
                this.actionSet = matchSet.buildActionSet(interpolationComponent);
                action = actionSet.get(0).getAction();
            }
        } else {
            // Situation is invalid, return (adapted) defaults
            this.actionSet.clear();
            action = getDefaultTLCParams().adaptCycleTime(cycleConstraint);
        }

        return action;
    }

    private TrafficLightControllerParameters handleCycleTimeConstraint(TrafficLightControllerParameters action, final int cycleConstraint)
    {
        // Handle cycle time constraint
        if (action.getCycleTime() != cycleConstraint && cycleConstraint != 0) {
            // Debugging
            if (action.getCycleTime() > cycleConstraint) {
                log.warn(OTCManager.getInstance().getTime() + " - Node " + managedNode.getId()
                        + ": Shortening of cycle time from " + action.getCycleTime() + " to " + cycleConstraint);
            }

            this.actionSet = updateSetToCycleTime(actionSet, cycleConstraint);

            // Update population
            population.addAll(actionSet.stream().map(classifier -> classifier).collect(Collectors.toList()));

            action = actionSet.get(0).getAction();
        }
        return action;
    }

    private Situation getInterpolationSituationBySignalGroups(final float[] situationByTurnings) {

        Situation interpolationSituation = null;

        //Using a TreeMap because ordering the map by the signal group id is important for interpolation
        //In this XCS this ordering is not necessary but more consistent with XCSCIC
        TreeMap<SignalGroup, Double> flowsForSignalGroup = new TreeMap<>(new SignalGroupComparator());

        // Signalgruppen des gesteuerten Knotens
        final List<SignalGroup> signalGrps = managedNode.getJunction().getSignalGroups();

        // SectionIds für Turning-basierte Situationsbeschreibung
        final int[] sectionIdsForTurnings = managedNode.getSectionIDsForTurnings();

        // Durchlaufe alle Signalgruppen
        for (SignalGroup signalGroup : signalGrps) {
            // Signalgruppen können leer sein
            if (signalGroup.getNumberOfTurnings() > 0) {
                float signalGroupFlow = 0;

                // Durchlaufe alle Abbiegebeziehungen der Signalgruppe
                List<Turning> turnings = signalGroup.getTurnings();

                for (Turning turning : turnings) {
                    int inSectionId = turning.getInSection().getId();
                    int outSectionId = turning.getOutSection().getId();

                    // Bestimme Verkehrsfluss für die Signalgruppe
                    for (int k = 0; k < sectionIdsForTurnings.length; k += 2) {
                        if (sectionIdsForTurnings[k] == inSectionId && sectionIdsForTurnings[k + 1] == outSectionId) {
                            signalGroupFlow += situationByTurnings[k / 2];
                            // Nach dem ersten Treffer kann gestoppt werden
                            break;
                        }
                    }
                }
                flowsForSignalGroup.put(signalGroup, (double)signalGroupFlow);
            }
        }

        interpolationSituation = new Situation(flowsForSignalGroup, managedNode.getJunction().exportTurningData());

        return interpolationSituation;
    }
}
