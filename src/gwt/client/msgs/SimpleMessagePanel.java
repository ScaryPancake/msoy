//
// $Id$

package client.msgs;

import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.fora.gwt.ForumMessage;

import client.ui.MessagePanel;

/**
 * A message panel that displays a forum message with a last edited date.
 */
public class SimpleMessagePanel extends MessagePanel
{
    public SimpleMessagePanel ()
    {
    }

    public SimpleMessagePanel (ForumMessage message)
    {
        setMessage(message);
    }

    public void setMessage (ForumMessage message)
    {
        setMessage(message.poster, message.created, message.message);

        if (!message.lastEdited.equals(message.created)) {
            getFlexCellFormatter().setRowSpan(0, 0, 3); // extend the photo cell
            setText(2, 0, "Edited on " + DateUtil.formatDateTime(message.lastEdited));
            getFlexCellFormatter().setStyleName(2, 0, "Posted");
            getFlexCellFormatter().addStyleName(2, 0, "LeftPad");
            getFlexCellFormatter().addStyleName(2, 0, "BottomPad");
        }
    }

    @Override // from MessagePanel
    protected boolean textIsHTML ()
    {
        return true;
    }
}
