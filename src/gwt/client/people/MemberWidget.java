//
// $Id$

package client.people;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MemberStatusLabel;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.util.Link;
import client.util.MediaUtil;
import client.util.InfoCallback;
import client.util.ServiceUtil;

/**
 * Displays a list of members.
 */
public class MemberWidget extends SmartTable
{
    public MemberWidget (MemberCard card)
    {
        super("memberWidget", 0, 5);

        setWidget(0, 0, MediaUtil.createMediaView(card.photo, MediaDesc.THUMBNAIL_SIZE,
                                                  Link.createListener(
                                                  Pages.PEOPLE, "" + card.name.getMemberId())),
                  1, "Photo");
        getFlexCellFormatter().setRowSpan(0, 0, 3);

        setWidget(0, 1, Link.create(card.name.toString(), Pages.PEOPLE,
                                               ""+card.name.getMemberId()), 1, "Name");

        // we'll overwrite these below if we have anything to display
        getFlexCellFormatter().setStyleName(1, 0, "Status");
        setHTML(1, 0, "&nbsp;");
        setHTML(2, 0, "&nbsp;");
        if (card.headline != null && card.headline.length() > 0) {
            setText(1, 0, card.headline);
        }
        setWidget(2, 0, new MemberStatusLabel(card));

        SmartTable extras = new SmartTable("Extras", 0, 5);
        addExtras(extras, card);
        setWidget(0, 2, extras);
        getFlexCellFormatter().setRowSpan(0, 2, getRowCount());
        getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
    }

    protected void addExtras (SmartTable extras, MemberCard card)
    {
        int row = extras.getRowCount();

        // always show the visit home button
        ClickListener onClick = Link.createListener(Pages.WORLD, "m" + card.name.getMemberId());
        extras.setWidget(row, 0,
            MsoyUI.createActionImage("/images/profile/visithome.png", onClick));
        extras.setWidget(row++, 1,
            MsoyUI.createActionLabel(_msgs.mlVisitHome(), onClick));
    }

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}