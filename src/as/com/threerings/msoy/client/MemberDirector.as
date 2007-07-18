//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.ReportingListener;

public class MemberDirector extends BasicDirector
{
    public const log :Log = Log.getLog(MemberDirector);

    public function MemberDirector (ctx :BaseContext)
    {
        super(ctx);
        _bctx = ctx;
    }

    /**
     * Request to make the user our friend.
     */
    public function inviteToBeFriend (friendId :int) :void
    {
        _msvc.inviteToBeFriend(
            _bctx.getClient(), friendId,
            new ReportingListener(_bctx, MsoyCodes.GENERAL_MSGS, null, "m.friend_invited"));
    }

    /**
     * Request to change our display name.
     */
    public function setDisplayName (newName :String) :void
    {
        _msvc.setDisplayName(_bctx.getClient(), newName, new ReportingListener(_bctx));
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.BASE_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _msvc = (client.requireService(MemberService) as MemberService);
    }

    protected var _bctx :BaseContext;
    protected var _msvc :MemberService;
}
}
