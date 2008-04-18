//
// $Id$

package com.threerings.msoy.applets.remixer {

import flash.events.Event;
import flash.events.TextEvent;

import mx.controls.CheckBox;
import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;
import mx.controls.Spacer;
import mx.controls.TextInput;

import mx.validators.NumberValidator;
import mx.validators.Validator;
import mx.validators.ValidationResult;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;

import com.whirled.remix.data.EditableDataPack;

public class DataEditor extends FieldEditor
{
    public function DataEditor (ctx :RemixContext, name :String)
    {
        var entry :Object = ctx.pack.getDataEntry(name);
        super(ctx, name, entry);

        _value = entry.value;
    }

    override protected function getUI (entry :Object) :Array
    {
        try {
            return Object(this)["setup" + entry.type](entry);
        } catch (err :Error) {
            // fall through to super
        }
        return super.getUI(entry);
    }

    protected function setupBoolean (entry :Object) :Array
    {
        var tog :CheckBox = new CheckBox();
        tog.selected = Boolean(entry.value);
        tog.addEventListener(Event.CHANGE, function (... ignored) :void {
            updateValue(tog.selected);
        });

        return [ tog, new Spacer(), tog ];
    }

    protected function setupString (entry :Object, validator :Validator = null) :Array
    {
        var label :Label = new Label();
        label.setStyle("color", NAME_AND_VALUE_COLOR);
        updateLabel(label, entry);
        _ctx.pack.addEventListener(EditableDataPack.DATA_CHANGED,
            function (event :ValueEvent) :void {
                if (event.value === entry.name) {
                    updateLabel(label, _ctx.pack.getDataEntry(entry.name));
                }
            });

        var dataEditor :DataEditor = this;
        var change :CommandButton = createEditButton(function () :void {
            new PopupEditor(dataEditor, _ctx.pack.getDataEntry(entry.name), validator);
        });

        return [ label, change, change ];
    }

    protected function setupNumber (entry :Object) :Array
    {
        var min :Number = Number(entry.min);
        var max :Number = Number(entry.max);

        // TODO: allow min/max either way, but allow a hint to specify to use the slider
        if (!isNaN(max) && !isNaN(min)) {
            var hslider :HSlider = new HSlider();
            hslider.minimum = min;
            hslider.maximum = max;
            hslider.value = Number(entry.value);
            hslider.addEventListener(Event.CHANGE, function (... ignored) :void {
                updateValue(hslider.value);
            });

            return [ hslider, new Spacer(), hslider ];

        } else {
            var val :NumberValidator = new NumberValidator();
            val.minValue = min;
            val.maxValue = max;
            return setupString(entry, val);
        }
    }

    protected function setupColor (entry :Object) :Array
    {
        var picker :ColorPicker = new ColorPicker();
        picker.selectedColor = uint(entry.value);
        _value = picker.selectedColor;
        picker.addEventListener(Event.CLOSE, function (... ignored) :void {
            updateValue(picker.selectedColor);
        });

        return [ picker, new Spacer(), picker ];
    }

    protected function updateLabel (label :Label, entry :Object) :void
    {
        label.text = (entry.value == null) ? "" : String(entry.value);
    }

    internal function updateValue (value :*) :void
    {
        if (value != _value) {
            _value = value;
            updateEntry();
        }
    }

    override protected function updateEntry () :void
    {
        _ctx.pack.setData(_name, _used.selected ? _value : null);
        setChanged();
    }

    protected var _value :*;
}
}
