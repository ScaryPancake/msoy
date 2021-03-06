//
// $Id$

package com.threerings.msoy.badge.server.persist;

import java.sql.Timestamp;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import com.samskivert.util.StringUtil;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.all.EarnedBadge;

public class EarnedBadgeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<EarnedBadgeRecord> _R = EarnedBadgeRecord.class;
    public static final ColumnExp<Integer> MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp<Integer> BADGE_CODE = colexp(_R, "badgeCode");
    public static final ColumnExp<Integer> LEVEL = colexp(_R, "level");
    public static final ColumnExp<Timestamp> WHEN_EARNED = colexp(_R, "whenEarned");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** Transforms a persistent record to a runtime record. */
    public static Function<EarnedBadgeRecord, EarnedBadge> TO_BADGE =
        new Function<EarnedBadgeRecord, EarnedBadge>() {
        public EarnedBadge apply (EarnedBadgeRecord record) {
            return record.toBadge();
        }
    };

    /** The id of the member that holds this badge. */
    @Id
    public int memberId;

    /** The code that uniquely identifies the badge type. */
    @Id
    public int badgeCode;

    /** The highest badge level that the player has attained. */
    public int level;

    /** The date and time when this badge was earned. */
    public Timestamp whenEarned;

    /**
     * Constructs an empty EarnedBadgeRecord.
     */
    public EarnedBadgeRecord ()
    {
    }

    /**
     * Constructs an EarnedBadgeRecord from an EarnedBadge.
     */
    public EarnedBadgeRecord (int memberId, EarnedBadge badge)
    {
        Preconditions.checkArgument(memberId > 0, "memberId must be positive.");
        this.memberId = memberId;
        this.badgeCode = badge.badgeCode;
        this.level = badge.level;
        this.whenEarned = new Timestamp(badge.whenEarned);
    }

    /**
     * Converts this persistent record to a runtime record.
     */
    public EarnedBadge toBadge ()
    {
        BadgeType type = BadgeType.getType(badgeCode);
        String levelUnits = type.getRequiredUnitsString(level);
        int coinValue = type.getCoinValue(level);
        return new EarnedBadge(badgeCode, level, levelUnits, coinValue, whenEarned.getTime());
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    /** Helper function for {@link #toString}. */
    public String badgeCodeToString ()
    {
        return String.valueOf(BadgeType.getType(badgeCode));
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link EarnedBadgeRecord}
     * with the supplied key values.
     */
    public static Key<EarnedBadgeRecord> getKey (int memberId, int badgeCode)
    {
        return newKey(_R, memberId, badgeCode);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(MEMBER_ID, BADGE_CODE); }
    // AUTO-GENERATED: METHODS END
}
