package {
import flash.display.Sprite;

import flash.geom.Matrix;
import flash.geom.Point;

import flash.events.Event;

import flash.utils.describeType;

import mx.core.MovieClipAsset;

import com.threerings.util.Line;

public class Kart extends KartSprite
{
    public function Kart (kartType :String, camera :Camera, ground :Ground) 
    {
        super(kartType, ground);
        _camera = camera;
        ground.getScenery().registerKart(this);

        addEventListener(Event.ENTER_FRAME, enterFrame);
    }

    /**
     * Get an update object to send to the other clients.
     */
    public function getUpdate () :Object
    {
        var loc :Point = _ground.getKartLocation();
        return {
            posX: loc.x,
            posY: loc.y,
            angle: _camera.angle,
            speed: _currentSpeed,
            movement: _movement
        };
    }

    public function moveForward (moving :Boolean) :void 
    { 
        keyAction(moving, MOVEMENT_FORWARD);
    }

    public function moveBackward (moving :Boolean) :void
    {
        keyAction(moving, MOVEMENT_BACKWARD);
    }

    public function turnLeft (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_LEFT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function turnRight (turning :Boolean) :void
    {
        keyAction(turning, MOVEMENT_RIGHT);
        if (!turning) keyAction(false, MOVEMENT_DRIFT);
    }

    public function jump () :void
    {
        if (_jumpFrameCount == 0) {
            _jumpFrameCount = JUMP_DURATION;
            y -= JUMP_HEIGHT;
        }
    }

    public function setFinishLine (line :Line) :void
    {
        _finishLine = line;
    }

    protected function enterFrame (event :Event) :void
    {
        // update camera and kart angles
        var viewAcceleration :int = Math.abs(_currentViewAngle) > TURN_VIEW_ANGLE ? 
            VIEW_ACCELERATION * 3 : VIEW_ACCELERATION;
        if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
            var maxViewAngle :int = _movement & MOVEMENT_DRIFT ? DRIFT_VIEW_ANGLE : 
                TURN_VIEW_ANGLE;
            if (_movement & MOVEMENT_RIGHT) {
                _currentTurnAngle = Math.min(MAX_TURN_ANGLE, _currentTurnAngle + TURN_ACCELERATION);
                _currentViewAngle = Math.min(maxViewAngle, _currentViewAngle + viewAcceleration);
            } else {
                _currentTurnAngle = Math.max(-MAX_TURN_ANGLE, _currentTurnAngle - 
                    TURN_ACCELERATION);
                _currentViewAngle = Math.max(-maxViewAngle, _currentViewAngle - viewAcceleration);
            }
        } else {
            if (_currentTurnAngle > 0) {
                _currentTurnAngle = Math.max(0, _currentTurnAngle - TURN_ACCELERATION);
            } else if (_currentTurnAngle < 0) {
                _currentTurnAngle = Math.min(0, _currentTurnAngle + TURN_ACCELERATION);
            }
            if (_currentViewAngle > 0) {
                _currentViewAngle = Math.max(0, _currentViewAngle - viewAcceleration);
            } else if (_currentViewAngle < 0) {
                _currentViewAngle = Math.min(0, _currentViewAngle + viewAcceleration);
            }
        }
        if (_currentSpeed > 0) {
            _camera.angle = (_camera.angle + _currentTurnAngle) % (Math.PI * 2);
        } else if (_currentSpeed < 0) {
            _camera.angle = (_camera.angle - _currentTurnAngle + Math.PI * 2) % (Math.PI * 2);
        }

        // update turn animation
        var frame :int = Math.abs(_currentViewAngle) as int;
        if (_currentViewAngle > 0) {
            frame = 360 - frame;
        } else if (_currentViewAngle == 0) {
            frame = 1;
        }
        if (_kart.currentFrame != frame) {
            _kart.gotoAndStop(frame);
        }

        var oldPos :Point = _camera.position;
        _camera.position = calculateNewPosition(_camera.position, _camera.angle,    
            _ground.getKartLocation());
        var intersection :int = _finishLine.intersects(new Line(oldPos, _camera.position));
        if (intersection == Line.INTERSECTION_NORTH) {
            Log.getLog(this).debug("crossed finish line NORTH");
        } else if (intersection == Line.INTERSECTION_SOUTH) {
            Log.getLog(this).debug("crossed finish line SOUTH");
        }

        // deal with a jump
        if (_jumpFrameCount > 0) {
            _jumpFrameCount--;
            if (_jumpFrameCount == 0) {
                y += JUMP_HEIGHT;
                if (_movement & (MOVEMENT_RIGHT | MOVEMENT_LEFT)) {
                    keyAction(true, MOVEMENT_DRIFT);
                }
            }
        }

        var collides :Object = _ground.getScenery().getCollidingObject();
        if (collides != null && collides.sceneryType == Scenery.BONUS) {
            _ground.getScenery().removeObject(collides);
        }
    }

    protected function keyAction (inMotion :Boolean, flag :int) :void
    {
        if (inMotion) {
            _movement |= flag;
        } else {
            _movement &= ~flag;
        }
    }

    /** turning constants */
    protected static const TURN_VIEW_ANGLE :int = 15; // in degrees
    protected static const DRIFT_VIEW_ANGLE :int = 45; // in degrees
    protected static const VIEW_ACCELERATION :int = 4; // degrees per frame

    /** values to control jumping */
    protected static const JUMP_DURATION :int = 3;
    protected static const JUMP_HEIGHT :int = 15;
   
    /** reference to the camera object */
    protected var _camera :Camera;

    /** The current amount we are adding or subtracting from the kart's turn angle */
    protected var _currentTurnAngle :Number = 0;

    /** The current angle we are viewing our own kart at */
    protected var _currentViewAngle :Number = 0;

    protected var _finishLine :Line;
}
}
