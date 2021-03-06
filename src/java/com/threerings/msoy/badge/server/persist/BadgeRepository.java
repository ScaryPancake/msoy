//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.DatabaseException;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;

import com.threerings.presents.annotation.BlockingThread;

@Singleton @BlockingThread
public class BadgeRepository extends DepotRepository
{
    @Inject public BadgeRepository (PersistenceContext ctx)
    {
        super(ctx);

        // TEMP: may be removed sometime after all servers have been updated past 2008-10-28
        registerMigration(new DataMigration("2008_10_28_erase_hidden_badge") {
            public void invoke () throws DatabaseException
            {
                from(InProgressBadgeRecord.class).where(
                    InProgressBadgeRecord.BADGE_CODE, 0x78a52e3b).delete();
            }
        });
    }

    /**
     * Stores the supplied badge record in the database.
     *
     * @return true if the record was created, false if it was updated.
     */
    public boolean storeBadge (EarnedBadgeRecord badge)
    {
        return store(badge);
    }

    /**
     * Stores the supplied in-progress badge record in the database.
     */
    public void storeInProgressBadge (InProgressBadgeRecord badge)
    {
        store(badge);
    }

    /**
     * Loads all of the specific member's earned badges.
     */
    public List<EarnedBadgeRecord> loadEarnedBadges (int memberId)
    {
        return from(EarnedBadgeRecord.class).where(EarnedBadgeRecord.MEMBER_ID, memberId).select();
    }

    /**
     * Returns up to limit badges, order by date descending.
     */
    public List<EarnedBadgeRecord> loadRecentEarnedBadges (int memberId, int limit)
    {
        return from(EarnedBadgeRecord.class).where(EarnedBadgeRecord.MEMBER_ID, memberId).
            limit(limit).descending(EarnedBadgeRecord.WHEN_EARNED).select();
    }

    /**
     * Loads all of the specified member's in-progress badges.
     */
    public List<InProgressBadgeRecord> loadInProgressBadges (int memberId)
    {
        return from(InProgressBadgeRecord.class).
            where(InProgressBadgeRecord.MEMBER_ID, memberId).select();
    }

    /**
     * Loads the EarnedBadgeRecord, if it exists, for the specified member and badge type.
     */
    public EarnedBadgeRecord loadEarnedBadge (int memberId, int badgeCode)
    {
        return load(EarnedBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Loads the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     */
    public InProgressBadgeRecord loadInProgressBadge (int memberId, int badgeCode)
    {
        return load(InProgressBadgeRecord.getKey(memberId, badgeCode));
    }

    /**
     * Deletes the InProgressBadgeRecord, if it exists, for the specified member and badge type.
     *
     * @return true if the record was deleted; false if it did not exist
     */
    public boolean deleteInProgressBadge (int memberId, int badgeCode)
    {
        return (delete(InProgressBadgeRecord.getKey(memberId, badgeCode)) > 0);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        from(EarnedBadgeRecord.class).where(EarnedBadgeRecord.MEMBER_ID.in(memberIds)).delete();
        from(InProgressBadgeRecord.class).where(
            InProgressBadgeRecord.MEMBER_ID.in(memberIds)).delete();
    }

    @Override
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(EarnedBadgeRecord.class);
        classes.add(InProgressBadgeRecord.class);
    }
}
