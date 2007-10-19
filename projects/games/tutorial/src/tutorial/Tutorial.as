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

import com.whirled.AVRGameControl;
import com.whirled.AVRGameControlEvent;

[SWF(width="520", height="100")]
public class Tutorial extends Sprite
{
    public static var log :Log = Log.getLog(Tutorial);

    public function Tutorial ()
    {
        root.loaderInfo.addEventListener(Event.COMPLETE, complete);

        // immediately set up the control and listen for all relevant events
        _control = new AVRGameControl(this);
        _control.addEventListener(
            AVRGameControlEvent.PLAYER_PROPERTY_CHANGED, playerPropertyChanged);
        _control.addEventListener(
            AVRGameControlEvent.MESSAGE_RECEIVED, messageReceived);
        _control.addEventListener(
            AVRGameControlEvent.QUEST_STATE_CHANGED, questStateChanged);

        // create but do not initialize the view
        _view = new View(this);
    }

    protected function complete (event :Event) :void
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // when we finish loading we know our own dimensions
        var width :Number = this.loaderInfo.width;
        var height :Number = this.loaderInfo.height;

        // then initialize the actual view and pad it a little
        _view.init(width, height);
        addChild(_view);

        // now surrender control until we find out whether or not we're minimized
    }

    protected function handleUnload (event :Event) :void
    {
    }

    protected function messageReceived (event :AVRGameControlEvent) :void
    {
        // the only message we expect to receive is the special tutorialEvent one
        if (event.name != "tutorialEvent") {
            return;
        }

        var step :int = getStep();

//        log.debug("Tutorial event [name=" + event.value + ", step=" + step + "]");

        if (event.value == "willMinimize") {
            _clientIsMinimized = true;

        } else if (event.value == "willUnminimize") {
            _clientIsMinimized = false;
            initialize();

        } else if (event.value == Quest.getQuest(step).trigger) {
            _control.setPlayerProperty(PROP_STEP_COMPLETED, step, true);

        } else {
            log.warning("Unknown tutorial event: " + event.value);
        }
    }

    protected function playerPropertyChanged (event: AVRGameControlEvent) :void
    {
        // log.debug("property changed: " + event.name + "=" + event.value);
        if (event.name == PROP_STEP_COMPLETED) {
            _view.gotoSwirlState(View.SWIRL_BOUNCY);

        } else if (event.name == PROP_TUTORIAL_STEP) {
            // we have arrived at a new tutorial step; reinitialize our state
            initialize();
        }
    }

    protected function questStateChanged (event :AVRGameControlEvent) :void
    {
//        log.debug("quest state changed [id=" + event.name + ", value=" + event.value + "]");
        // we've acquired a new active quest or dropped an old one; update our display

        var step :int = getStep();
        if (event.value) {
            // if this was the very first step, get rid of the big swirl
            if (step == 0) {
                _view.gotoSwirlState(View.SWIRL_IDLE);
            }
            // accepting a new quest triggers the summary box
//            _view.popupSummary(step);
            return;
        }
        // else the player dropped the quest or clicked the completion popup's "OK" button
        var quest :Quest = Quest.getQuest(step);
        if (event.name == quest.questId && testCompletedStep(step)) {
            // looks like it was completed
            _activeQuest = null;
            _control.setPlayerProperty(PROP_STEP_COMPLETED, null, true);
            _control.setPlayerProperty(PROP_TUTORIAL_STEP, getStep() + 1, true);
            return;
        }
        log.warning("Deactivation of unexpected quest [questId=" + event.name +
                    ", current=" + quest.questId + "]");
        return;
    }

    protected function initialize () :void
    {
        if (_clientIsMinimized) {
            // sanity check - should not really happen
            return;
        }
        // figure out which quest we ought to be on
        var step :int = getStep();
        if (step >= Quest.getQuestCount()) {
            // we're done! TODO: deactivate us somehow
            return;
        }

        var quest :Quest = Quest.getQuest(step);

        // check against our current active quests
        var quests :Array = _control.getActiveQuests();
        for (var ii :int = 0; ii < quests.length; ii ++) {
            var tuple :Array = quests[ii];
            if (tuple[0] == quest.questId) {
                _activeQuest = quest.questId;
                if (testCompletedStep(step)) {
                    _view.gotoSwirlState(View.SWIRL_BOUNCY);
                } else {
                    _view.gotoSwirlState(View.SWIRL_IDLE);
                }
                return;
            }
        }
        // we're not on the right quest, signal the view
        _view.gotoSwirlState(step == 0 ? View.SWIRL_HUGE : View.SWIRL_BOUNCY);
    }

    public function swirlClicked (swirlState :int) :void
    {
        var step :int = getStep();
        var quest :Quest = Quest.getQuest(step);

        if (testCompletedStep(step)) {
            _control.completeQuest(quest.questId, quest.outro, quest.payout);
            return;
        }
        if (_activeQuest) {
            // TODO: instruct view to display summary box
            return;
        }
        if (step == 0 && swirlState != View.SWIRL_HUGE) {
            log.warning("Eek, unexpected huge swirl click [step=" + getStep() + "]");
        }
        _control.offerQuest(quest.questId, quest.intro, quest.status);
        _view.gotoSwirlState(View.SWIRL_IDLE);
    }

    protected function getStep () :int
    {
        return int(_control.getPlayerProperty(PROP_TUTORIAL_STEP));
    }

    protected function testCompletedStep (step :int) :Boolean
    {
        var tmp :Object = _control.getPlayerProperty(PROP_STEP_COMPLETED);
        return tmp != null && int(tmp) == step;
    }

    protected var _activeQuest :String;
    protected var _control :AVRGameControl;
    protected var _clientIsMinimized :Boolean = true;
    protected var _view :View;

    protected static const PROP_STEP_COMPLETED :String = "stepCompleted";
    protected static const PROP_TUTORIAL_STEP :String = "tutorialStep";
}
}


