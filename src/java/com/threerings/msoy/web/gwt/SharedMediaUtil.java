//
// $Id$

package com.threerings.msoy.web.gwt;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

/**
 * Utility routines for displaying media or generating html code to display media.
 */
public class SharedMediaUtil
{
    /** Represents the size of an html image (&lt;img/&gt;). */
    public static class Dimensions
    {
        /** The width of the image. */
        public final String width;

        /** The height of the image. */
        public final String height;

        public Dimensions (String width, String height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Resolves the correct width and height for embedding an image media descriptor as an html
     * image. If no specific values are required, null is returned.
     */
    public static Dimensions resolveImageSize (MediaDesc desc, int width, int height)
    {
        if (!(desc instanceof MediaDesc)) {
            return null;
        }
        switch (desc.getConstraint()) {
        case MediaDesc.HALF_HORIZONTALLY_CONSTRAINED:
            return (width < MediaDescSize.THUMBNAIL_WIDTH) ?
                new Dimensions(width + "px", "auto") : null;

        case MediaDesc.HALF_VERTICALLY_CONSTRAINED:
            return (height < MediaDescSize.THUMBNAIL_HEIGHT) ?
                new Dimensions("auto", height + "px") : null;

        case MediaDesc.HORIZONTALLY_CONSTRAINED:
            return new Dimensions(width + "px", "auto");

        case MediaDesc.VERTICALLY_CONSTRAINED:
            return new Dimensions("auto", height + "px");

        default:
            return null;
        }
    }
}
