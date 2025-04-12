package de.dfg.oc.otc.region;

import java.util.ArrayList;
import java.util.List;

/**
 * This class defines the information a node receives from the RegionalManager
 * after the centralised calculation of PSS Paths has been performed.
 *
 * @author tomforde
 */
class PSSInfo {
    /**
     * Does the node take part in a PSS.
     */
    private final boolean activePSS;
    /**
     * Is the node end node of the PSS.
     */
    private final boolean isEnd;
    /**
     * Is the node start node of the PSS.
     */
    private final boolean isStart;
    /**
     * The OTCNode's ID, this information is meant for.
     */
    private final int otcNodeID;
    /**
     * The list of all predecessors within the PSS system.
     */
    private final List<OTCNodeRegion> preds;

    /**
     * The list of all successors within the PSS system.
     */
    private final List<OTCNodeRegion> succs;

    PSSInfo(final int id, final boolean isPSSactive) {
        this.otcNodeID = id;
        this.isStart = false;
        this.isEnd = false;
        this.activePSS = isPSSactive;
        this.preds = new ArrayList<>();
        this.succs = new ArrayList<>();
    }

    PSSInfo(final int id, final OTCNodeRegion pred, final OTCNodeRegion succ, final boolean start,
            final boolean end, final boolean pssActive) {
        this.otcNodeID = id;
        this.preds = new ArrayList<>();
        this.preds.add(pred);

        this.succs = new ArrayList<>();
        this.succs.add(succ);

        this.isStart = start;
        this.isEnd = end;
        this.activePSS = pssActive;
    }

    final int getOtcNodeID() {
        return otcNodeID;
    }

    final List<OTCNodeRegion> getPreds() {
        return preds;
    }

    final List<OTCNodeRegion> getSuccs() {
        return succs;
    }

    final boolean isActivePSS() {
        return activePSS;
    }

    final boolean isEnd() {
        return isEnd;
    }

    final boolean isStart() {
        return isStart;
    }
}
