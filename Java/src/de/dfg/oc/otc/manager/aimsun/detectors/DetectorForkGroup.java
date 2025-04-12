package de.dfg.oc.otc.manager.aimsun.detectors;

import de.dfg.oc.otc.layer1.observer.monitoring.DetectorDataValue;
import de.dfg.oc.otc.manager.aimsun.Link;

import java.util.*;

/**
 * A DetectorForkGroup is a group of detectors covering a fork.
 *
 * @author Matthias Sommer
 */
public class DetectorForkGroup extends AbstractDetectorGroup implements Observer {
    /**
     * Links belonging to this detector group.
     */
    private final List<Link> associatedLinks;
    private final List<Detector> downstreamDetectors;
    /**
     * Number of incoming sections.
     */
    private final int inStreams;
    private final Map<Detector, DetectorDataValue> notifiedDetectors;
    /**
     * Number of outgoing sections.
     */
    private final int outStreams;
    private final List<Detector> upstreamDetectors;
    private int numberDetectors = 2;

    public DetectorForkGroup(final Detector upstreamDetector, final Detector downstreamDetector, final int instreams, final int outstreams,
                             final List<Link> associatedLinks) {
        super();

        this.downstreamDetectors = new ArrayList<>(2);
        this.upstreamDetectors = new ArrayList<>(2);

        this.upstreamDetectors.add(upstreamDetector);
        this.downstreamDetectors.add(downstreamDetector);

        this.inStreams = instreams;
        this.outStreams = outstreams;
        this.notifiedDetectors = new HashMap<>();

        this.associatedLinks = associatedLinks;
    }

    /**
     * Check links of two different DetectorBranchGroups and remember unknown
     * links from otherPair.
     *
     * @return list of new links
     */
    private Collection<Link> compairAssociatedLinks(final Iterable<Link> associatedLinks) {
        final Collection<Link> linksToAdd = new ArrayList<>();

        for (Link associatedLink : associatedLinks) {
            boolean different = true;

            for (Link link : this.associatedLinks) {
                if (associatedLink.isEquallyComposed(link)) {
                    different = false;
                }
            }

            if (different) {
                linksToAdd.add(associatedLink);
            }
        }

        return linksToAdd;
    }

    public final List<Detector> getDownstreamDetectors() {
        return this.downstreamDetectors;
    }

    public final int getInStreams() {
        return this.inStreams;
    }

    /**
     * Gibt die kleinste und gr�sste Entfernung zwischen 2 Detektoren dieses
     * Paares zur�ck.
     *
     * @return Die kleinste und gr�sste Entfernung zweier Detektoren in diesem
     * Paar.
     */
    public final float[] getMinMaxDetectorDistance() {
        final float[] minMaxDistances = new float[2];
        float maxDistance = 0;
        float minDistance = 1000;

        for (Detector upstream : this.upstreamDetectors) {
            for (Detector downstream : this.downstreamDetectors) {
                for (Link link : this.associatedLinks) {
                    if (link.isDetectorContained(upstream) && link.isDetectorContained(downstream)) {
                        float nextDistance = link.getDetectorDistance(upstream, downstream);
                        if (nextDistance > maxDistance) {
                            maxDistance = nextDistance;
                        }
                        if (nextDistance < minDistance && nextDistance > -1) {
                            minDistance = nextDistance;
                        }
                        break;
                    }
                }
            }
        }

        minMaxDistances[0] = minDistance;
        minMaxDistances[1] = maxDistance;

        return minMaxDistances;
    }

    public final int getNumberUncontrolledStreams() {
        return this.inStreams + this.outStreams - (this.upstreamDetectors.size() + this.downstreamDetectors.size() - 2);
    }

    public final int getOutStreams() {
        return this.outStreams;
    }

    @Override
    public final String getPairDescription() {
        String description = "";

        for (Detector detector : this.upstreamDetectors) {
            description = description.concat(detector.getId() + " + ");
        }

        description = description.concat(",");

        for (Detector detector : this.downstreamDetectors) {
            description = description.concat(detector.getId() + " + ");
        }

        return description;
    }

    public final List<Detector> getUpstreamDetectors() {
        return this.upstreamDetectors;
    }

    public final boolean isDetectorContained(final Detector detector) {
        return this.upstreamDetectors.contains(detector) || this.downstreamDetectors.contains(detector);
    }

    /**
     * Compares detectors based on id.
     *
     * @return true if detector is contained in group
     */
    private boolean isDetectorContained(final Detector detect, final boolean upstream) {
        if (upstream) {
            for (Detector detector : this.upstreamDetectors) {
                if (detector.getId() == detect.getId()) {
                    return true;
                }
            }
        } else {
            for (Detector detector : this.downstreamDetectors) {
                if (detector.getId() == detect.getId()) {
                    return true;
                }
            }
        }

        return false;
    }

    public final boolean isDetectorUpstream(final Detector detector) {
        return this.upstreamDetectors.contains(detector);
    }

    @Override
    public final boolean isEquallyComposed(final AbstractDetectorGroup pair) {
        if (pair.getType() != getType()) {
            return false;
        }

        if (this.equals(pair)) {
            return true;
        }

        final List<Detector> upStream = ((DetectorForkGroup) pair).upstreamDetectors;
        final List<Detector> downStream = ((DetectorForkGroup) pair).downstreamDetectors;

        if (upStream.size() == this.upstreamDetectors.size() && downStream.size() == this.downstreamDetectors.size()) {
            for (int i = 0; i < upStream.size(); i++) {
                if (this.upstreamDetectors.get(i).getId() != upStream.get(i).getId()) {
                    return false;
                }
            }

            for (int i = 0; i < downStream.size(); i++) {
                if (this.downstreamDetectors.get(i).getId() != downStream.get(i).getId()) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Combines this and another DetectorBranchGroup. Add new links and new
     * detectors from the other pair to myself.
     *
     * @param pair , das DividedPair mit dem dieses zusammengef�hrt werden
     *             soll.
     */
    public final void mergeWithPair(final DetectorForkGroup pair) {
        final Collection<Link> linksToAdd = new ArrayList<>();

        pair.upstreamDetectors.stream().filter(detector -> !isDetectorContained(detector, true)).forEach(detector -> {
            this.upstreamDetectors.add(detector);
            this.numberDetectors++;
            linksToAdd.addAll(compairAssociatedLinks(pair.associatedLinks));
        });

        pair.downstreamDetectors.stream().filter(detector -> !isDetectorContained(detector, false)).forEach(detector -> {
            this.downstreamDetectors.add(detector);
            this.numberDetectors++;
            linksToAdd.addAll(compairAssociatedLinks(pair.associatedLinks));
        });

        this.associatedLinks.addAll(linksToAdd);
    }

    @Override
    public final void registerAsObserver() {
        for (Detector detector : this.upstreamDetectors) {
            detector.addObserver(this);
        }
        for (Detector detector : this.downstreamDetectors) {
            detector.addObserver(this);
        }
    }

    @Override
    public final void update(final Observable o, final Object arg) {
        /*
         * Ansatz: Wie bei SimplePair, nur f�r alle Detektoren. Das heisst, es
		 * wird der Durchschnitt �ber alle vorhandenen Detektoren gebildet.
		 * Dies ist nicht immer sinnvoll, wenn z.B. nur eine Richtung
		 * interessiert, kann aber in dieser Basisform von anderen Algorithmen
		 * genutzt werden. ECA macht hier eine spezielle Behandlung, braucht
		 * diesen Durchschnittswert aber als Anhaltspunkt f�r die Existenz
		 * dieses Paares.
		 */
        final Detector detector = (Detector) o;
        final DetectorDataValue value = (DetectorDataValue) arg;

        if (!this.notifiedDetectors.keySet().contains(detector)) {
            this.notifiedDetectors.put(detector, value);
        }

        if (this.notifiedDetectors.size() == this.numberDetectors) {
            // Werte aggregieren und Observer benachrichtigen
            final int numberFeatures = value.getValues().length;
            final float[] aggregate = new float[numberFeatures];

            for (int i = 0; i < aggregate.length; i++) {
                for (Map.Entry<Detector, DetectorDataValue> detectorDetectorDataValueEntry : this.notifiedDetectors.entrySet()) {
                    aggregate[i] += this.notifiedDetectors.get(detectorDetectorDataValueEntry.getKey()).getValues()[i];
                }
                aggregate[i] /= (float) this.numberDetectors;
            }

            final DetectorDataValue aggregatedValue = new DetectorDataValue(value.getTime(), aggregate);

            setChanged();
            notifyObservers(aggregatedValue);

            // Neuer Zeitschritt
            this.notifiedDetectors.clear();
        }
    }
}
