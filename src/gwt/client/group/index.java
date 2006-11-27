//
// $Id$

package client.group;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

import client.shell.MsoyEntryPoint;

public class index extends MsoyEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        if (token.length() > 0) {
            try {
                setContent(new GroupView(_ctx, Integer.parseInt(token)));
            } catch (Exception e) {
                // TODO: display error
            }
        } else {
            setContent(new GroupList(_ctx));
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "group";
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        onHistoryChanged(History.getToken());
    }
}
