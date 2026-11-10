package dev.coldpenguins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.InstrumentComponent;

import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxSong;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(ColdPenguins.ID)
public final class ColdPenguins {
    public static final String ID = "coldpenguins";
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ID);

    public static final Supplier<EntityType<Penguin>> PENGUIN = ENTITIES.register("penguin",
            () -> EntityType.Builder.of(Penguin::new, MobCategory.CREATURE)
                    .sized(0.75F, 1.0F).eyeHeight(0.85F).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "penguin"))));

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    static final Supplier<PenguinNestBlock> NEST = BLOCKS.register("penguin_nest",
            id -> new PenguinNestBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK, id)).strength(0.5F)
                    .sound(SoundType.STONE).noCollision().noOcclusion()));
    static final Supplier<BlockEntityType<PenguinNestBlock.Nest>> NEST_ENTITY = BLOCK_ENTITIES.register("penguin_nest",
            () -> new BlockEntityType<>(PenguinNestBlock.Nest::new, java.util.Set.of(NEST.get())));

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);

    public static final Supplier<Item> SANTA_HAT = ITEMS.register("santa_hat",
            id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).component(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.HEAD)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER)
                    .setAsset(ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(ID, "santa_hat"))).build())));

    public static final Supplier<Item> PENGUIN_EGG = ITEMS.register("penguin_egg",
            id -> new PenguinEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(16)));

    public static final Supplier<Item> PENGUIN_SPAWN_EGG = registerPenguinSpawnEgg("penguin_spawn_egg", PenguinSpecies.ORIGINAL);
    public static final Supplier<Item> EMPEROR_SPAWN_EGG = registerPenguinSpawnEgg("emperor_penguin_spawn_egg", PenguinSpecies.EMPEROR);
    public static final Supplier<Item> GENTOO_SPAWN_EGG = registerPenguinSpawnEgg("gentoo_penguin_spawn_egg", PenguinSpecies.GENTOO);
    public static final Supplier<Item> AFRICAN_SPAWN_EGG = registerPenguinSpawnEgg("african_penguin_spawn_egg", PenguinSpecies.AFRICAN);
    public static final Supplier<Item> BLUE_SPAWN_EGG = registerPenguinSpawnEgg("blue_penguin_spawn_egg", PenguinSpecies.BLUE);
    public static final Supplier<Item> ROCKHOPPER_SPAWN_EGG = registerPenguinSpawnEgg("rockhopper_penguin_spawn_egg", PenguinSpecies.ROCKHOPPER);
    public static final Supplier<Item> MACARONI_SPAWN_EGG = registerPenguinSpawnEgg("macaroni_penguin_spawn_egg", PenguinSpecies.MACARONI);

    private static Supplier<Item> registerPenguinSpawnEgg(String name, PenguinSpecies species) {
        return ITEMS.register(name, id -> {
            var data = new net.minecraft.nbt.CompoundTag();
            data.putString("Species", species.id);
            return new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))
                    .spawnEgg(PENGUIN.get()).component(DataComponents.ENTITY_DATA,
                            net.minecraft.world.item.component.TypedEntityData.of(PENGUIN.get(), data)));
        });
    }

    static final ResourceKey<JukeboxSong> PENGUIN_DANCE_SONG = ResourceKey.create(
            Registries.JUKEBOX_SONG, Identifier.fromNamespaceAndPath(ID, "penguin_dance"));
    public static final Supplier<Item> PENGUIN_DANCE_DISC = ITEMS.register("penguin_dance_disc",
            id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).jukeboxPlayable(PENGUIN_DANCE_SONG)));

    public static final Supplier<Item> PENGUIN_FEATHER = ITEMS.register("penguin_feather",
            id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    public static final Supplier<Item> YELLOW_PENGUIN_FEATHER = ITEMS.register("yellow_penguin_feather",
            id -> new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
    static final TagKey<Instrument> PENGUIN_HORNS = TagKey.create(Registries.INSTRUMENT,
            Identifier.fromNamespaceAndPath(ID, "penguin_horns"));
    public static final Supplier<Item> PENGUIN_HORN = ITEMS.register("penguin_horn",
            id -> new PenguinHornItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))
                    .stacksTo(1).useCooldown(7.0F)
                    .delayedComponent(DataComponents.INSTRUMENT, lookup -> new InstrumentComponent(
                            lookup.lookupOrThrow(Registries.INSTRUMENT).getOrThrow(ResourceKey.create(
                                    Registries.INSTRUMENT, Identifier.fromNamespaceAndPath(ID, "penguin")))))));
    public static final Supplier<Item> BLACK_TIPPED_ARROW = ITEMS.register("black_tipped_arrow",
            id -> new BlackTippedArrowItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));

    public static final Supplier<EntityType<BlackTippedArrow>> BLACK_ARROW_PROJECTILE =
            ENTITIES.register("black_tipped_arrow",
                    () -> EntityType.Builder.<BlackTippedArrow>of(BlackTippedArrow::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F).clientTrackingRange(4).updateInterval(20)
                            .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "black_tipped_arrow"))));

    public static final Supplier<EntityType<ThrownPenguinEgg>> THROWN_EGG = ENTITIES.register("penguin_egg",
            () -> EntityType.Builder.<ThrownPenguinEgg>of(ThrownPenguinEgg::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(ID, "penguin_egg"))));

    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, ID);

    public static final Supplier<SoundEvent> PENGUIN_TRILL = SOUNDS.register("penguin.trill",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ID, "penguin.trill")));
    public static final Supplier<SoundEvent> PENGUIN_HONK = SOUNDS.register("penguin.honk",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ID, "penguin.honk")));

    public static final Supplier<SoundEvent> PENGUIN_DANCE_SOUND = SOUNDS.register("music.penguin_dance",
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ID, "music.penguin_dance")));

    public ColdPenguins(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(this::addCreativeItems);
        modBus.addListener(this::registerDispenserBehavior);
        modBus.addListener(this::registerAttributes);
        modBus.addListener(this::registerSpawnPlacement);
        NeoForge.EVENT_BUS.addListener(this::eatCaughtFish);
        NeoForge.EVENT_BUS.addListener(this::hearPenguinDance);
    }

    private void hearPenguinDance(VanillaGameEvent event) {
        boolean playing = event.getVanillaEvent().is(GameEvent.JUKEBOX_PLAY);
        if (!playing && !event.getVanillaEvent().is(GameEvent.JUKEBOX_STOP_PLAY)) {
            return;
        }
        BlockPos jukeboxPos = BlockPos.containing(event.getEventPosition());
        if (playing && (!(event.getLevel().getBlockEntity(jukeboxPos) instanceof JukeboxBlockEntity jukebox)
                || !jukebox.getTheItem().is(PENGUIN_DANCE_DISC.get()))) {
            return;
        }
        for (Penguin penguin : event.getLevel().getEntitiesOfClass(Penguin.class,
                new AABB(jukeboxPos).inflate(64))) {
            if (jukeboxPos.closerToCenterThan(penguin.position(), 64.0)) {
                penguin.hearDanceMusic(jukeboxPos, playing);
            }
        }
    }

    private void registerDispenserBehavior(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerProjectileBehavior(BLACK_TIPPED_ARROW.get()));
    }

    private void eatCaughtFish(LivingDropsEvent event) {
        if (event.getSource().getDirectEntity() instanceof Penguin penguin && penguin.isInWater()
                && event.getEntity().isInWater() && Penguin.isPrey(event.getEntity())) {
            event.getDrops().removeIf(drop -> penguin.isFood(drop.getItem()));
        }
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.INGREDIENTS)) {
            event.accept(PENGUIN_EGG.get());
            event.accept(PENGUIN_FEATHER.get());
            event.accept(YELLOW_PENGUIN_FEATHER.get());
        } else if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
            event.accept(BLACK_TIPPED_ARROW.get());
            event.accept(SANTA_HAT.get());
        } else if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(PENGUIN_DANCE_DISC.get());
            event.accept(PENGUIN_HORN.get());
        } else if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) {
            event.accept(PENGUIN_SPAWN_EGG.get());
            event.accept(EMPEROR_SPAWN_EGG.get());
            event.accept(GENTOO_SPAWN_EGG.get());
            event.accept(AFRICAN_SPAWN_EGG.get());
            event.accept(BLUE_SPAWN_EGG.get());
            event.accept(ROCKHOPPER_SPAWN_EGG.get());
            event.accept(MACARONI_SPAWN_EGG.get());
        }
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(PENGUIN.get(), Penguin.createAttributes().build());
    }

    private void registerSpawnPlacement(RegisterSpawnPlacementsEvent event) {
        event.register(PENGUIN.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Penguin::checkLandSpawnRules,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
