package dev.coldpenguins;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fish.Cod;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public final class Penguin extends Animal {
    private static final EntityDataAccessor<Integer> SPECIES =
            SynchedEntityData.defineId(Penguin.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GOLDEN_PLUMAGE =
            SynchedEntityData.defineId(Penguin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BUILDING_NEST =
            SynchedEntityData.defineId(Penguin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DANCING =
            SynchedEntityData.defineId(Penguin.class, EntityDataSerializers.BOOLEAN);
    private BlockPos danceJukebox;
    private Vec3 hornPosition;
    private long hornCallExpires;
    private NestProject nestProject;
    private static final int CHICK_AGE_TICKS = 10 * 24_000;
    private static final int MAX_FLOCK_SIZE = 10;
    private static final double FAMILY_RANGE = 30.0;
    private static final double FAMILY_ROAM_RADIUS = FAMILY_RANGE / 2.0;
    private static final int FISH_RANGE = 100;
    private static final long MAX_SWIM_TICKS = 3 * 60 * 20;
    private static final long RETURN_BUFFER_TICKS = 25 * 20;
    private long swimStarted = -1;
    private long nextSwimTime;
    private int nextShoreJumpTick;
    private BlockPos lastDryLand;
    private UUID familyId;
    private Penguin familyAnchor;
    private long nextFamilySearch;
    private List<Penguin> nearbyFamily = List.of();
    private long nextFamilyMemberSearch;
    private long growsUpAt;

    public Penguin(EntityType<? extends Penguin> type, Level level) {
        super(type, level);
        moveControl = new MoveControl(this) {
            private final MoveControl swimming = new SmoothSwimmingMoveControl(
                    Penguin.this, 85, 20, 0.7F, 1.0F, false);

            @Override
            public void setWantedPosition(double x, double y, double z, double speed) {
                super.setWantedPosition(x, y, z, speed);
                swimming.setWantedPosition(x, y, z, speed);
            }

            @Override
            public void tick() {
                if (Penguin.this.isInWater()) {
                    swimming.tick();
                    if (!Penguin.this.getNavigation().isDone()) {
                        Penguin.this.tryShoreJump(this.wantedX, this.wantedY, this.wantedZ);
                    }
                } else {
                    Penguin.this.setYya(0.0F);
                    super.tick();
                }
            }
        };
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH, 16)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FollowHornGoal());
        goalSelector.addGoal(1, new SwimGoal());
        goalSelector.addGoal(2, new ChickFleeGoal());
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return !Penguin.this.isBaby() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !Penguin.this.isBaby() && super.canContinueToUse();
            }
        });
        goalSelector.addGoal(3, new BuildNestGoal());
        goalSelector.addGoal(4, new BreedGoal(this, 1.0));
        goalSelector.addGoal(5, new SantaDanceGoal());
        goalSelector.addGoal(5, new DanceGoal());
        goalSelector.addGoal(6, new FollowFamilyGoal());
        goalSelector.addGoal(7, new WanderGoal());
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SPECIES, 0);
        builder.define(GOLDEN_PLUMAGE, false);
        builder.define(BUILDING_NEST, false);
        builder.define(DANCING, false);
    }

    public PenguinSpecies getSpecies() {
        return PenguinSpecies.values()[Math.clamp(entityData.get(SPECIES), 0, PenguinSpecies.values().length - 1)];
    }

    void setSpecies(PenguinSpecies species) {
        entityData.set(SPECIES, species.ordinal());
        getAttribute(Attributes.SCALE).setBaseValue(species.scale);
        refreshDimensions();
        if (species != PenguinSpecies.ORIGINAL) entityData.set(GOLDEN_PLUMAGE, false);
    }

    @Override
    protected net.minecraft.network.chat.Component getTypeName() {
        return getSpecies() == PenguinSpecies.ORIGINAL ? super.getTypeName()
                : net.minecraft.network.chat.Component.translatable("entity.coldpenguins.penguin." + getSpecies().id);
    }

    boolean hasGoldenPlumage() {
        return entityData.get(GOLDEN_PLUMAGE);
    }

    boolean isBuildingNest() {
        return entityData.get(BUILDING_NEST);
    }

    boolean isDancing() {
        return entityData.get(DANCING);
    }

    void hearDanceMusic(BlockPos jukebox, boolean playing) {
        if (playing) {
            danceJukebox = jukebox.immutable();
        } else if (jukebox.equals(danceJukebox)) {
            danceJukebox = null;
        }
    }

    Item featherItem() {
        return hasGoldenPlumage() && !isBaby()
                ? ColdPenguins.YELLOW_PENGUIN_FEATHER.get() : ColdPenguins.PENGUIN_FEATHER.get();
    }

    void hearPenguinHorn(Vec3 origin) {
        hornPosition = origin;
        hornCallExpires = level().getGameTime() + 120 * 20;
        getNavigation().stop();
    }

    boolean hasSantaHat() {
        return getItemBySlot(EquipmentSlot.HEAD).is(ColdPenguins.SANTA_HAT.get());
    }

    private void tryShoreJump(double wantedX, double wantedY, double wantedZ) {
        if (tickCount < nextShoreJumpTick || wantedY < getY() + 0.15
                || level().getFluidState(blockPosition().above()).is(FluidTags.WATER)
                || !level().getFluidState(BlockPos.containing(wantedX, wantedY, wantedZ)).isEmpty()) {
            return;
        }
        Vec3 toward = new Vec3(wantedX - getX(), 0, wantedZ - getZ());
        if (toward.horizontalDistanceSqr() > 4.0 && !horizontalCollision) {
            return;
        }
        Vec3 push = toward.normalize().scale(0.24);
        setDeltaMovement(push.x, 0.52, push.z);
        nextShoreJumpTick = tickCount + 12;
        syncVelocity = true;
    }

    private static java.util.Optional<BlockPos> findClosestMatch(BlockPos origin, int horizontal,
            int vertical, java.util.function.Predicate<BlockPos> predicate) {
        for (BlockPos candidate : BlockPos.withinBoxByManhattanDistance(origin, horizontal, vertical, horizontal)) {
            if (predicate.test(candidate)) {
                return java.util.Optional.of(candidate.immutable());
            }
        }
        return java.util.Optional.empty();
    }

    private void rollPlumage() {
        entityData.set(GOLDEN_PLUMAGE, getSpecies() == PenguinSpecies.ORIGINAL && random.nextInt(100) == 0);
    }

    static Penguin createChick(ServerLevel level) {
        return createChick(level, PenguinSpecies.ORIGINAL);
    }

    static Penguin createChick(ServerLevel level, PenguinSpecies species) {
        Penguin chick = ColdPenguins.PENGUIN.get().create(level, EntitySpawnReason.BREEDING);
        if (chick != null) {
            chick.setSpecies(species);
            chick.rollPlumage();
            chick.setBaby(true);
        }
        return chick;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, SpawnGroupData groupData) {
        rollPlumage();
        setSpecies(PenguinSpecies.forBiome(level.getBiome(blockPosition()), random));
        boolean natural = reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
        if (!natural) {
            return super.finalizeSpawn(level, difficulty, reason, new AgeableMobGroupData(false));
        }

        FlockData flock = groupData instanceof FlockData existing ? existing : new FlockData(getUUID(), getSpecies());
        int flockMembers = flock.getGroupSize();
        familyId = flock.familyId;
        setSpecies(flock.species);
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, flock);
        if (flockMembers == 1 || (flockMembers > 1 && random.nextFloat() < 0.25F)) {
            setBaby(true);
        }
        return result;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new AmphibiousPathNavigation(this, level);
    }

    @Override
    public void travel(Vec3 movement) {
        if (isLocalInstanceAuthoritative() && isInWater()) {
            moveRelative(getSpeed(), movement);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
        } else {
            super.travel(movement);
        }
    }

    static boolean isPrey(Entity entity) {
        return entity instanceof Cod || entity instanceof Salmon;
    }

    @Override
    public boolean doHurtTarget(ServerLevel server, Entity target) {
        if (target instanceof Player) {
            boolean hit = target.hurtServer(server, damageSources().mobAttack(this), 1.0F);
            if (hit) {
                setLastHurtMob(target);
            }
            return hit;
        }
        boolean hunting = isInWater() && target.isInWater() && isPrey(target);
        boolean hit = super.doHurtTarget(server, target);
        if (hunting && hit && !target.isAlive()) {
            heal(2.0F);
            playSound(SoundEvents.GENERIC_EAT.value(), 0.55F, getVoicePitch());
        }
        return hit;
    }

    public static boolean checkLandSpawnRules(EntityType<Penguin> type, LevelAccessor level,
                                              EntitySpawnReason reason, BlockPos position, RandomSource random) {
        BlockPos ground = position.below();
        boolean ice = level.getBlockState(ground).is(BlockTags.ICE);
        boolean emperorOnIce = ice && PenguinSpecies.isEmperorHabitat(level.getBiome(position));
        return level.getFluidState(position).isEmpty()
                && level.getFluidState(ground).isEmpty()
                && level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
                // Vanilla ice only permits polar bears; explicitly allow emperor colonies too.
                && (emperorOnIce || (!ice && checkMobSpawnRules(type, level, reason, position, random)));
    }

    @Override
    public boolean hurtServer(ServerLevel server, DamageSource source, float amount) {
        float previousHealth = getHealth();
        boolean hurt = super.hurtServer(server, source, amount);
        if (hurt && isAlive() && getHealth() < previousHealth
                && server.getGameRules().get(GameRules.MOB_DROPS)) {
            spawnAtLocation(server, featherItem());
        }
        return hurt;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return MAX_FLOCK_SIZE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damage) {
        return ColdPenguins.PENGUIN_HONK.get();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON);
    }

    @Override
    public Penguin getBreedOffspring(ServerLevel level, AgeableMob partner) {
        // Natural breeding still uses the nest-building override; vanilla spawn eggs use this hook.
        return createChick(level, getSpecies());
    }

    @Override
    protected void applyImplicitComponents(net.minecraft.core.component.DataComponentGetter components) {
        super.applyImplicitComponents(components);
        var data = components.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
        if (data != null && data.type() == ColdPenguins.PENGUIN.get()) {
            var tag = data.copyTagWithoutId();
            if (tag.contains("Species")) setSpecies(PenguinSpecies.fromName(tag.getStringOr("Species", "original")));
        }
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel level, Animal partner) {
        if (!(partner instanceof Penguin other) || nestProject != null || other.nestProject != null
                || isInWater() || other.isInWater()) {
            return;
        }
        int checked = 0;
        for (BlockPos candidate : BlockPos.withinBoxByManhattanDistance(blockPosition(), 7, 3, 7)) {
            if (!PenguinNestBlock.isAvailable(level, candidate)) {
                continue;
            }
            if (++checked > 16) {
                break;
            }
            Path firstPath = getNavigation().createPath(candidate, 1);
            Path secondPath = other.getNavigation().createPath(candidate, 1);
            if (firstPath != null && firstPath.canReach() && secondPath != null && secondPath.canReach()) {
                NestProject project = new NestProject(this, other, candidate.immutable(), level.getGameTime() + 300);
                nestProject = project;
                other.nestProject = project;
                return;
            }
        }
    }

    @Override
    protected int getBabyStartAge() {
        return -CHICK_AGE_TICKS;
    }

    @Override
    public void setAge(int age) {
        super.setAge(age);
        growsUpAt = age < 0 ? level().getGameTime() - (long) age : 0;
    }

    @Override
    public void aiStep() {
        if (!level().isClientSide() && isBaby() && !isAgeLocked() && growsUpAt > 0) {
            // Reconcile elapsed time while unloaded before vanilla advances the loaded age.
            long remaining = Math.max(0, growsUpAt - level().getGameTime());
            setAge((int) -Math.min(remaining, -(long) getAge()));
        }
        super.aiStep();
        if (level() instanceof ServerLevel server) {
            long now = server.getGameTime();
            boolean musicPlaying = danceJukebox != null
                    && danceJukebox.closerToCenterThan(position(), 64.0)
                    && server.getBlockEntity(danceJukebox) instanceof JukeboxBlockEntity jukebox
                    && jukebox.getTheItem().is(ColdPenguins.PENGUIN_DANCE_DISC.get())
                    && jukebox.getSongPlayer().isPlaying();
            if (!musicPlaying) {
                danceJukebox = null;
            }
            entityData.set(DANCING, (musicPlaying || hasSantaHat()) && hornPosition == null
                    && (onGround() || hasSantaHat()) && !isInWater() && !isBuildingNest() && getTarget() == null && !isPanicking()
                    && (getLastHurtByMob() == null || tickCount - getLastHurtByMobTimestamp() > 60));
            if (isInWater()) {
                if (swimStarted < 0) {
                    swimStarted = now;
                }
            } else if (onGround()) {
                lastDryLand = blockPosition();
                if (swimStarted >= 0) {
                    swimStarted = -1;
                    nextSwimTime = now + 20 * 20;
                }
                if (getDeltaMovement().horizontalDistanceSqr() > 0.0025 && random.nextInt(500) == 0) {
                    playContactCall(0.55F);
                }
            }
        }
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ColdPenguins.SANTA_HAT.get()) && getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            if (!level().isClientSide()) {
                setItemSlot(EquipmentSlot.HEAD, held.copyWithCount(1));
                setDropChance(EquipmentSlot.HEAD, 2.0F);
                setPersistenceRequired();
                held.consume(1, player);
                playSound(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && held.isEmpty() && hasSantaHat()) {
            if (!level().isClientSide()) {
                ItemStack hat = getItemBySlot(EquipmentSlot.HEAD);
                setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                if (!player.addItem(hat)) {
                    spawnAtLocation((ServerLevel) level(), hat);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (isBaby() && isFood(held)) {
            return super.mobInteract(player, hand);
        }
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }

        if (level() instanceof ServerLevel server) {
            playContactCall(getSoundVolume());
            spawnAtLocation(server, featherItem());
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(),
                    3, 0.2, 0.1, 0.2, 0.0);
        }
        return InteractionResult.SUCCESS;
    }

    private void playContactCall(float volume) {
        SoundEvent call = random.nextInt(5) == 0
                ? ColdPenguins.PENGUIN_HONK.get() : ColdPenguins.PENGUIN_TRILL.get();
        playSound(call, volume, getVoicePitch());
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Species", getSpecies().id);
        tag.putLong("GrowsUpAt", growsUpAt);
        tag.putBoolean("GoldenPlumage", hasGoldenPlumage());
        tag.putLong("SwimStarted", swimStarted);
        tag.putLong("NextSwimTime", nextSwimTime);
        if (lastDryLand != null) {
            tag.putLong("LastDryLand", lastDryLand.asLong());
        }
        if (familyId != null) {
            tag.store("Family", UUIDUtil.CODEC, familyId);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        setSpecies(PenguinSpecies.fromName(tag.getStringOr("Species", "original")));
        long ageDeadline = level().getGameTime() - (long) getAge();
        long savedDeadline = tag.getLongOr("GrowsUpAt", ageDeadline);
        growsUpAt = isBaby() ? Math.min(savedDeadline > 0 ? savedDeadline : ageDeadline, ageDeadline) : 0;
        entityData.set(GOLDEN_PLUMAGE, tag.getBooleanOr("GoldenPlumage", false));
        swimStarted = tag.getLongOr("SwimStarted", -1);
        nextSwimTime = tag.getLongOr("NextSwimTime", 0);
        lastDryLand = tag.getLong("LastDryLand").map(BlockPos::of).orElse(null);
        familyId = tag.read("Family", UUIDUtil.CODEC).orElse(null);
        // Resize penguins saved by the original release.
        getAttribute(Attributes.SCALE).setBaseValue(getSpecies().scale);
        refreshDimensions();
    }

    private Penguin familyAnchor() {
        if (familyId == null || familyId.equals(getUUID()) || !(level() instanceof ServerLevel server)) {
            return this;
        }
        if (server.getGameTime() >= nextFamilySearch) {
            nextFamilySearch = server.getGameTime() + 40;
            if (server.getEntity(familyId) instanceof Penguin leader && leader.isAlive()) {
                familyAnchor = leader;
            } else {
                familyAnchor = server.getEntitiesOfClass(Penguin.class, getBoundingBox().inflate(FAMILY_RANGE),
                                other -> other.isAlive() && familyId.equals(other.familyId))
                        .stream().min(Comparator.comparing(Penguin::getUUID)).orElse(this);
            }
        }
        return familyAnchor != null && familyAnchor.isAlive() && !familyAnchor.isRemoved()
                ? familyAnchor : this;
    }

    private boolean nearFamily(Vec3 destination) {
        Penguin anchor = familyAnchor();
        if (anchor != this) {
            return anchor.position().distanceToSqr(destination) <= FAMILY_ROAM_RADIUS * FAMILY_ROAM_RADIUS;
        }
        if (familyId == null || !(level() instanceof ServerLevel server)) {
            return true;
        }

        // The anchor waits for lagging relatives instead of leading them farther away.
        if (server.getGameTime() >= nextFamilyMemberSearch) {
            nextFamilyMemberSearch = server.getGameTime() + 40;
            nearbyFamily = server.getEntitiesOfClass(Penguin.class, getBoundingBox().inflate(FAMILY_RANGE + 12.0),
                    other -> other != this && other.isAlive() && familyId.equals(other.familyId));
        }
        for (Penguin relative : nearbyFamily) {
            double currentDistance = distanceToSqr(relative);
            double nextDistance = relative.position().distanceToSqr(destination);
            if (relative.isAlive() && nextDistance > Math.max(currentDistance, FAMILY_ROAM_RADIUS * FAMILY_ROAM_RADIUS)) {
                return false;
            }
        }
        return true;
    }

    private static final class FlockData extends AgeableMobGroupData {
        private final UUID familyId;

        private final PenguinSpecies species;

        FlockData(UUID familyId, PenguinSpecies species) {
            super(false);
            this.familyId = familyId;
            this.species = species;
        }
    }

    private void navigateToward(BlockPos target, double speed, int reach) {
        Vec3 offset = Vec3.atBottomCenterOf(target).subtract(position());
        BlockPos destination = offset.lengthSqr() > 26 * 26
                ? BlockPos.containing(position().add(offset.normalize().scale(24))) : target;
        Path path = getNavigation().createPath(destination, reach);
        if (path != null && getNavigation().moveTo(path, speed)) {
            return;
        }
        Vec3 waypoint = DefaultRandomPos.getPosTowards(Penguin.this, 12, 8,
                Vec3.atBottomCenterOf(target), Math.PI / 3.0);
        if (waypoint != null) {
            getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, speed);
        }
    }


    private final class FollowHornGoal extends Goal {
        private long nextRepath;

        FollowHornGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (hornPosition == null) {
                return false;
            }
            if (level().getGameTime() >= hornCallExpires || distanceToSqr(hornPosition) <= 2.5 * 2.5) {
                hornPosition = null;
                return false;
            }
            return !isInWater() || swimStarted < 0
                    || level().getGameTime() - swimStarted < MAX_SWIM_TICKS - RETURN_BUFFER_TICKS;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            nextRepath = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            getLookControl().setLookAt(hornPosition.x, hornPosition.y, hornPosition.z, 20, 20);
            if (level().getGameTime() >= nextRepath) {
                nextRepath = level().getGameTime() + 15;
                navigateToward(BlockPos.containing(hornPosition), 1.3, 1);
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class SantaDanceGoal extends Goal {
        private Vec3 center;
        private int danceTicks;

        SantaDanceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return hasSantaHat() && isDancing();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            center = position();
            danceTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (danceTicks % 8 == 0) {
                double angle = danceTicks * Math.PI / 64.0;
                Vec3 step = center.add(Math.cos(angle) * 1.6, 0, Math.sin(angle) * 1.6);
                BlockPos floor = BlockPos.containing(step);
                if (canStandAt(floor)) {
                    getNavigation().moveTo(step.x, floor.getY(), step.z, 1.05);
                }
            }
            if (danceTicks % 18 == 0 && onGround()) {
                getJumpControl().jump();
            }
            danceTicks++;
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class DanceGoal extends Goal {
        DanceGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isDancing();
        }

        @Override
        public boolean canContinueToUse() {
            return isDancing();
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            getNavigation().stop();
            if (danceJukebox != null) {
                getLookControl().setLookAt(danceJukebox.getX() + 0.5,
                        danceJukebox.getY() + 0.5, danceJukebox.getZ() + 0.5, 15, 15);
            }
        }
    }

    private static final class NestProject {
        private final Penguin first;
        private final Penguin second;
        private final BlockPos position;
        private final long deadline;
        private int workTicks;

        NestProject(Penguin first, Penguin second, BlockPos position, long deadline) {
            this.first = first;
            this.second = second;
            this.position = position;
            this.deadline = deadline;
        }

        boolean canBuild(ServerLevel level) {
            return first.isAlive() && second.isAlive() && first.isInLove() && second.isInLove()
                    && !first.isInWater() && !second.isInWater() && level.getGameTime() < deadline
                    && PenguinNestBlock.isAvailable(level, position);
        }

        boolean isAtSite(Penguin penguin) {
            return penguin.onGround() && penguin.distanceToSqr(Vec3.atBottomCenterOf(position)) <= 4.0;
        }

        void tick(ServerLevel level) {
            boolean working = isAtSite(first) && isAtSite(second);
            first.entityData.set(BUILDING_NEST, working);
            second.entityData.set(BUILDING_NEST, working);
            if (!working) {
                return;
            }

            workTicks++;
            if (workTicks % 10 == 0) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()),
                        position.getX() + 0.5, position.getY() + 0.15, position.getZ() + 0.5,
                        5, 0.23, 0.07, 0.23, 0.02);
                level.playSound(null, position, SoundEvents.STONE_PLACE, first.getSoundSource(), 0.28F, 1.15F);
            }
            if (workTicks >= 80) {
                var nest = ColdPenguins.NEST.get().defaultBlockState()
                        .setValue(PenguinNestBlock.EGGS, 1 + first.random.nextInt(3));
                if (level.setBlock(position, nest, Block.UPDATE_ALL)) {
                    if (level.getBlockEntity(position) instanceof PenguinNestBlock.Nest nestEntity) {
                        nestEntity.setSpecies(first.random.nextBoolean() ? first.getSpecies() : second.getSpecies());
                    }
                    first.finalizeSpawnChildFromBreeding(level, second, null);
                    first.playSound(ColdPenguins.PENGUIN_HONK.get(), first.getSoundVolume(), first.getVoicePitch());
                }
                clear();
            }
        }

        void clear() {
            first.nestProject = null;
            second.nestProject = null;
            first.entityData.set(BUILDING_NEST, false);
            second.entityData.set(BUILDING_NEST, false);
        }
    }

    private final class BuildNestGoal extends Goal {
        private NestProject project;
        private int repathTicks;

        BuildNestGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            project = nestProject;
            if (project == null || !(level() instanceof ServerLevel server)) {
                return false;
            }
            if (!project.canBuild(server)) {
                project.clear();
                return false;
            }
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return project == nestProject && level() instanceof ServerLevel server && project.canBuild(server);
        }

        @Override
        public void start() {
            repathTicks = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Vec3 center = Vec3.atBottomCenterOf(project.position);
            getLookControl().setLookAt(center.x, center.y + 0.15, center.z, 20.0F, 35.0F);
            if (project.isAtSite(Penguin.this)) {
                getNavigation().stop();
            } else if (--repathTicks <= 0) {
                repathTicks = 15;
                Path path = getNavigation().createPath(project.position, 1);
                if (path != null) {
                    getNavigation().moveTo(path, 1.0);
                }
            }
            if (project.first == Penguin.this && level() instanceof ServerLevel server) {
                project.tick(server);
            }
        }

        @Override
        public void stop() {
            if (project == nestProject) {
                project.clear();
            }
            getNavigation().stop();
        }
    }

    private final class FollowFamilyGoal extends Goal {
        private Penguin anchor;
        private int repathTicks;

        FollowFamilyGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            anchor = familyAnchor();
            return anchor != Penguin.this && distanceToSqr(anchor) > 11.0 * 11.0;
        }

        @Override
        public boolean canContinueToUse() {
            return anchor == familyAnchor() && anchor != Penguin.this && distanceToSqr(anchor) > 6.0 * 6.0;
        }

        @Override
        public void start() {
            repathTicks = 0;
        }

        @Override
        public void tick() {
            if (--repathTicks <= 0) {
                repathTicks = adjustedTickDelay(10);
                if (!getNavigation().moveTo(anchor, 1.25)) {
                    Vec3 waypoint = DefaultRandomPos.getPosTowards(Penguin.this, 12, 8,
                            anchor.position(), Math.PI / 3.0);
                    if (waypoint != null) {
                        getNavigation().moveTo(waypoint.x, waypoint.y, waypoint.z, 1.25);
                    }
                }
            }
        }

        @Override
        public void stop() {
            getNavigation().stop();
        }
    }

    private final class WanderGoal extends RandomStrollGoal {
        WanderGoal() {
            super(Penguin.this, 1.0, 75, false);
        }

        @Override
        protected Vec3 getPosition() {
            if (isInWater()) {
                return null;
            }
            BlockPos water = level().getGameTime() >= nextSwimTime && random.nextInt(3) == 0
                    ? findClosestMatch(blockPosition(), 6, 2,
                            pos -> level().getFluidState(pos).is(FluidTags.WATER)).orElse(null)
                    : null;
            Vec3 destination = water == null ? LandRandomPos.getPos(Penguin.this, 8, 5) : Vec3.atBottomCenterOf(water);
            return destination != null && nearFamily(destination) ? destination : null;
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && nearFamily(new Vec3(wantedX, wantedY, wantedZ));
        }
    }

    private boolean canStandAt(BlockPos position) {
        if (position == null || !level().hasChunkAt(position)) {
            return false;
        }
        Vec3 feet = Vec3.atBottomCenterOf(position);
        return level().getFluidState(position).isEmpty()
                && level().getBlockState(position.below()).isFaceSturdy(level(), position.below(), Direction.UP)
                && level().noCollision(this, getDimensions(getPose()).makeBoundingBox(feet));
    }

    private BlockPos findShore() {
        BlockPos nearby = findClosestMatch(blockPosition(), 10, 6, this::canStandAt)
                .map(BlockPos::immutable).orElse(null);
        if (nearby != null) {
            return nearby;
        }
        if (canStandAt(lastDryLand)) {
            return lastDryLand;
        }

        // Sample the surface for a bank when the penguin starts far offshore or deep underwater.
        for (int radius = 8; radius <= 104; radius += 8) {
            for (int direction = 0; direction < 16; direction++) {
                double angle = direction * Math.PI / 8;
                int x = getBlockX() + (int) Math.round(radius * Math.cos(angle));
                int z = getBlockZ() + (int) Math.round(radius * Math.sin(angle));
                BlockPos column = new BlockPos(x, getBlockY(), z);
                if (level().hasChunkAt(column)) {
                    int y = level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                    BlockPos surface = new BlockPos(x, y, z);
                    if (canStandAt(surface)) {
                        return surface;
                    }
                }
            }
        }
        return null;
    }

    private final class SwimGoal extends Goal {
        private AbstractFish prey;
        private BlockPos shore;
        private long nextSearch;
        private long nextAttack;
        private boolean returning;

        SwimGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return isInWater();
        }

        @Override
        public boolean canContinueToUse() {
            return isInWater();
        }

        @Override
        public void start() {
            returning = false;
            nextSearch = 0;
            shore = null;
            prey = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            long now = level().getGameTime();
            long elapsed = swimStarted < 0 ? 0 : now - swimStarted;
            if (!returning && elapsed >= MAX_SWIM_TICKS - RETURN_BUFFER_TICKS) {
                returnToLand();
            }
            if (returning) {
                exitWater(now, elapsed);
                return;
            }
            if (now >= nextSearch) {
                nextSearch = now + 30;
                prey = level().getEntitiesOfClass(AbstractFish.class, getBoundingBox().inflate(FISH_RANGE),
                                fish -> isPrey(fish) && fish.isAlive() && fish.isInWater()
                                        && distanceToSqr(fish) <= FISH_RANGE * FISH_RANGE)
                        .stream().min(Comparator.comparingDouble(Penguin.this::distanceToSqr)).orElse(null);
                if (prey == null) {
                    returnToLand();
                    exitWater(now, elapsed);
                    return;
                }
                navigateToward(prey.blockPosition(), 1.25, 1);
            }
            if (prey != null && prey.isAlive() && prey.isInWater()) {
                getLookControl().setLookAt(prey, 20.0F, 20.0F);
                if (now >= nextAttack && isWithinMeleeAttackRange(prey)) {
                    nextAttack = now + 20;
                    doHurtTarget((ServerLevel) level(), prey);
                }
            }
        }

        private void returnToLand() {
            returning = true;
            prey = null;
            nextSearch = 0;
            getNavigation().stop();
        }

        private void exitWater(long now, long elapsed) {
            boolean deadlineReached = elapsed >= MAX_SWIM_TICKS;
            if (deadlineReached && canStandAt(shore)) {
                returnToShore();
                return;
            }
            if (now < nextSearch) {
                return;
            }
            nextSearch = now + 30;
            if (!canStandAt(shore)) {
                shore = findShore();
            }
            if (shore != null) {
                if (deadlineReached) {
                    returnToShore();
                } else {
                    approachShore(shore);
                }
            } else {
                BlockPos up = blockPosition().above(10);
                navigateToward(up, 1.15, 1);
            }
        }

        private void approachShore(BlockPos bank) {
            Vec3 landing = Vec3.atBottomCenterOf(bank);
            Vec3 offset = landing.subtract(position());
            if (offset.horizontalDistanceSqr() <= 4.0 && offset.y > 0.15 && offset.y <= 1.6
                    && !level().getFluidState(blockPosition().above()).is(FluidTags.WATER)) {
                getNavigation().stop();
                tryShoreJump(landing.x, landing.y, landing.z);
                return;
            }

            // Swim to water beside the bank before requesting the jump onto its top.
            Vec3 bankToPenguin = new Vec3(-offset.x, 0, -offset.z).normalize();
            BlockPos approach = BlockPos.containing(landing.add(bankToPenguin.scale(1.2)).add(0, -1, 0));
            if (level().getFluidState(approach).is(FluidTags.WATER)) {
                navigateToward(approach, 1.25, 0);
            } else {
                navigateToward(bank, 1.25, 0);
            }
        }

        private void returnToShore() {
            Vec3 safe = Vec3.atBottomCenterOf(shore);
            teleportTo(safe.x, safe.y, safe.z);
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
        }

        @Override
        public void stop() {
            getNavigation().stop();
            prey = null;
        }
    }

    private final class ChickFleeGoal extends PanicGoal {
        ChickFleeGoal() {
            super(Penguin.this, 1.4);
        }

        @Override
        protected boolean shouldPanic() {
            return Penguin.this.isBaby() && Penguin.this.getLastDamageSource() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Penguin.this.isBaby() && super.canContinueToUse();
        }

        @Override
        protected boolean findRandomPosition() {
            DamageSource damage = Penguin.this.getLastDamageSource();
            Entity attacker = damage == null ? null : damage.getEntity();
            Vec3 escape = attacker == null ? null
                    : DefaultRandomPos.getPosAway(Penguin.this, 8, 4, attacker.position());
            if (escape == null) {
                return super.findRandomPosition();
            }
            posX = escape.x;
            posY = escape.y;
            posZ = escape.z;
            return true;
        }
    }
}
