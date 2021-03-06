//
// $Id$

package client.person;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.CenteredBox;
import com.threerings.gwt.ui.InlinePanel;

import com.threerings.msoy.person.gwt.Gallery;
import com.threerings.msoy.person.gwt.GalleryData;

import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.util.MediaUtil;

/**
 * Displays the Gallery meta data: name, description, thumbnail, yada yada.
 *
 * @author mjensen
 */
public class GalleryDetailPanel extends AbsolutePanel
{
    public GalleryDetailPanel (GalleryData galleryData)
    {
        Gallery gallery = galleryData.gallery;
        setStyleName("galleryDetailPanel");

        // Thumbnail centered horizontally & vertically
        add(new CenteredBox(MediaUtil.createMediaView(gallery.thumbMedia,
            MediaDescSize.THUMBNAIL_SIZE), "GalleryThumbnail",
            MediaDescSize.getWidth(MediaDescSize.THUMBNAIL_SIZE),
            MediaDescSize.getHeight(MediaDescSize.THUMBNAIL_SIZE)), 10, 10);

        String countText = galleryData.photos.size() == 1 ? _pmsgs.galleryOnePhoto()
            : _pmsgs.galleryPhotoCount("" + galleryData.photos.size());
        add(MsoyUI.createLabel(countText, "Count"), 20, 80);

        // Gallery and creator name are inline
        InlinePanel nameAndCreator = new InlinePanel("NameAndCreator");
        nameAndCreator.add(MsoyUI.createLabel(GalleryPanel.getGalleryLabel(gallery,
            galleryData.owner), "Name"));
        nameAndCreator.add(new CreatorLabel(galleryData.owner));
        add(nameAndCreator, 105, 5);

        add(MsoyUI.createLabel(gallery.description, "Description"), 105, 40);
    }

    protected static final PersonMessages _pmsgs = (PersonMessages)GWT.create(PersonMessages.class);
}
