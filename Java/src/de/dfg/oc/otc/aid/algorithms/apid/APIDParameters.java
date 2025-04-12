package de.dfg.oc.otc.aid.algorithms.apid;

/**
 * Created by Dietmar on 03.05.2017.
 */
public class APIDParameters {

    /**
     * Defines whether medium traffic detection should be enabled or not.
     */
    public boolean MEDIUM_TRAFFIC_DETECTION_ENABLED;
    /**
     * Defines whether compression wave tests should be enabled or not. Deniz, Compass = false
     */
    public boolean COMPRESSION_WAVE_TEST_ENABLED;
    /**
     * Defines whether persistence tests should be enabled or not.
     */
    public boolean PERSISTENCE_TEST_ENABLED;
    /**
     * Occupancy threshold for medium traffic tests. Deniz: 60
     */
    public float TH_MEDIUM_TRAFFIC;
    /**
     * Occupancy threshold for incident clearance. Deniz: -0.4
     */
    public float TH_INC_CLR;
    /**
     * Occupancy threshold for persistence tests.
     */
    public float TH_PT;
    /**
     * Occupancy threshold 1 for compression wave test. Deniz: -1.3
     */
    public float TH_CW1;
    /**
     * Occupancy threshold 2 for compression wave test. Deniz: -1.5
     */
    public float TH_CW2;
    /**
     * Duration of persistence test in seconds.
     */
    public float PERSISTENCE_TEST_PERIOD;
    /**
     * Duration of compression wave test in seconds.
     */
    public float COMPRESSION_WAVE_TEST_PERIOD;
    /**
     * Threshold for the difference between the occupancies of two succeeding
     * detectors (OCCDF). Deniz: 10.2
     */
    public float TH_ID1;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors (OCCRDF). Deniz: 0.0, Compass: 0.4
     */
    public float TH_ID2;
    /**
     * Threshold for the occupancy of the 2nd detector (DOCC). Deniz: 20.8, compass: 28.8
     */
    public float TH_ID3;
    /**
     * Threshold for the relative difference between the occupancies of two
     * succeeding detectors for medium traffic (OCCRDF).
     */
    public float TH_MED_ID1;
    /**
     * Threshold for the relative temporal difference in speed for medium
     * traffic (SPDTDF).
     */
    public float TH_MED_ID2;

    public APIDParameters(boolean MEDIUM_TRAFFIC_DETECTION_ENABLED, boolean COMPRESSION_WAVE_TEST_ENABLED, boolean PERSISTENCE_TEST_ENABLED,
                          float TH_MEDIUM_TRAFFIC, float TH_INC_CLR, float TH_PT, float TH_CW1, float TH_CW2,
                          float PERSISTENCE_TEST_PERIOD, float COMPRESSION_WAVE_TEST_PERIOD, float TH_ID1, float TH_ID2,
                          float TH_ID3, float TH_MED_ID1, float TH_MED_ID2)
    {
        this.MEDIUM_TRAFFIC_DETECTION_ENABLED = MEDIUM_TRAFFIC_DETECTION_ENABLED;
        this.COMPRESSION_WAVE_TEST_ENABLED = COMPRESSION_WAVE_TEST_ENABLED;
        this.PERSISTENCE_TEST_ENABLED = PERSISTENCE_TEST_ENABLED;
        this.TH_MEDIUM_TRAFFIC = TH_MEDIUM_TRAFFIC;
        this.TH_INC_CLR = TH_INC_CLR;
        this.TH_PT = TH_PT;
        this.TH_CW1 = TH_CW1;
        this.TH_CW2 = TH_CW2;
        this.PERSISTENCE_TEST_PERIOD = PERSISTENCE_TEST_PERIOD;
        this.COMPRESSION_WAVE_TEST_PERIOD = COMPRESSION_WAVE_TEST_PERIOD;
        this.TH_ID1 = TH_ID1;
        this.TH_ID2 = TH_ID2;
        this.TH_ID3 = TH_ID3;
        this.TH_MED_ID1 = TH_MED_ID1;
        this.TH_MED_ID2 = TH_MED_ID2;
    }

    public void setParameters(APIDAlgorithm apid)
    {
        apid.MEDIUM_TRAFFIC_DETECTION_ENABLED = this.MEDIUM_TRAFFIC_DETECTION_ENABLED;
        apid.COMPRESSION_WAVE_TEST_ENABLED = this.COMPRESSION_WAVE_TEST_ENABLED;
        apid.PERSISTENCE_TEST_ENABLED = this.PERSISTENCE_TEST_ENABLED;
        apid.TH_MEDIUM_TRAFFIC = this.TH_MEDIUM_TRAFFIC;
        apid.TH_INC_CLR = this.TH_INC_CLR;
        apid.TH_PT = this.TH_PT;
        apid.TH_CW1 = this.TH_CW1;
        apid.TH_CW2 = this.TH_CW2;
        apid.PERSISTENCE_TEST_PERIOD = this.PERSISTENCE_TEST_PERIOD;
        apid.COMPRESSION_WAVE_TEST_PERIOD = this.COMPRESSION_WAVE_TEST_PERIOD;
        apid.TH_ID1 = this.TH_ID1;
        apid.TH_ID2 = this.TH_ID2;
        apid.TH_ID3 = this.TH_ID3;
        apid.TH_MED_ID1 = this.TH_MED_ID1;
        apid.TH_MED_ID2 = this.TH_MED_ID2;
    }

    @Override
    public String toString()
    {
        String s = this.MEDIUM_TRAFFIC_DETECTION_ENABLED + ";";
        s += this.COMPRESSION_WAVE_TEST_ENABLED + ";";
        s += this.PERSISTENCE_TEST_ENABLED + ";";
        s += this.TH_MEDIUM_TRAFFIC + ";";
        s += this.TH_INC_CLR + ";";
        s += this.TH_PT + ";";
        s += this.TH_CW1 + ";";
        s += this.TH_CW2 + ";";
        s += this.PERSISTENCE_TEST_PERIOD + ";";
        s += this.COMPRESSION_WAVE_TEST_PERIOD + ";";
        s += this.TH_ID1 + ";";
        s += this.TH_ID2 + ";";
        s += this.TH_ID3 + ";";
        s += this.TH_MED_ID1 + ";";
        s += this.TH_MED_ID2;
        return s;
    }
}
