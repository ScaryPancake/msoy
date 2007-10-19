//
// $Id$

package tutorial {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.filters.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;

import com.threerings.util.EmbeddedSwfLoader;

public class View extends Sprite
{
    public static const SWIRL_NONE :int = 1;
    public static const SWIRL_IDLE :int = 2;
    public static const SWIRL_HUGE :int = 3;
    public static const SWIRL_BOUNCY :int = 4;

    public function View (tutorial :Tutorial)
    {
        _tutorial = tutorial;

        var loader :EmbeddedSwfLoader = new EmbeddedSwfLoader();
        loader.addEventListener(Event.COMPLETE, handleSwirlLoaded);
        loader.load(ByteArray(new SWIRL()));

        _swirlState = SWIRL_NONE;
    }

    /**
     * Called when there's a new batch of text to display.
     */
//    public function setSummary (summary :String) :void
//    {
//        _summary = summary;
//        updateSummary();
//    }

    /**
     * Called when we know our dimensions and can set up the text field.
     */
    public function init (width :Number, height :Number) :void
    {
        _width = width;
        _height = height;

        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 14;
        format.color = 0xDD7700;

        _textField = new TextField();
        /**
        _textField.border = false;
        _textField.borderColor = 0xFFFFFF;
        _textField.defaultTextFormat = format;
        _textField.multiline = true;
        _textField.embedFonts = true;
        _textField.autoSize = TextFieldAutoSize.NONE;
        _textField.wordWrap = true;
        _textField.width = width - SWIRL_SIZE;
        _textField.height = height;
        _textField.alpha = 0.5;
        */

        // don't add the text field until the swirly is loaded
        maybeFinishUI();
    }

    protected function handleSwirlLoaded (evt :Event) :void
    {
        _swirl = MovieClip(EmbeddedSwfLoader(evt.target).getContent());
        _swirl.visible = false;

        _todoDeleteMe = buildHugeSwirlTextLayer();

        _scenes = new Object();
        for (var ii :int = 0; ii < _swirl.scenes.length; ii ++) {
            var scene :Scene = _swirl.scenes[ii];
            _scenes[scene.name] = scene;
        }

        // don't add the swirly until the text field is loaded
        maybeFinishUI();
    }

    protected function maybeFinishUI () :void
    {
        // if both initializations are complete, actually add the bits
        if (_swirl && _textField) {
//            _swirl.scaleX = 0.2;
//            _swirl.scaleY = 0.2;
//            _swirl.x = -10;
//            _swirl.y = 0;
            _swirl.visible = true;
            _swirl.addEventListener(MouseEvent.CLICK, swirlClicked);
            this.addChild(_swirl);

            _todoDeleteMe.visible = false;
            _todoDeleteMe.addEventListener(MouseEvent.CLICK, swirlClicked);
            this.addChild(_todoDeleteMe);

//            var square :Sprite = new Sprite();
//            square.graphics.beginFill(0xFF0000);
//            square.graphics.drawRect(0, 0, _width, SWIRL_SIZE);
//            this.addChild(square);
//            _swirl.mask = square;

//            this.addChild(_textField);
//            _textField.x = SWIRL_SIZE;

            updateSummary();
            maybeTransition();
        }
    }

    // only try to display our summary text when the textfield is setup
    protected function updateSummary () :void
    {
        if (_textField) {
            _textField.htmlText = _summary ? _summary : "";
        }
    }

    // some day clicking on the swirly will do something
    protected function swirlClicked (evt :Event) :void
    {
        // when the swirly is big, clicking it offers the first quest
        _tutorial.swirlClicked(_swirlState);
    }

    public function gotoSwirlState (state :int) :void
    {
        // TODO: If we continue down this path, replace with properly queued transitions.
        if (state == SWIRL_HUGE) {
            if (_swirlState != SWIRL_NONE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "]");
            }
            _transition = SCN_APPEAR;
        } else if (state == SWIRL_IDLE) {
            if (_swirlState == SWIRL_HUGE) {
                // TODO: Does this automatically transition into SCN_IDLE?
                _transition = SCN_MINIMIZE;
            } else {
                _transition = SCN_IDLE;
                if (_swirlState != SWIRL_BOUNCY && swirlState != SWIRL_NONE) {
                    log.warning(
                        "Unexpected transtion [from=" + _swirlState + ", to=" + state + "]");
                }
            }
        } else if (state == SWIRL_BOUNCY) {
            if (_swirlState != SWIRL_IDLE) {
                log.warning("Unexpected transtion [from=" + _swirlState + ", to=" + state + "]");
            }
            _transition = SCN_LOOKATME;
        } else {
            log.warning("Can't goto unknown swirl state [state=" + state + "]");
            return;
        }
        _swirlState = state;
        maybeTransition();
    }

    protected function maybeTransition () :void
    {
        if (_swirl && _swirl.visible && _transition) {
            if (_transition == SCN_IDLE || _transition == SCN_LOOKATME) {
                _swirl.scaleX = _swirl.scaleY = 0.15;
            } else {
                _swirl.scaleX = _swirl.scaleY = 1.0;
            }
            _swirl.gotoAndPlay(1, _transition);
            // TODO: make this appear slowly, probably as part of the .swf
            _todoDeleteMe.visible = (_transition == SCN_APPEAR || _transition == SCN_MAXIMIZE);
            _transition = null;
        }
    }

    // TODO: ask Bill to simply include this in the SWF?
    protected function buildHugeSwirlTextLayer() :DisplayObject
    {
        var textLayer :Sprite = new Sprite();

        var format :TextFormat = new TextFormat();
        format.font = "SunnySide";
        format.size = 28;
        format.color = 0x203344;
        format.align = TextFormatAlign.CENTER;

        var topText :TextField = new TextField();
        topText.defaultTextFormat = format;
        topText.multiline = true;
        topText.wordWrap = true;
        topText.selectable = false;
        topText.embedFonts = true;
        topText.x = 0;
        topText.y = 0;
        topText.width = 180;
        topText.height = 100;
        topText.x = 190;
        topText.y = 90;
        topText.htmlText = "Welcome to<br>Whirled!";
        textLayer.addChild(topText);

        format = new TextFormat();
        format.font = "SunnySide";
        format.size = 16;
        format.color = 0x203344;
        format.italic = true;
        format.align = TextFormatAlign.CENTER;

        var midText :TextField = new TextField();
        midText.defaultTextFormat = format;
        midText.multiline = true;
        midText.wordWrap = true;
        midText.embedFonts = true;
        midText.selectable = false;
        midText.x = 0;
        midText.y = 0;
        midText.width = 240;
        midText.height = 100;
        midText.x = 160;
        midText.y = 180;
        midText.htmlText = "Whirled is a web-based social world for chat, games and player-created content.";
        textLayer.addChild(midText);

        format = new TextFormat();
        format.font = "SunnySide";
        format.size = 18;
        format.color = 0x323E44;
        format.align = TextFormatAlign.CENTER;

        var botText :TextField = new TextField();
        botText.defaultTextFormat = format;
        botText.multiline = true;
        botText.wordWrap = true;
        botText.embedFonts = true;
        botText.selectable = false;
        botText.x = 0;
        botText.y = 0;
        botText.width = 180;
        botText.height = 100;
        botText.x = 190;
        botText.y = 260;
        botText.htmlText = "Click me to begin exploring Whirled!";
        textLayer.addChild(botText);

        return textLayer;
    }

    protected var _tutorial :Tutorial;

    protected var _width :Number;
    protected var _height :Number;
    protected var _summary :String;
    protected var _textField :TextField;

    protected var _swirl :MovieClip;
    protected var _swirlState :int;
    protected var _transition :String;
    protected var _scenes :Object;

    protected var _todoDeleteMe :DisplayObject;

    protected static const log :Log = Log.getLog(View);

    [Embed(source="../../rsrc/whatsnext.swf", mimeType="application/octet-stream")]
    protected static const SWIRL :Class;

    [Embed(source="../../rsrc/SunnySide.ttf", fontName="SunnySide", fontWeight="Regular")]
    protected static const FONT :Class;

    protected static const SCN_APPEAR :String = "appear";
    protected static const SCN_MAXIMIZE :String = "maximize";
    protected static const SCN_MINIMIZE :String = "minimize";
    protected static const SCN_IDLE :String = "idle";
    protected static const SCN_LOOKATME :String = "lookatme";
    protected static const SCN_GOODJOB :String = "goodjob";
    protected static const SCN_TEXTBOX :String = " textbox ";
}
}
