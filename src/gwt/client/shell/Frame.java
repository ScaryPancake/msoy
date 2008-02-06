//
// $Id$

package client.shell;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowResizeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.Predicate;

import client.util.MsoyUI;

/**
 * The frame wraps the top-level page HTML and handles displaying the navigation, the page content,
 * the client and sliding things around.
 */
public class Frame
{
    /** The height of our page header (just the menus and status stuff). */
    public static final int HEADER_HEIGHT = 50;

    /** The height of our Flash or Java client in pixels. */
    public static final int CLIENT_HEIGHT = 544;

    /** The maximum width of our content UI, the remainder is used by the world client. */
    public static final int CONTENT_WIDTH = 700;

    /** The width of the separator bar displayed between the client and the content. */
    public static final int SEPARATOR_WIDTH = 8;

    /** The offset of the content close button, from the left edge of the separator bar. */
    public static final int CLOSE_BUTTON_OFFSET = -16;

    /**
     * Called by the Application to initialize us once in the lifetime of the app.
     */
    public static void init (NaviPanel navi, StatusPanel status)
    {
        // create our member header
        _mheader = new FlexTable();
        _mheader.setWidth("100%");
        _mheader.setCellPadding(0);
        _mheader.setCellSpacing(0);
        _mheader.setStyleName("msoyHeader");
        _mheader.getFlexCellFormatter().setStyleName(0, 0, "Logo");
        _mheader.setWidget(0, 0, MsoyUI.createActionImage(LOGO_PATH, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WHIRLED, "mywhirled");
            }
        }));
        _mheader.getFlexCellFormatter().setStyleName(0, 1, "Navi");
        _mheader.setWidget(0, 1, navi);
        _mheader.getFlexCellFormatter().setStyleName(0, 2, "Left");
        _mheader.getFlexCellFormatter().setStyleName(0, 3, "Right");
        _mheader.getFlexCellFormatter().setHorizontalAlignment(0, 3, HasAlignment.ALIGN_RIGHT);
        _mheader.setWidget(0, 3, status);

        // create our guest header
        _gheader = new FlexTable();
        _gheader.setWidth("100%");
        _gheader.setCellPadding(0);
        _gheader.setCellSpacing(0);
        _gheader.setStyleName("msoyHeader");
        _gheader.getFlexCellFormatter().setStyleName(0, 0, "Logo");
        _gheader.setWidget(0, 0, MsoyUI.createActionImage(LOGO_PATH, new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WHIRLED, "");
            }
        }));
        FlexTable signup = new FlexTable();
        signup.setWidget(0, 0, MsoyUI.createLabel("New to Whirled?", "New"));
        signup.setWidget(1, 0, Application.createLink("Sign up!", Page.ACCOUNT, "create"));
        _gheader.getFlexCellFormatter().setStyleName(0, 1, "Signup");
        _gheader.setWidget(0, 1, signup);
        _gheader.getFlexCellFormatter().setStyleName(0, 2, "Logon");
        _gheader.setWidget(0, 2, new LogonPanel(true));

        _minimizeContent = MsoyUI.createActionLabel("", "Minimize", new ClickListener() {
            public void onClick (Widget sender) {
                setContentMinimized(true, null);
            }
        });
        _maximizeContent = MsoyUI.createActionLabel("", "Maximize", new ClickListener() {
            public void onClick (Widget sender) {
                setContentMinimized(false, null);
            }
        });

        // if we're tall enough, handle scrolling ourselves
        Window.enableScrolling(pageTooShort());
        Window.addWindowResizeListener(_resizer);

        // set up the callbackd that our flash clients can call
        configureCallbacks();
    }

    /**
     * Sets the title of the browser window and the page (displayed below the Whirled logo).
     */
    public static void setTitle (String title)
    {
        setTitle(title, null);
    }

    /**
     * Sets the title and subtitle of the browser window and the page. The subtitle is displayed to
     * the right of the title in the page and tacked onto the title for the browser window.
     */
    public static void setTitle (String title, String subtitle)
    {
        if (_content != null) {
            _content.setPageTitle(title, subtitle);
        }
        title = (subtitle == null) ? title : (title + " - " + subtitle);
        Window.setTitle(CShell.cmsgs.windowTitle(title));
    }

    /**
     * Minimizes or maximizes the page content. NOOP if the content min/max interface is not being
     * displayed.
     */
    public static void setContentMinimized (boolean minimized, Command onComplete)
    {
        if (minimized && _minimizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_minimizeContent);
            RootPanel.get(SEPARATOR).add(_maximizeContent);
            new SlideContentOff().start(onComplete);

        } else if (!minimized && _maximizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_maximizeContent);
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            new SlideContentOn().start(onComplete);

        } else if (onComplete != null) {
            // no action needed, just run the onComplete
            onComplete.execute();
        }
    }

    /**
     * Switches the frame into client display mode (clearing out any content) and notes the history
     * token for the current page so that it can be restored in the event that we open a normal
     * page and then later close it.
     */
    public static void setShowingClient (String closeToken)
    {
        // note the current history token so that we can restore it if needed
        _closeToken = closeToken;

        // clear out our content and the expand/close controls
        RootPanel.get(CONTENT).clear();
        RootPanel.get(CONTENT).setWidth("0px");

        // clear out the divider
        RootPanel.get(SEPARATOR).clear();
        RootPanel.get(SEPARATOR).setWidth("0px");

        // have the client take up all the space
        RootPanel.get(CLIENT).setWidth("100%");
    }

    /**
     * Clears any open client and restores the content display.
     */
    public static void closeClient (boolean deferred)
    {
        if (deferred) {
            DeferredCommand.add(new Command() {
                public void execute () {
                    closeClient(false);
                }
            });
            return;
        }

        WorldClient.clientWillClose();
        _closeToken = null;
        RootPanel.get(SEPARATOR).clear();
        RootPanel.get(Frame.CLIENT).clear();
        RootPanel.get(Frame.CLIENT).setWidth("0px");
        RootPanel.get(Frame.CONTENT).setWidth("100%");
        _content.setCloseVisible(false);

        // if we're on a "world" page, go to "whirledwide"
        if (History.getToken().startsWith(Page.WORLD)) {
            Application.go(Page.WHIRLED, "whirledwide");
        }
    }

    /**
     * Clears the open content and restores the client to its full glory.
     */
    public static void closeContent ()
    {
        if (_closeToken == null) {
            return;
        }

        if (_maximizeContent.isAttached()) {
            RootPanel.get(SEPARATOR).remove(_maximizeContent);
            History.newItem(_closeToken);

        } else {
            new SlideContentOff().start(new Command() {
                public void execute () {
                    RootPanel.get(SEPARATOR).remove(_minimizeContent);
                    History.newItem(_closeToken);
                }
            });
        }

        _content = null;
        _contlist = null;
        _scroller = null;
    }

    /**
     * Shows or hides the navigation header as desired.
     */
    public static void setHeaderVisible (boolean visible)
    {
        RootPanel.get(HEADER).remove(_gheader);
        RootPanel.get(HEADER).remove(_mheader);
        if (visible) {
            RootPanel.get(HEADER).add(CShell.getMemberId() == 0 ? _gheader : _mheader);
        }
    }

    /**
     * Requests that the specified widget be scrolled into view.
     */
    public static void ensureVisible (Widget widget)
    {
        if (_scroller != null) {
            _scroller.ensureVisible(widget);
        }
    }

    /**
     * Displays the supplied dialog in the frame.
     */
    public static void showDialog (String title, Widget dialog)
    {
        Dialog pd = new Dialog(title, dialog);
        if (_contlist != null) {
            _contlist.insert(pd, 0); // TODO: animate this sliding down
        } else {
            RootPanel.get(HEADER).add(pd); // TODO: animate this sliding down
        }
    }

    /**
     * Clears the specified dialog from the frame. Returns true if the dialog was located and
     * cleared, false if not.
     */
    public static boolean clearDialog (final Widget dialog)
    {
        return clearDialog(new Predicate() {
            public boolean isMatch (Object o) {
                return (o == dialog);
            }
        }) > 0;
    }

    /**
     * Clears all dialogs that match the specified predicate. Returns the number of dialogs
     * cleared.
     */
    public static int clearDialog (Predicate pred)
    {
        int removed = clearDialog(RootPanel.get(HEADER), pred);
        if (_contlist != null) {
            removed += clearDialog(_contlist, pred);
        }
        return removed;
    }

    /**
     * Configures the frame with our page's content table.
     */
    protected static void initContent (Page.Content content)
    {
        _content = content;
    }

    /**
     * Displays the supplied page content (which is generally the same as the table previously
     * configured via {@link #initContent}). Will animate the content sliding on if appropriate.
     */
    protected static void showContent (Widget page)
    {
        RootPanel.get(CONTENT).clear();

        // clear out any lingering dialogs
        clearDialog(Predicate.TRUE);

        // note that this is our current content
        _contlist = new FlowPanel();
        _contlist.setWidth("100%");
        _contlist.add(page);

        Widget content;
        if (pageTooShort()) {
            content = _contlist;
            Window.enableScrolling(true);
        } else {
            content = (_scroller = new ScrollPanel(_contlist));
            _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            Window.enableScrolling(false);
        }

        // if we're displaying the client or we have a minimized page, unminimize things first
        if (_maximizeContent.isAttached() ||
            (_closeToken != null && !_minimizeContent.isAttached())) {
            RootPanel.get(SEPARATOR).clear();
            RootPanel.get(SEPARATOR).add(_minimizeContent);
            new SlideContentOn().start(null);

        } else {
            RootPanel.get(CONTENT).add(content);
            RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
        }

        _content.setCloseVisible(RootPanel.get(CLIENT).getWidgetCount() > 0);
    }

    protected static boolean pageTooShort ()
    {
        return Window.getClientHeight() < (HEADER_HEIGHT + CLIENT_HEIGHT);
    }

    protected static void restoreClient ()
    {
        setContentMinimized(true, null);
    }

    protected static int clearDialog (ComplexPanel panel, Predicate pred)
    {
        if (panel == null) {
            return 0; // cope with stale index.html files
        }
        int removed = 0;
        for (int ii = 0; ii < panel.getWidgetCount(); ii++) {
            Widget widget = panel.getWidget(ii);
            if (widget instanceof Dialog && pred.isMatch(((Dialog)widget).getContent())) {
                panel.remove(ii);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks () /*-{
       $wnd.restoreClient = function () {
            @client.shell.Frame::restoreClient()();
       };
       $wnd.clearClient = function () {
            @client.shell.Frame::closeClient(Z)(true);
       };
    }-*/;

    protected static native boolean isLinux () /*-{
        return (navigator.userAgent.toLowerCase().indexOf("linux") != -1);
    }-*/;

    protected static abstract class Slider extends Timer
    {
        public void start (Command onComplete) {
            _onComplete = onComplete;
//             scheduleRepeating(25);
            run();
        }

        protected void done () {
            cancel();
            if (_onComplete != null) {
                _onComplete.execute();
            }
        }

        protected Command _onComplete;
        protected static final int FRAMES = 6;
    }

    protected static class SlideContentOff extends Slider
    {
        public SlideContentOff () {
            RootPanel.get(CONTENT).clear();
            WorldClient.setMinimized(false);
            _content.setCloseVisible(false);
        }

        public void run () {
//             if (_startWidth >= _endWidth) {
                RootPanel.get(CONTENT).setWidth("0px");
                RootPanel.get(CLIENT).setWidth(_endWidth + "px");
                done();

//             } else {
//                 RootPanel.get(CONTENT).setWidth((_availWidth - _startWidth) + "px");
//                 RootPanel.get(CLIENT).setWidth(_startWidth + "px");
//                 _startWidth += _deltaWidth;
//             }
        }

        protected int _availWidth = Window.getClientWidth() - SEPARATOR_WIDTH;
        protected int _startWidth = Math.max(_availWidth - CONTENT_WIDTH, 0);
        protected int _endWidth = _availWidth;
        protected int _deltaWidth = (_endWidth - _startWidth) / FRAMES;
    }

    protected static class SlideContentOn extends Slider
    {
        public void run () {
//             if (_startWidth <= _endWidth) {
                if (_scroller == null) {
                    RootPanel.get(CONTENT).add(_contlist);
                } else {
                    RootPanel.get(CONTENT).add(_scroller);
                }
                RootPanel.get(CONTENT).setWidth(CONTENT_WIDTH + "px");
                RootPanel.get(CLIENT).setWidth(_endWidth + "px");
                WorldClient.setMinimized(true);
                _content.setCloseVisible(true);
                done();

//             } else {
//                 RootPanel.get(CONTENT).setWidth((_availWidth - _startWidth) + "px");
//                 RootPanel.get(CLIENT).setWidth(_startWidth + "px");
//                 _startWidth += _deltaWidth;
//             }
        }

        protected int _availWidth = Window.getClientWidth() - SEPARATOR_WIDTH;
        protected int _endWidth = Math.max(_availWidth - CONTENT_WIDTH, 0);
        protected int _startWidth = _availWidth;
        protected int _deltaWidth = (_endWidth - _startWidth) / FRAMES;
    }

    protected static class Dialog extends FlexTable
    {
        public Dialog (String title, Widget content)
        {
            setCellPadding(0);
            setCellSpacing(0);
            setStyleName("pageHeader");

            setText(0, 0, title);
            getFlexCellFormatter().setStyleName(0, 0, "TitleCell");

            setWidget(0, 1, MsoyUI.createActionLabel("", "CloseBox", new ClickListener() {
                public void onClick (Widget sender) {
                    Frame.clearDialog(getContent());
                }
            }));
            getFlexCellFormatter().setStyleName(0, 1, "CloseCell");

            setWidget(1, 0, content);
            getFlexCellFormatter().setColSpan(1, 0, 2);

            setWidget(2, 0, WidgetUtil.makeShim(5, 5));
            getFlexCellFormatter().setColSpan(2, 0, 2);
        }

        public Widget getContent ()
        {
            return getWidget(1, 0);
        }
    }

    protected static WindowResizeListener _resizer = new WindowResizeListener() {
        public void onWindowResized (int width, int height) {
            if (_scroller != null) {
                _scroller.setHeight((Window.getClientHeight() - HEADER_HEIGHT) + "px");
            }
        }
    };

    protected static FlexTable _mheader, _gheader;
    protected static String _closeToken;

    protected static Page.Content _content;
    protected static FlowPanel _contlist;
    protected static ScrollPanel _scroller;
    protected static Label _minimizeContent, _maximizeContent;

    // constants for our top-level elements
    protected static final String HEADER = "header";
    protected static final String CONTENT = "content";
    protected static final String SEPARATOR = "seppy";
    protected static final String CLIENT = "client";

    protected static final String LOGO_PATH = "/images/header/header_logo.png";
}
