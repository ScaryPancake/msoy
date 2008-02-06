//
// $Id$

package client.whirled;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WorldService;
import com.threerings.msoy.web.client.WorldServiceAsync;
import com.threerings.msoy.web.data.Invitation;

import client.msgs.MsgsEntryPoint;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we're not logged in, default to whirledwide, if so, default to mywhirled
        String action = args.get(0, "");

        // if we're logged in and specified no action, go to "My Whirled"
        if (CWhirled.getMemberId() != 0 && action.equals("")) {
            action = "mywhirled";
        }

        if (action.equals("whirledwide")) {
            Frame.setTitle(CWhirled.msgs.titleWhirledwide());
            setContent(new Whirledwide(createPopulationDisplay()));

        // only load their invitation and redirect to the main page if they're not logged in
        } else if (action.equals("i") && CWhirled.getMemberId() == 0) {
            String inviteId = args.get(1, "");
            if (Application.activeInvite != null &&
                Application.activeInvite.inviteId.equals(inviteId)) {
                Application.go(Page.WHIRLED, "");
            } else {
                CWhirled.membersvc.getInvitation(inviteId, true, new MsoyCallback() {
                    public void onSuccess (Object result) {
                        Application.activeInvite = (Invitation)result;
                        Application.go(Page.WHIRLED, "");
                    }
                });
            }

        // if we are logged in, we always display mywhirled instead of the landing page
        } else if (action.equals("mywhirled") && CWhirled.getMemberId() != 0) {
            Frame.setTitle(CWhirled.msgs.titleMyWhirled());
            setContent(new MyWhirled(createPopulationDisplay()));
            FlashClients.tutorialEvent("myWhirledVisited");

        } else {
            displayWhat();
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return WHIRLED;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CWhirled.worldsvc = (WorldServiceAsync)GWT.create(WorldService.class);
        ((ServiceDefTarget)CWhirled.worldsvc).setServiceEntryPoint("/worldsvc");

        // load up our translation dictionaries
        CWhirled.msgs = (WhirledMessages)GWT.create(WhirledMessages.class);
    }

    protected void displayWhat ()
    {
        Frame.closeClient(false); // no client on the main guest landing page
        setContent(new WhatIsTheWhirled(), false);
    }

    protected PopulationDisplay createPopulationDisplay ()
    {
        return new PopulationDisplay() {
            public void displayPopulation (int population) {
                // This is a hack to get the population into the usual tabs spot...
                VerticalPanel container = new VerticalPanel();
                container.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
                container.add(WidgetUtil.makeShim(5, 3));
                Label popLabel = new Label(CWhirled.msgs.populationDisplay("" + population));
                popLabel.setStyleName("PopulationDisplay");
                container.add(popLabel);
                setPageTabs(container);
            }
        };
    }
}
