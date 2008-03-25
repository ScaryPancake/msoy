//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all the information needed to launch a particular game.
 */
public class LaunchConfig
    implements IsSerializable
{
    /** A constant used to denote in-world Flash games. */
    public static final int FLASH_IN_WORLD = 0;

    /** A constant used to denote lobbied Flash games. */
    public static final int FLASH_LOBBIED = 1;

    /** A constant used to denote single player Flash games. */
    public static final int FLASH_SOLO = 2;

    /** A constant used to denote Java games lobbied in Flash. */
    public static final int JAVA_FLASH_LOBBIED = 3;

    /** A constant used to denote Java games lobbied themselves (in Java). */
    public static final int JAVA_SELF_LOBBIED = 4;

    /** A constant used to denote single player Java games. */
    public static final int JAVA_SOLO = 5;

    /** The unique identifier for the game in question. */
    public int gameId;

    /** The type of this game (see above constants). */
    public int type;

    /** The display name of this game. */
    public String name;

    /** The path (relative to the resource URL) for the game client media (SWF or JAR). */
    public String gameMediaPath;

    /** The server to which the game should connect (if this is a multiplayer game). */
    public String server;

    /** The port on which the game should connect to the server (if this is a multiplayer game). */
    public int port;

    /** The port on which the game should connect to the server for HTTP requests (used by Java
     * which must connect back to the game server for its game jar file). */
    public int httpPort;

    /** If true, the (Java) game requires the signed client that includes the LWJGL libraries. */
    public boolean lwjgl;

    /** If a first-time guest requests a launch config, we assign them a guest id so that they can
     * log directly into the game server instead of waiting for their authentication with the world
     * server to complete and result in their being assigned a guest id. */
    public int guestId;

    /**
     * Creates a URL that can be used to communicate directly to the server represented by this
     * launch config.
     */
    public String getURL (String path)
    {
        String port = (httpPort == 80) ? "" : (":" + httpPort);
        return "http://" + server + port + path;
    }
}
