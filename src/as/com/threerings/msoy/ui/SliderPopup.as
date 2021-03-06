//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.events.MouseEvent;
import flash.geom.Rectangle;

import mx.binding.utils.BindingUtils;
import mx.containers.Canvas;
import mx.controls.VSlider;
import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.events.SliderEvent;

import com.threerings.util.Util;

/** Background skin to be loaded from the style sheet. */
[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * Helper UI element for generic popup slider controls, such as volume and zoom.
 *
 * Slider control has "weak singleton" semantics - an instance can be created with
 * the 'new' keyword, but if any other instance previously existed, it will be closed
 * and removed from display prior to displaying the new one.
 */
public class SliderPopup extends Canvas
{
    /**
     * Helper method to either close the currently displayed SliderPopup and show this one,
     * or untoggle this one if it was already being displayed.
     */
    public static function toggle (
        trigger :DisplayObject, startValue :Number, bindTo :Function,
        sliderInitProps :Object = null) :void
    {
        if (_currentInstance != null) {
            const skipUp :Boolean = (_currentInstance._trigger == trigger);
            _currentInstance.destroy();
            // if we're toggling the same trigger, just pop down
            if (skipUp) {
                return;
            }
        }
        _currentInstance = new SliderPopup(trigger, startValue, bindTo, sliderInitProps);
        _currentInstance.show();
    }

    public function SliderPopup (
        trigger :DisplayObject, startValue :Number, bindTo :Function,
        sliderInitProps :Object = null)
    {
        styleName = "sliderPopup";

        _trigger = trigger;
        owner = DisplayObjectContainer(Application.application.systemManager);
        owner.addEventListener(MouseEvent.CLICK, handleOutsideClick);

        // Initialize the window
        var r : Rectangle = _trigger.getBounds(trigger.stage);
        width = 29;
        height = 120;
        x = r.x - 1;
        y = r.y - height;
        verticalScrollPolicy = horizontalScrollPolicy = ScrollPolicy.OFF;

        // Initialize slider
        _slider = new VSlider();
        //_slider.getThumbAt(0).scaleX = 2;
        _slider.sliderThumbClass = SliderPopupThumb;
        _slider.x = 4;
        _slider.y = 10;
        _slider.height = 80;
        Util.init(_slider, sliderInitProps, { minimum: 0, maximum: 1, liveDragging: true });
        _slider.value = startValue;

        BindingUtils.bindSetter(bindTo, _slider, "value");
        BindingUtils.bindSetter(sliderValueChanged, _slider, "value");
        _adjusted = false; // reset this to false because binding the setter calls once immediately

        addChild(_slider);

        _cursorOffCanvas = true;
        addEventListener(MouseEvent.ROLL_OUT, mouseOutHandler, false, 0, true);
        addEventListener(MouseEvent.ROLL_OVER, mouseOverHandler, false, 0, true);
        _slider.addEventListener(SliderEvent.THUMB_RELEASE, thumbReleaseHandler, false, 0, true);

        var but :TickButton = new TickButton(_slider);
        but.x = 4;
        but.y = 90;
        addChild(but);

        visible = false;
        owner.addChild(this);

        // Setting the skin happens after adding to the parent's draw list -
        // this ensures styles are properly loaded
        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);
    }

    /** Show the popup, by adding it to the application's display list,
     *  and register for appropriate events. */
    public function show () : void
    {
        visible = true;
    }

    /** Makes the pop-up invisible, but doesn't remove it. */
    public function hide () :void
    {
        visible = false;
    }

    /** Remove the pop-up from the display list, and unregister
     *  from any events. This should make the object ready to be GC'd,
     *  if there are no external references holding it. */
    public function destroy () :void
    {
        owner.removeEventListener(MouseEvent.CLICK, handleOutsideClick);
        owner.removeChild(this);
        if (this == _currentInstance) {
            _currentInstance = null;
        }
    }

    protected function sliderValueChanged (val :Number) :void
    {
        _adjusted = true;
    }

    // EVENT HANDLERS

    protected function handleOutsideClick (event :MouseEvent) :void
    {
        if (visible && _cursorOffCanvas) {
            destroy();
        }
    }

    /** Watch for the mouse leaving the area. */
    protected function mouseOutHandler (event : MouseEvent) : void
    {
        if (event.relatedObject != null) {
            _cursorOffCanvas = true;

            if (_adjusted && !event.buttonDown) {
                // We rolled out into room view, or other element - close up,
                // but don't delete the object, in case there are still events
                // queued up for it.
                destroy();
            }
        }
    }

    protected function mouseOverHandler (event :MouseEvent) :void
    {
        _cursorOffCanvas = false;
    }

    protected function thumbReleaseHandler (event :SliderEvent): void
    {
        if (_cursorOffCanvas) {
            destroy();
        }
    }

    /** The actual slider. */
    protected var _slider : VSlider;

    /** True if the mouse cursor has left the canvas area. */
    protected var _cursorOffCanvas :Boolean;

    /** Have we adjusted the slider? */
    protected var _adjusted :Boolean;

    /** The object that triggered this popup. */
    protected var _trigger :DisplayObject;

    /** Pointer to any other instance of the popup being currently displayed.
        Unfortunately, ActionScript doesn't support singleton semantics
        very well - instead, we just hold on to a reference, and hide any
        previous instance before displaying a new one. */
    protected static var _currentInstance : SliderPopup;
}
}
