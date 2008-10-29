//
// $Id$

package com.threerings.msoy.world.tour.client {

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomObjectView;

import com.threerings.msoy.world.tour.data.TourMarshaller;

public class TourDirector extends BasicDirector
{
    public const log :Log = Log.getLog(this);

    // reference the TourMarshaller class
    TourMarshaller;

    public function TourDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
    }

    public function isOnTour () :Boolean
    {
        return _wctx.getMemberObject().onTour;
    }

    public function startTour () :void
    {
        if (isOnTour()) {
            _wctx.displayFeedback(MsoyCodes.WORLD_MSGS, "e.already_touring");

        } else {
            nextRoom();
        }
    }

    public function nextRoom () :void
    {
        const roomView :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        const loadingDone :Boolean = (roomView != null) && roomView.loadingDone();
        _tsvc.nextRoom(_ctx.getClient(), loadingDone, new ResultAdapter(
            function (cause :String) :void {
                _wctx.displayFeedback(MsoyCodes.WORLD_MSGS, cause);
            },
            function (sceneId :int) :void {
                _wctx.getSceneDirector().moveTo(sceneId);
            }));
    }

    public function endTour () :void
    {
        if (isOnTour()) {
            _tsvc.endTour(_ctx.getClient());
        }
    }

    /**
     * Called when attributes change on our client object.
     */
    protected function cliObjAttrChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.ON_TOUR) {
            checkTouringStatus();
        }
    }

    protected function checkTouringStatus () :void
    {
        const onTour :Boolean = isOnTour();
        if (onTour && (_tourCtrl == null)) {
            _tourCtrl = new TourControl(_wctx, nextRoom, endTour);
            _wctx.getControlBar().addCustomComponent(_tourCtrl);

        } else if (!onTour && (_tourCtrl != null)) {
            _tourCtrl.parent.removeChild(_tourCtrl);
            _tourCtrl = null;
        }
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        client.getClientObject().addListener(new AttributeChangeAdapter(cliObjAttrChanged));
        checkTouringStatus();
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        super.registerServices(client);

        client.addServiceGroup(MsoyCodes.WORLD_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        _tsvc = (client.requireService(TourService) as TourService);
    }

    protected var _wctx :WorldContext;

    protected var _tsvc :TourService;

    protected var _tourCtrl :TourControl;
}
}
