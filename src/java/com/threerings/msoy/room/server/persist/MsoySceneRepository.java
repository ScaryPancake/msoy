//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.Timestamp;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.Key;
import com.samskivert.depot.KeySet;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.SchemaMigration;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;
import com.samskivert.depot.expression.EpochSeconds;
import com.samskivert.depot.expression.FunctionExp;
import com.samskivert.depot.expression.LiteralExp;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.operator.Arithmetic;
import com.samskivert.depot.operator.Conditionals.*;
import com.samskivert.depot.operator.Logic;

import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.util.Name;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.server.persist.SceneRepository;
import com.threerings.whirled.util.NoSuchSceneException;
import com.threerings.whirled.util.UpdateList;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.server.persist.CountRecord;
import com.threerings.msoy.server.persist.HotnessConfig;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.RecordFunctions;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.server.persist.AudioRepository;
import com.threerings.msoy.item.server.persist.DecorRecord;
import com.threerings.msoy.item.server.persist.DecorRepository;
import com.threerings.msoy.item.server.persist.ItemRecord;

import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.room.data.SceneOwnershipUpdate;
import com.threerings.msoy.room.server.RoomExtras;

import static com.threerings.msoy.Log.log;

/**
 * Provides scene storage services for the msoy server.
 */
@Singleton @BlockingThread
public class MsoySceneRepository extends DepotRepository
    implements SceneRepository
{
    @Inject public MsoySceneRepository (PersistenceContext ctx)
    {
        super(ctx);

        _ratingRepo = new RatingRepository(
            ctx, SceneRecord.SCENE_ID, SceneRecord.RATING_SUM, SceneRecord.RATING_COUNT) {
            @Override
            protected Class<? extends PersistentRecord> getTargetClass () {
                return SceneRecord.class;
            }
            @Override
            protected Class<RatingRecord> getRatingClass () {
                return coerceRating(SceneRatingRecord.class);
            }
        };

        ctx.registerMigration(SceneRecord.class, new SchemaMigration.Drop(12, "audioId"));
        ctx.registerMigration(SceneRecord.class, new SchemaMigration.Drop(12, "audioMediaHash"));
        ctx.registerMigration(SceneRecord.class, new SchemaMigration.Drop(12, "audioMediaType"));
        ctx.registerMigration(SceneRecord.class, new SchemaMigration.Drop(12, "audioVolume"));
    }

    /**
     * Returns the total number of scenes in the repository.
     */
    public int getSceneCount ()
    {
        return load(CountRecord.class, new FromOverride(SceneRecord.class)).count;
    }

    /**
     * Returns the number of rooms owned by the specified member.
     */
    public int getRoomCount (int memberId)
    {
        Where where = new Where(
            new Logic.And(new Equals(SceneRecord.OWNER_TYPE, MsoySceneModel.OWNER_TYPE_MEMBER),
                          new Equals(SceneRecord.OWNER_ID, memberId)));
        return load(CountRecord.class, new FromOverride(SceneRecord.class), where).count;
    }

    /**
     * Retrieve a list of all the scenes that the user owns.
     */
    public List<SceneRecord> getOwnedScenes (byte ownerType, int memberId)
    {
        Where where = new Where(new Logic.And(new Equals(SceneRecord.OWNER_TYPE, ownerType),
                                              new Equals(SceneRecord.OWNER_ID, memberId)));
        return findAll(SceneRecord.class, where);
    }

    /**
     * Retrieve a list of all the member scenes that the user directly owns.
     */
    public List<SceneRecord> getOwnedScenes (int memberId)
    {
        return getOwnedScenes(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    public RatingRepository getRatingRepository ()
    {
        return _ratingRepo;
    }

    /**
     * Return the scene name for the specified id, or null (no exception) if the scene
     * doesn't exist.
     */
    public String identifyScene (int sceneId)
    {
        // TODO: use a @Computed record?
        SceneRecord scene = load(SceneRecord.class, SceneRecord.getKey(sceneId));
        return (scene == null) ? null : scene.name;
    }

    /**
     * Publishes a scene, marking it as a Whirled Tourist trap.
     */
    public void publishScene (int sceneId)
    {
        updatePartial(SceneRecord.class, sceneId,
            SceneRecord.LAST_PUBLISHED, new Timestamp(System.currentTimeMillis()));
    }

    /**
     * Given a list of scene ids, return a map containing the current names, indexed by scene id.
     */
    public IntMap<String> identifyScenes (Set<Integer> sceneIds)
    {
        IntMap<String> names = IntMaps.newHashIntMap();
        // TODO: use a @Computed record?
        for (SceneRecord scene : loadAll(SceneRecord.class, sceneIds)) {
            names.put(scene.sceneId, scene.name);
        }
        return names;
    }

    // from interface SceneRepository
    public void applyAndRecordUpdate (SceneModel model, SceneUpdate update)
    {
        // ensure that the update has been applied
        int targetVers = update.getSceneVersion() + update.getVersionIncrement();
        if (model.version != targetVers) {
            log.warning("Refusing to apply update, wrong version [want=" + model.version +
                        ", have=" + targetVers + ", update=" + update + "].");
            return;
        }
        // now pass it to the accumulator who will take it from here
        _accumulator.add(update);
    }

    // from interface SceneRepository
    public SceneModel loadSceneModel (int sceneId)
        throws NoSuchSceneException
    {
        SceneRecord scene = loadScene(sceneId);
        if (scene == null) {
            throw new NoSuchSceneException(sceneId);
        }
        MsoySceneModel model = scene.toSceneModel();

        // populate the name of the owner
        switch (model.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            model.ownerName = _memberRepo.loadMemberName(model.ownerId);
            break;

        case MsoySceneModel.OWNER_TYPE_GROUP:
            GroupRecord grec = _groupRepo.loadGroup(model.ownerId);
            if (grec != null) {
                model.ownerName = grec.toGroupName();
                model.gameId = grec.gameId;
            }
            break;

        default:
            log.warning("Unable to populate owner name, unknown ownership type", new Exception());
            break;
        }

        // load up all of our furni data, specifically using a safe caching strategy
        List<SceneFurniRecord> records = findAll(
            SceneFurniRecord.class, CacheStrategy.RECORDS, Lists.newArrayList(
                new Where(SceneFurniRecord.SCENE_ID, sceneId)));
        List<FurniData> flist = Lists.newArrayList();
        for (SceneFurniRecord furni : records) {
            flist.add(furni.toFurniData());
        }
        model.furnis = flist.toArray(new FurniData[flist.size()]);

        // load up our room decor
        if (model.decor.itemId != 0) {
            DecorRecord record = _decorRepo.loadItem(model.decor.itemId);
            if (record != null) {
                model.decor = (Decor) record.toItem();
            }
        }
        if (model.decor.itemId == 0) { // still the default?
            // the scene specified no or an invalid decor, just load up the default
            model.decor = MsoySceneModel.defaultMsoySceneModelDecor();
        }

        return model;
    }

    // from interface SceneRepository
    public UpdateList loadUpdates (int sceneId)
    {
        return new UpdateList(); // we don't do scene updates
    }

    // from interface SceneRepository
    public Object loadExtras (int sceneId, SceneModel model)
    {
        MsoySceneModel mmodel = (MsoySceneModel) model;
        List<ItemIdent> memoryIds = Lists.newArrayList();
        for (FurniData furni : mmodel.furnis) {
            if (furni.itemType != Item.NOT_A_TYPE) {
                memoryIds.add(furni.getItemIdent());
            }
        }
        if (mmodel.decor != null) {
            memoryIds.add(mmodel.decor.getIdent());
        }
        RoomExtras extras = new RoomExtras();
        if (memoryIds.size() > 0) {
            extras.memories = _memoryRepo.loadMemories(memoryIds);
        }
        extras.playlist = Lists.transform(_audioRepo.loadItemsByLocation(sceneId),
            new ItemRecord.ToItem<Audio>());
        return extras;
    }

    /**
     * Load the SceneRecord for the given sceneId
     */
    public SceneRecord loadScene (int sceneId)
    {
        return load(SceneRecord.class, SceneRecord.getKey(sceneId));
    }

    /**
     * Load the SceneRecords for the given sceneIds.
     */
    public List<SceneRecord> loadScenes (Collection<Integer> sceneIds)
    {
        return loadAll(SceneRecord.class, sceneIds);
    }

    /**
     * Loads "new and hot" rooms...
     */
    public List<SceneRecord> loadScenes (int offset, int rows)
    {
        List<QueryClause> clauses = Lists.newArrayList();
        clauses.add(new Limit(offset, rows));

        List<SQLExpression> exprs = Lists.newArrayList();
        List<OrderBy.Order> orders = Lists.newArrayList();

        // only load public, published rooms
        clauses.add(new Where(new Logic.And(
                new Logic.Not(new IsNull(SceneRecord.LAST_PUBLISHED)),
                new Equals(SceneRecord.ACCESS_CONTROL, MsoySceneModel.ACCESS_EVERYONE)
        )));

        exprs.add(NEW_AND_HOT_ORDER);
        orders.add(OrderBy.Order.DESC);

        clauses.add(new OrderBy(exprs.toArray(new SQLExpression[exprs.size()]),
                                orders.toArray(new OrderBy.Order[orders.size()])));

        return findAll(SceneRecord.class, clauses);
    }

    /**
     * Returns the canonical snapshot image for the specified scene or null if it has none.
     */
    public MediaDesc loadSceneSnapshot (int sceneId)
    {
        SceneRecord scene = loadScene(sceneId);
        return (scene == null) ? null : scene.getSnapshotFull();
    }

    /**
     * Saves the specified update to the database.
     */
    protected void persistUpdate (SceneUpdate update)
    {
        int finalVersion = update.getSceneVersion() + update.getVersionIncrement();
        persistUpdates(Collections.singleton(update), finalVersion);
    }

    /**
     * Saves the provided set of updates to the database. Errors applying any of the individual
     * updates are caught and logged so that one update application failure does not prevent
     * subsequent updates from failing.
     */
    protected void persistUpdates (Iterable<? extends SceneUpdate> updates, int finalVersion)
    {
        int sceneId = 0;
        for (SceneUpdate update : updates) {
            sceneId = update.getSceneId();
            try {
                applyUpdate(update);
            } catch (Exception e) {
                log.warning("Failed to apply scene update " + update +
                        " from " + StringUtil.toString(updates) + ".", e);
            }
        }
        if (sceneId != 0) {
            try {
                updatePartial(SceneRecord.class, sceneId, SceneRecord.VERSION, finalVersion);
            } catch (Exception e) {
                log.warning("Failed to update scene to final version", "id", sceneId,
                        "fvers", finalVersion, e);
            }
        }
    }

    /**
     * Applies an update that adds, removes or changes furni.
     */
    protected void applyUpdate (SceneUpdate update)
    {
        if (update instanceof FurniUpdate.Add) {
            insert(new SceneFurniRecord(update.getSceneId(), ((FurniUpdate)update).data));

        } else if (update instanceof FurniUpdate.Change) {
            update(new SceneFurniRecord(update.getSceneId(), ((FurniUpdate)update).data));

        } else if (update instanceof FurniUpdate.Remove) {
            delete(SceneFurniRecord.class,
                   SceneFurniRecord.getKey(update.getSceneId(), ((FurniUpdate)update).data.id));

        } else if (update instanceof SceneAttrsUpdate) {
            SceneAttrsUpdate scup = (SceneAttrsUpdate)update;
            updatePartial(
                SceneRecord.class, update.getSceneId(),
                SceneRecord.NAME, scup.name,
                SceneRecord.ACCESS_CONTROL, scup.accessControl,
                SceneRecord.PLAYLIST_CONTROL, scup.playlistControl,
                SceneRecord.DECOR_ID, scup.decor.itemId,
                SceneRecord.ENTRANCE_X, scup.entrance.x,
                SceneRecord.ENTRANCE_Y, scup.entrance.y,
                SceneRecord.ENTRANCE_Z, scup.entrance.z);

        } else if (update instanceof SceneOwnershipUpdate) {
            SceneOwnershipUpdate sou = (SceneOwnershipUpdate)update;
            Map<ColumnExp,Object> updates = Maps.newHashMap();
            updates.put(SceneRecord.OWNER_TYPE, sou.ownerType);
            updates.put(SceneRecord.OWNER_ID, sou.ownerId);
            if (sou.lockToOwner) {
                updates.put(SceneRecord.ACCESS_CONTROL, MsoySceneModel.ACCESS_OWNER_ONLY);
            }
            updatePartial(SceneRecord.class, update.getSceneId(), updates);

        } else {
            log.warning("Unable to apply unknown furni update", "class", update.getClass(),
                        "update", update);
        }
    }

    /**
     * Creates a new blank room for the specified member.
     *
     * @param ownerType may be an individual member or a group.
     * @param portalAction to where to link the new room's door.
     * @param firstTime whether this the first room this owner has created.
     */
    public SceneRecord createBlankRoom (
        byte ownerType, int ownerId, String roomName, String portalAction, boolean firstTime)
    {
        // determine the scene id to clone
        SceneRecord.Stock stock = null;
        switch (ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            stock = firstTime ? SceneRecord.Stock.FIRST_MEMBER_ROOM :
                SceneRecord.Stock.EXTRA_MEMBER_ROOM;
            break;
        case MsoySceneModel.OWNER_TYPE_GROUP:
            stock = firstTime ? SceneRecord.Stock.FIRST_GROUP_HALL :
                 SceneRecord.Stock.EXTRA_GROUP_HALL;
            break;
        }

        // load up the stock scene
        SceneRecord record = null;
        if (stock != null) {
            record = load(SceneRecord.class, SceneRecord.getKey(stock.getSceneId()));
        }

        // if we fail to load a stock scene, just create a totally blank scene
        if (record == null) {
            log.info("Unable to find stock scene to clone", "type", ownerType);
            MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
            model.ownerType = ownerType;
            model.ownerId = ownerId;
            model.version = 1;
            model.name = roomName;
            return insertScene(model);
        }

        // fill in our new bits and write out our new scene
        record.accessControl = MsoySceneModel.ACCESS_EVERYONE;
        record.ownerType = ownerType;
        record.ownerId = ownerId;
        record.name = roomName;
        record.version = 1;
        record.sceneId = 0;
        insert(record);

        // now load up furni from the stock scene
        Where where = new Where(SceneFurniRecord.SCENE_ID, stock.getSceneId());
        for (SceneFurniRecord furni : findAll(SceneFurniRecord.class, where)) {
            furni.sceneId = record.sceneId;
            // if the scene has a portal pointing to the default public space; rewrite it to point
            // to our specified new portal destination (if we have one)
            if (portalAction != null && furni.actionType == FurniData.ACTION_PORTAL &&
                furni.actionData != null && furni.actionData.startsWith(
                    SceneRecord.Stock.PUBLIC_ROOM.getSceneId() + ":")) {
                furni.actionData = portalAction;
            }
            insert(furni);
        }

        return record;
    }

    /** Loads the room properties. */
    public List<RoomPropertyRecord> loadProperties (int ownerId, int sceneId)
    {
        return findAll(RoomPropertyRecord.class, new Where(
            RoomPropertyRecord.OWNER_ID, ownerId,
            RoomPropertyRecord.SCENE_ID, sceneId));
    }

    /** Saves a room property, deleting if the value is null. */
    public void storeProperty (RoomPropertyRecord record)
    {
        if (record.value == null) {
            delete(record);
        } else {
            store(record);
        }
    }

    public void setCanonicalImage (int sceneId, byte[] canonicalHash, byte canonicalType,
        byte[] thumbnailHash, byte thumbnailType)
    {
        updatePartial(SceneRecord.class, sceneId,
            SceneRecord.CANONICAL_IMAGE_HASH, canonicalHash,
            SceneRecord.CANONICAL_IMAGE_TYPE, canonicalType,
            SceneRecord.THUMBNAIL_HASH, thumbnailHash,
            SceneRecord.THUMBNAIL_TYPE, thumbnailType);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        // delete all scenes owned by these members
        List<Key<SceneRecord>> skeys = findAllKeys(
            SceneRecord.class, false, new Where(new In(SceneRecord.OWNER_ID, memberIds)));
        if (!skeys.isEmpty()) {
            deleteAll(SceneRecord.class, KeySet.newKeySet(SceneRecord.class, skeys));
            // delete all furni from all of those scenes
            List<Integer> scids = Lists.transform(skeys, RecordFunctions.<SceneRecord>getIntKey());
            deleteAll(SceneFurniRecord.class, new Where(new In(SceneFurniRecord.SCENE_ID, scids)));
        }
        // delete all scene ratings by these members
        _ratingRepo.purgeMembers(memberIds);
    }

    /**
     * Insert a new scene, with furni and all, into the database and return the newly assigned
     * sceneId.
     */
    protected SceneRecord insertScene (MsoySceneModel model)
    {
        SceneRecord scene = new SceneRecord(model);
        insert(scene);
        for (FurniData data : model.furnis) {
            insert(new SceneFurniRecord(scene.sceneId, data));
        }
        return scene;
    }

    protected void checkCreateStockScene (SceneRecord.Stock stock)
    {
        // if it's already created, we're good to go
        if (load(SceneRecord.class, SceneRecord.getKey(stock.getSceneId())) != null) {
            return;
        }

        MsoySceneModel model = MsoySceneModel.blankMsoySceneModel();
        model.sceneId = stock.getSceneId();
        model.version = 1;
        model.name = stock.getName();

        if (stock == SceneRecord.Stock.PUBLIC_ROOM) {
            // set it up to be owned by group 1
            model.ownerType = MsoySceneModel.OWNER_TYPE_GROUP;
            model.ownerId = 1;

        } else {
            // add a door to the PUBLIC_ROOM
            FurniData f = new FurniData();
            f.id = 1;
            f.media = new MediaDesc("e8b660ec5aa0aa30dab46b267daf3b80996269e7.swf");
            f.loc = new MsoyLocation(1, 0, 0.5, 0);
            f.scaleX = 1.4f;
            f.actionType = FurniData.ACTION_PORTAL;
            f.actionData = SceneRecord.Stock.PUBLIC_ROOM.getSceneId() + ":" +
                SceneRecord.Stock.PUBLIC_ROOM.getName();
            model.addFurni(f);
        }

        log.info("Creating stock scene " + stock + ".");
        insertScene(model);
    }

    @Override // from DepotRepository
    protected void init ()
    {
        super.init();

        // create our stock scenes if they are not yet created
        for (SceneRecord.Stock stock : SceneRecord.Stock.values()) {
            checkCreateStockScene(stock);
        }

        // initialize our update accumulator
        _accumulator.init(this);
    }

    protected static SQLExpression getRatingExpression ()
    {
        // TODO: PostgreSQL flips out when you CREATE INDEX using a prepared statement
        // TODO: with parameters. So we trick Depot using a literal expression here. :/
        return new Arithmetic.Div(
            SceneRecord.RATING_SUM,
            new FunctionExp("GREATEST", SceneRecord.RATING_COUNT, new LiteralExp("1.0")));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(SceneRecord.class);
        classes.add(SceneFurniRecord.class);
        classes.add(RoomPropertyRecord.class);
        classes.add(SceneRatingRecord.class);
    }

    protected RatingRepository _ratingRepo;

    // dependencies
    @Inject protected AudioRepository _audioRepo;
    @Inject protected DecorRepository _decorRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MemoryRepository _memoryRepo;
    @Inject protected UpdateAccumulator _accumulator;

    /** Order for New & Hot. If you change this, also migrate the {@link SceneRecord} index. */
    protected static final SQLExpression NEW_AND_HOT_ORDER =
        new Arithmetic.Add(getRatingExpression(), new Arithmetic.Div(
            new EpochSeconds(SceneRecord.LAST_PUBLISHED),
            // TODO: PostgreSQL flips out when you CREATE INDEX using a prepared statement
            // TODO: with parameters. So we trick Depot using a literal expression here. :/
            new LiteralExp("" + HotnessConfig.DROPOFF_SECONDS)));
}
