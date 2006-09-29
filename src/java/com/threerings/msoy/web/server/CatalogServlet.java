//
// $Id$

package com.threerings.msoy.web.server;

import java.util.ArrayList;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

import com.threerings.msoy.item.data.ItemIdent;
import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemGIdent;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebCreds;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ItemService}.
 */
public class CatalogServlet extends RemoteServiceServlet
    implements CatalogService
{
    // from interface CatalogService
    public ArrayList loadCatalog (WebCreds creds, String type)
        throws ServiceException
    {
        // TODO: validate this user's creds

        // convert the string they supplied to an item enumeration
        ItemEnum etype = ItemEnum.valueOf(type);
        if (etype == null) {
            log.warning("Requested to load catalog for invalid item type " +
                        "[who=" + creds + ", type=" + type + "].");
            throw new ServiceException("", ServiceException.INTERNAL_ERROR);
        }

        // load their catalog via the catalog manager
        ServletWaiter<ArrayList<CatalogListing>> waiter =
            new ServletWaiter<ArrayList<CatalogListing>>(
                "loadCatalog[" + creds.memberId + ", " + etype + "]");
        MsoyServer.itemMan.loadCatalog(creds.memberId, etype, waiter);
        return waiter.waitForResult();
    }

    // from interface CatalogService
    public Item purchaseItem (WebCreds creds, ItemGIdent item)
        throws ServiceException
    {
        ItemIdent ident = ItemServlet.toIdent(creds, item, "purchaseItem");
        ServletWaiter<Item> waiter = new ServletWaiter<Item>(
            "purchaseItem[" + creds.memberId + ", " + item + "]");
        MsoyServer.itemMan.purchaseItem(creds.memberId, ident, waiter);
        return waiter.waitForResult();

    }

    // from interface CatalogService
    public CatalogListing listItem (WebCreds creds, ItemGIdent item)
        throws ServiceException
    {
        ItemIdent ident = ItemServlet.toIdent(creds, item, "listItem");
        ServletWaiter<CatalogListing> waiter =
            new ServletWaiter<CatalogListing>("listItem[" + item + "]");
        MsoyServer.itemMan.listItem(ident, waiter);
        return waiter.waitForResult();
    }
}
