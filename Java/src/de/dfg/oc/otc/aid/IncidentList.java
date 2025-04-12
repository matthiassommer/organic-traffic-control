package de.dfg.oc.otc.aid;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wrapper class used for un-/marshalling a list of incidents from/to a xml
 * file.
 */
@XmlRootElement
class IncidentList {
    /**
     * List containing the incidents.
     */
    private final List<Incident> incidents = new ArrayList<>();

    /**
     * Returns the list of incidents.
     *
     * @return List of incidents
     */
    @XmlElement(name = "incident")
    public Collection<Incident> getIncidents() {
        return incidents;
    }
}
