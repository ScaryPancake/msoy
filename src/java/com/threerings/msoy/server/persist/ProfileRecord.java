//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

/**
 * Contains a single row from the PROFILES table.
 */
public class ProfileRecord
{
    /** The unique id of the memory with whom this profile is associated. */
    public int memberId;

    /** The member's home page URL (maxlen: 255). */
    public String homePageURL;

    /** A short bit of text provided by the member (maxlen: 255). */
    public String headline;

    /** Whether the member identifies as male or female. */
    public boolean isMale;

    /** The date on which the member claims to be born. */
    public Date birthday;

    /** The locale from which the member claims to hail (maxlen: 255). */
    public String location;
}
