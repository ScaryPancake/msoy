//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Maps a Party to its leader in a PartyPlaceObject.
 */
@com.threerings.util.ActionScript(omit=true)
public class PartyLeader extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The id of the party. */
    public int partyId;

    /** The memberId of the leader. */
    public int leaderId;

    /** Suitable for deserialization. */
    public PartyLeader ()
    {
    }

    /**
     * Constructor.
     */
    public PartyLeader (int partyId, int leaderId)
    {
        this.partyId = partyId;
        this.leaderId = leaderId;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return partyId;
    }
}
