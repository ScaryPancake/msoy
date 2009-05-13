//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.StyledTabPanel;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays an interface for editing a game.
 */
public class EditGamePanel extends FlowPanel
{
    public EditGamePanel ()
    {
        setStyleName("editGame");
        // TODO: add loading display
    }

    public void setGame (int gameId, final int tabIdx)
    {
        if (_gameId == gameId) {
            _tabs.selectTab(tabIdx);
            return;
        }

        _gameId = gameId;
        _gamesvc.loadGameData(gameId, new InfoCallback<GameService.GameData>() {
            public void onSuccess (GameService.GameData data) {
                init(data, tabIdx);
            }
        });
    }

    protected void init (final GameService.GameData data, int tabIdx)
    {
        clear();

        SmartTable header = new SmartTable(0, 10);
        header.setText(0, 0, data.info.name, 1, "Title");
        // TODO: add thumbnail if we've got one
        add(header);

        // add our giant tab list of doom
        add(_tabs = new StyledTabPanel());

        addTab(_msgs.egTabInfo(), new LazyPanel() {
            protected Widget createWidget () {
                return new InfoEditorPanel(data.info);
            }
        });
        addTab(_msgs.egTabCode(), new LazyPanel() {
            protected Widget createWidget () {
                return new CodeEditorPanel(data.code);
            }
        });
        addTab(_msgs.egTabTrophies(), new LazyPanel() {
            protected Widget createWidget () {
                return createSubItemPanel(Item.TROPHY_SOURCE, data);
            }
        });
        addTab(_msgs.egTabIPacks(), new LazyPanel() {
            protected Widget createWidget () {
                return createSubItemPanel(Item.ITEM_PACK, data);
            }
        });
        addTab(_msgs.egTabLPacks(), new LazyPanel() {
            protected Widget createWidget () {
                return createSubItemPanel(Item.LEVEL_PACK, data);
            }
        });
        addTab(_msgs.egTabPrizes(), new LazyPanel() {
            protected Widget createWidget () {
                return createSubItemPanel(Item.PRIZE, data);
            }
        });

        // select the desired tab
        _tabs.selectTab(tabIdx);

        // route tab selection through the URL
        _tabs.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            public void onBeforeSelection (BeforeSelectionEvent<Integer> event) {
                CShell.log("Going to tab " + event.getItem());
                Link.go(Pages.GAMES, Args.compose("e", _gameId, event.getItem()));
            }
        });
    }

    protected void addTab (String label, LazyPanel panel)
    {
        _tabs.add(panel, label);
    }

    protected Widget createSubItemPanel (byte itemType, GameService.GameData data)
    {
        SmartTable panel = new SmartTable();
        panel.setText(0, 0, "Sub-item " + itemType);
        return panel;
    }

    protected int _gameId;
    protected StyledTabPanel _tabs;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
