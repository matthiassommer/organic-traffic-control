package de.dfg.oc.otc.routing.linkState;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains several {@link DatabaseEntry}. Sent by a source
 * {@link LinkStateRC} by flooding to his direct neighbours if costs to a
 * neighbour have changed.
 * <p/>
 * Advertisements with older sequence numbers will be ignored and not further
 * sent on. The advertisement won't be sent back to the source.
 *
 * @author lyda
 */
public class Advertisement {
    /**
     * List of database entries.
     */
    private final List<DatabaseEntry> databaseEntries;
    /**
     * Sequence number of this advertisement.
     */
    private final int sequenceNumber;
    /**
     * The source who sent this advertisement.
     */
    private final LinkStateRC source;

    /**
     * Create a new advertisement with the origin sender and the sequence
     * number.
     *
     * @param source         the sender
     * @param sequenceNumber the sequence number
     */
    public Advertisement(final LinkStateRC source, final int sequenceNumber) {
        this.source = source;
        this.sequenceNumber = sequenceNumber;
        this.databaseEntries = new ArrayList<>();
    }

    /**
     * Add a new database entry.
     *
     * @param entry for the database
     */
    public final void addLinkStateDataBaseEntry(final DatabaseEntry entry) {
        this.databaseEntries.add(entry);
    }

    /**
     * Get all database entries of this advertisement.
     *
     * @return database entries
     */
    public final Iterable<DatabaseEntry> getDatabaseEntries() {
        return databaseEntries;
    }

    /**
     * Get the sequence number of this advertisement.
     *
     * @return sequence number
     */
    public final int getSequenceNumber() {
        return sequenceNumber;
    }

    /**
     * Get the source who sent this advertisement.
     *
     * @return sender
     */
    public final LinkStateRC getSource() {
        return source;
    }
}
