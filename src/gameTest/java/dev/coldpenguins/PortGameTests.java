package dev.coldpenguins;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Integration checks executed in a real 26.3 server; excluded from the release JAR. */
@EventBusSubscriber(modid = ColdPenguins.ID)
public final class PortGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (!net.neoforged.neoforge.server.loading.ServerModLoader.isGameTestServer()) return;
        var environment = event.registerEnvironment(id("port"));
        register(event, environment, "saved_penguin", PortGameTests::savedPenguin);
        register(event, environment, "chick_feeding", PortGameTests::chickFeeding);
        register(event, environment, "chick_growth", PortGameTests::chickGrowth);
        register(event, environment, "nest_hatching", PortGameTests::nestHatching);
        register(event, environment, "item_components", PortGameTests::itemComponents);
        register(event, environment, "loot", PortGameTests::loot);
        register(event, environment, "underwater_arrow", PortGameTests::underwaterArrow);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ColdPenguins.ID, path);
    }

    private static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
            String name, Consumer<GameTestHelper> body) {
        event.registerTest(id(name), new GameTestInstance(new TestData<>(environment, id("empty"), 100, 0, true)) {
            @Override protected net.minecraft.network.chat.MutableComponent typeDescription() { return net.minecraft.network.chat.Component.literal(name); }
            @Override public net.minecraft.network.chat.Component describe() { return typeDescription(); }
            @Override public void run(GameTestHelper helper) { body.accept(helper); }
            @Override public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
        });
    }

    private static void savedPenguin(GameTestHelper helper) {
        var level = helper.getLevel();
        var penguin = Penguin.createChick(level);
        helper.assertTrue(penguin != null && penguin.isBaby(), "Egg creates a chick");
        CompoundTag legacy = new CompoundTag();
        legacy.putInt("Age", -1200);
        legacy.putLong("GrowsUpAt", level.getGameTime() + 500);
        legacy.putBoolean("GoldenPlumage", true);
        var family = java.util.UUID.randomUUID();
        legacy.store("Family", UUIDUtil.CODEC, family);
        penguin.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
        helper.assertTrue(penguin.isBaby() && penguin.hasGoldenPlumage(), "Legacy chick age and plumage load");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        penguin.addAdditionalSaveData(output);
        var saved = output.buildResult();
        helper.assertTrue(saved.read("Family", UUIDUtil.CODEC).orElseThrow().equals(family), "Family survives reload");
        helper.assertTrue(saved.getLongOr("GrowsUpAt", 0) == level.getGameTime() + 500, "Growth deadline survives reload");
        penguin.setBaby(false);
        helper.assertTrue(!penguin.isBaby(), "Chick can become adult");
        helper.succeed();
    }

    private static void chickFeeding(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var fish : java.util.List.of(net.minecraft.world.item.Items.COD, net.minecraft.world.item.Items.SALMON)) {
            var chick = Penguin.createChick(level);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(fish, 2));
            int before = chick.getAge();
            var result = chick.mobInteract(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            helper.assertTrue(result.consumesAction(), "Chick accepts " + fish);
            helper.assertTrue(player.getMainHandItem().getCount() == 1, "Feeding consumes one fish");
            helper.assertTrue(chick.getAge() > before && chick.isBaby(), "Feeding advances growth");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
            chick.addAdditionalSaveData(output);
            helper.assertTrue(output.buildResult().getLongOr("GrowsUpAt", 0)
                    == level.getGameTime() - chick.getAge(), "Feeding also advances the saved deadline");
        }
        helper.succeed();
    }

    private static void chickGrowth(GameTestHelper helper) {
        var level = helper.getLevel();
        var chick = Penguin.createChick(level);
        chick.setNoAi(true);
        chick.setAge(-2);
        chick.aiStep();
        chick.aiStep();
        helper.assertTrue(!chick.isBaby(), "Loaded chick grows up automatically");
        var legacy = new CompoundTag();
        legacy.putInt("Age", -2); // Older saves may have no custom deadline.
        chick.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), legacy));
        chick.aiStep();
        chick.aiStep();
        helper.assertTrue(!chick.isBaby(), "Legacy chick without deadline grows up automatically");
        var unloaded = new CompoundTag();
        unloaded.putInt("Age", -240000);
        unloaded.putLong("GrowsUpAt", level.getGameTime() - 1);
        chick.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), unloaded));
        chick.aiStep();
        helper.assertTrue(!chick.isBaby(), "Chick whose deadline passed while unloaded becomes adult");
        helper.succeed();
    }

    private static void nestHatching(GameTestHelper helper) {
        BlockPos floor = new BlockPos(2, 1, 2);
        BlockPos nestPos = floor.above();
        helper.setBlock(floor, Blocks.STONE);
        helper.setBlock(nestPos, ColdPenguins.NEST.get().defaultBlockState().setValue(PenguinNestBlock.EGGS, 2));
        var level = helper.getLevel();
        var nest = (PenguinNestBlock.Nest) level.getBlockEntity(helper.absolutePos(nestPos));
        var data = new CompoundTag();
        data.putLong("IncubationRemaining", 1);
        data.putLong("LastIncubationGameTime", level.getGameTime());
        data.putLong("LastIncubationDayTime", level.getDefaultClockTime());
        nest.loadAdditional(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
        helper.succeedWhen(() -> {
            helper.assertBlockProperty(nestPos, PenguinNestBlock.EGGS, 0);
            var chicks = level.getEntitiesOfClass(Penguin.class,
                    new net.minecraft.world.phys.AABB(helper.absolutePos(nestPos)).inflate(3));
            helper.assertTrue(chicks.size() == 2 && chicks.stream().allMatch(Penguin::isBaby), "Both nest eggs hatch as chicks");
        });
    }

    private static void underwaterArrow(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int x = 1; x <= 5; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
            }
        }
        var vanilla = net.minecraft.world.entity.EntityTypes.ARROW.create(level,
                net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var penguin = new BlackTippedArrow(ColdPenguins.BLACK_ARROW_PROJECTILE.get(), level);
        for (var arrow : java.util.List.of(vanilla, penguin)) {
            arrow.setPos(pos.getX() + 0.2, pos.getY() + 0.5, pos.getZ() + 0.5);
            arrow.setNoGravity(true);
            arrow.baseTick(); // Initialize fluid contact, as the server does before sustained underwater flight.
            arrow.setDeltaMovement(0.25, 0, 0);
            arrow.tick();
        }
        double start = pos.getX() + 0.2;
        helper.assertTrue(Math.abs((penguin.getX() - start) - 2 * (vanilla.getX() - start)) < 0.001,
                "Black-tipped arrows move twice as far per underwater tick");
        helper.assertTrue(Math.abs(penguin.getDeltaMovement().x - 0.2) < 0.001,
                "Water drag preserves the expected arrow velocity");
        helper.succeed();
    }

    private static void loot(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int eggs = 0; eggs <= 3; eggs++) {
            var state = ColdPenguins.NEST.get().defaultBlockState().setValue(PenguinNestBlock.EGGS, eggs);
            var drops = net.minecraft.world.level.block.Block.getDrops(state, level, pos, null);
            helper.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum() == eggs,
                    "Nest drops exactly its occupied egg count: " + eggs);
        }
        var penguin = Penguin.createChick(level);
        penguin.setBaby(false);
        var table = level.getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                        id("entities/penguin")));
        for (boolean golden : new boolean[]{false, true}) {
            var data = new CompoundTag();
            data.putBoolean("GoldenPlumage", golden);
            penguin.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, penguin)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, penguin.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,
                            level.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            var drops = table.getRandomItems(params);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(golden
                    ? ColdPenguins.YELLOW_PENGUIN_FEATHER.get() : ColdPenguins.PENGUIN_FEATHER.get()),
                    "Feather color matches adult plumage: " + golden);
        }
        helper.succeed();
    }

    private static void itemComponents(GameTestHelper helper) {
        var horn = new ItemStack(ColdPenguins.PENGUIN_HORN.get());
        var instrument = horn.get(DataComponents.INSTRUMENT);
        helper.assertTrue(instrument != null && instrument.instrument().value().range() == 100.0F, "Horn instrument resolves");
        helper.assertTrue(instrument.instrument().value().useDuration() == 7.0F, "Horn lasts seven seconds");
        var egg = new ItemStack(ColdPenguins.PENGUIN_SPAWN_EGG.get());
        helper.assertTrue(egg.get(DataComponents.ENTITY_DATA) != null, "Spawn egg has entity data");
        var hat = new ItemStack(ColdPenguins.SANTA_HAT.get());
        helper.assertTrue(hat.get(DataComponents.EQUIPPABLE).assetId().isPresent(), "Hat has an equipment model");
        helper.succeed();
    }
}
