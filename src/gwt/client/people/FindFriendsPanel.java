//
// $Id$

package client.people;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.EmailContact;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.Link;

/**
 * Presents various ways to find friends already on Whirled.
 */
public class FindFriendsPanel extends InvitePanel
{
    public FindFriendsPanel ()
    {
        addStyleName("findFriends");

        // introduction to sharing
        SmartTable intro = new SmartTable();
        intro.setWidget(0, 0, new Image("/images/people/share_header.png"));
        intro.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        intro.setWidget(0, 2, MsoyUI.createHTML(_msgs.ffIntro(), "Title"));
        add(intro);

        add(new WebMailControls(_msgs.ffCheckWebmail(), _msgs.ffFind()) {
            protected void handleAddresses (List<EmailContact> addresses) {
                List<EmailContact> members = new ArrayList<EmailContact>();
                for (EmailContact ec : addresses) {
                    if (ec.mname != null) {
                        members.add(ec);
                    }
                }
                handleMembers(members);
            }
        });

        add(_results = new SmartTable(0, 5));
        _results.setWidth("100%"); // TODO
        _results.setWidget(0, 0, Link.create(_msgs.ffSkip(), Pages.PEOPLE,
                                             Args.compose("invites", "newuser")));
        _results.getFlexCellFormatter().setHorizontalAlignment(0, 0, HasAlignment.ALIGN_RIGHT);
    }

    protected void handleMembers (List<EmailContact> members)
    {
        // clear out our old skip or results display
        remove(_results);

        int row = 0;
        add(_results = new SmartTable(0, 5));
        _results.setWidth("100%");
        _results.setText(row++, 0, _msgs.ffResultsTitle(), 4, "Bold");

        if (members.size() == 0) {
            _results.setText(row++, 0, _msgs.ffResultsNone(), 4, null);

        } else {
            _results.setText(row++, 0, _msgs.ffResultsSome(), 4, null);
            _results.setHTML(row++, 0, "&nbsp;", 4, null);
            _results.setText(row, 0, _msgs.ffResultsMName(), 1, "Header");
            _results.setText(row++, 1, _msgs.ffResultsEmail(), 1, "Header");

            for (EmailContact ec : members) {
                _results.setText(row, 0, ec.mname.toString());
                _results.setText(row, 1, _msgs.inviteMember(ec.name, ec.email));
                ClickListener onClick = new FriendInviter(ec.mname, "InvitePanel");
                _results.setWidget(row, 2, MsoyUI.createActionImage(ADD_IMAGE, onClick));
                _results.setWidget(row++, 3, MsoyUI.createActionLabel(
                                       _msgs.mlAddFriend(), onClick));
            }

            _results.setHTML(row++, 0, "&nbsp;", 4, null);
            _results.setText(row, 0, _msgs.ffAllDone(), 3, null);
        }

        _results.setWidget(row, 1, Link.create(_msgs.ffNext(), Pages.PEOPLE,
                                               Args.compose("invites", "newuser")), 1, null);
    }

    protected SmartTable _results;

    protected static final String ADD_IMAGE = "/images/profile/addfriend.png";
}