//
// $Id$

package client.mail;

import client.group.GroupInvite;
import client.profile.FriendInvite;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.web.client.WebContext;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MailMessage;

/**
 * Base class for payload visualizers. Concrete subclasses of this object are configured
 * with a {@link WebContext} and a {@link MailMessage}, and will be asked to hand out Widgets
 * to be displayed in mail messages in the GTW Mail system through the functions
 * {@link #widgetForRecipient()} and {@link #widgetForOthers()).
 */
public abstract class MailPayloadDisplay
{
    /**
     * Constructs and retursn the appropriate {@link MailPayloadDisplay} for the 
     * given mail message (presuming it has a payload).
     */
    public static MailPayloadDisplay getDisplay (WebContext ctx, MailMessage message)
    {
        if (message.payload == null) {
            return null;
        }
        switch (message.payload.getType()) {
        case MailPayload.TYPE_GROUP_INVITE:
            return new GroupInvite.Display(ctx, message);
        case MailPayload.TYPE_FRIEND_INVITE:
            return new FriendInvite.Display(ctx, message);
        }
        throw new IllegalArgumentException(
            "Unknown payload requested [type=" + message.payload.getType() + "]");
    }

    public MailPayloadDisplay (WebContext ctx, MailMessage message)
    {
        _ctx = ctx;
        _message = message;
    }
    
    /**
     *  Returns the {@link Widget} to be displayed to the recipient of this message.
     *  This object may (and typically will) contain active UI components to initiate
     *  requests to the server. May be null, in which case nothing is displayed to
     *  the recipient.
     */
    public abstract Widget widgetForRecipient (MailUpdateListener listener);

    /**
     *  Returns a {@link Widget} to display to anybody who is not this message's recipient.
     *  This object is meant to illustrate to an observer what the message looks like to
     *  the recipient, but any UI components it includes should be inactive. May be null,
     *  in which case nothing is displayed to the viewer.
     */
    public abstract Widget widgetForOthers ();

    /**
     * Performs a server request to update the state for this message. If the callback
     * argument is null, one is created for you which does nothing on success and throws
     * a RuntimeException on failure.
     */
    protected void updateState (MailPayload payload, AsyncCallback callback)
    {
        if (callback == null) {
            callback = new AsyncCallback() {
                public void onSuccess (Object result) {
                }
                public void onFailure (Throwable caught) {
                    throw new RuntimeException(caught);
                }
            };
        }
        _ctx.mailsvc.updatePayload(_ctx.creds, _message.headers.folderId,
                                   _message.headers.messageId, payload, callback);
    }

    protected WebContext _ctx;
    protected MailMessage _message;
}
