package {

import mx.core.MovieClipAsset;
import flash.media.Sound;
import flash.media.SoundTransform;

public class WarthogShipType extends ShipType
{
    public function WarthogShipType () :void
    {
        super("Warthog", 1.75, 0.01, -0.005, 0.975, 0.825, 0.25, 1.5);

        // Turn off the sound for the movie til we need it.
        ENGINE_MOV.soundTransform = Sounds.OFF;
    }

    // Shooting sounds.
    [Embed(source="rsrc/ships/warthog/beam.mp3")]
    protected static var beamSound :Class;

    public const BEAM :Sound = Sound(new beamSound());

    [Embed(source="rsrc/ships/warthog/beam_tri.mp3")]
    protected static var triBeamSound :Class;

    public const TRI_BEAM :Sound = Sound(new triBeamSound());

    // Ship spawning
    [Embed(source="rsrc/ships/warthog/spawn.mp3")]
    protected static var spawnSound :Class;

    public const SPAWN :Sound = Sound(new spawnSound());

    // Looping sound - this is a movieclip to make the looping work without
    //  hiccups.  This is pretty hacky - we can't control the looping sound
    //  appropriately, so we just manipulate the volume.  So, the sounds are
    //  always running, just sometimes really quietly.  Bleh.

    // Engine hum.
    [Embed(source="rsrc/ships/warthog/engine_sound.swf")]
    public static var engineSound :Class;

    public const ENGINE_MOV :MovieClipAsset =
        MovieClipAsset(new engineSound());

    // Animations
    [Embed(source="rsrc/ships/warthog/ship.swf#ship_movie_01_alt")]
    public const SHIP_ANIM :Class;

    [Embed(source="rsrc/ships/warthog/ship_shield.swf")]
    public const SHIELD_ANIM :Class;

    [Embed(source="rsrc/ships/warthog/ship_explosion_big.swf")]
    public const EXPLODE_ANIM :Class;

    [Embed(source="rsrc/ships/warthog/beam.swf")]
    public const SHOT_ANIM :Class;

}
}
