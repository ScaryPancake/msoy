//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.client.PlaceView;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * An expanded PlaceView interface that can be used by views that wish to learn about their actual
 * pixel dimensions.
 */
public interface MsoyPlaceView extends PlaceView, PlaceLayer
{
    /**
     * Inform the place view whether or not it's showing.
     */
    function setIsShowing (showing :Boolean) :void;

    /**
     * Whether or not to put whitespace above and below this view.
     */
    function padVertical () :Boolean;

    /**
     * Indicates if we should use the chat overlay for this place.
     */
    function shouldUseChatOverlay () :Boolean;

    /**
     * Get the place name, or null if none.
     */
    function getPlaceName () :String;

    /**
     * Get the place logo, thumbnail media descriptor, or null if none.
     */
    function getPlaceLogo () :MediaDesc;
}
}
