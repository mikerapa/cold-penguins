# Cold Penguins

A mod for Minecraft Java Edition 26.3 with NeoForge 26.3.0.101 or a newer 26.3 release.

This branch ports Cold Penguins to 26.3. The original Minecraft 1.21.1 version remains on `main`.

## Play

1. Install NeoForge for Minecraft 26.3 and use Java 25.
2. Remove the previous Cold Penguins JAR if you are upgrading. Put `cold-penguins-1.13.2+mc26.3.jar` in that installation's `mods` folder. For multiplayer, install it on the server and on each client.
3. Launch the NeoForge profile. Look for penguins in snowy plains, snowy beaches, snowy taigas, and other frozen biomes. Natural spawn attempts happen on dry land and form flocks of five to ten with both adults and chicks. Terrain and the passive mob cap can leave smaller flocks. Penguins from the same spawn group remember their family and try to stay within 30 blocks of each other, including after a world reload. In Creative mode, the Penguin Spawn Egg is in the Spawn Eggs tab. With cheats enabled, you can also use `/summon coldpenguins:penguin` in an open area.

Right-click a penguin with an empty main hand to pet it. Hearts and a contact call show that it was petted. Penguins occasionally call while walking and sometimes flap their flippers on land. The call is usually a trill, with a one-in-five chance of a honk. Both calls use field recordings. The trill is from a little penguin, and the honk is from a Galápagos penguin. See the audio credits packaged in the JAR. Penguins honk when they breed or take damage. Adults fight back against their attacker, while chicks flee. Penguins can dive and swim at any depth without drowning. They jump up toward shore waypoints to climb out onto full-block banks and ice. While swimming, they hunt and eat cod and salmon within 100 blocks. They head for land if no prey is present or the three-minute swim limit is approaching. At the limit, a penguin still in the water returns directly to a safe shore if one is available. Penguins keep looking for an exit when no safe shore can be found. Attacks against players deal one damage point (half a heart) before armor and other reductions. Petting drops one Penguin Feather. Yellow-and-white adults drop yellow feathers, while other penguins drop black feathers. Penguins also drop a feather of the same color on a damaging hit they survive or when they die, subject to Minecraft's mob loot rule.

Craft four Black-Tipped Arrows with one flint, one stick, and one Penguin Feather in any arrangement. The arrow has the regular arrow silhouette with an extended black tip. Use them in bows, crossbows, or dispensers. They travel at standard arrow speed in air and cover twice the distance per movement step underwater. Reduced water drag gives them approximately four times a normal arrow's horizontal range in uninterrupted water.

Original and Gentoo adults are one block tall; Emperor adults are 1.2 blocks, African adults 0.9 blocks, and Blue adults 0.65 blocks tall. Feed two adults raw cod or raw salmon near a patch of dry land or snow. The pair walks to a suitable site and works together for about four seconds, pecking at stones as it builds a nest. Each finished nest contains one to three eggs. The eggs hatch after three Minecraft days (72,000 ticks), including nights skipped by sleeping, leaving an empty rock nest. Incubation progress survives a world reload, and moving the clock backward does not extend it. If the nest is unloaded when the eggs are due, they hatch when the chunk ticks again.

Each new penguin has a one-in-one-hundred chance to grow up with yellow-and-white plumage. To collect throwable Penguin Eggs in Survival, break an occupied nest before the eggs hatch. The eggs are also available in Creative mode. Each impact has a 30% chance to hatch one chick.

Original chicks are half a block tall, with grey feathers, orange feet and beaks, and black eyes. New species have their own chick colors and grow to their species' adult size. They become black-and-white adults after 240,000 world ticks (ten Minecraft days, or 3 hours and 20 minutes at normal speed). Feed chicks raw cod or raw salmon to speed up growth. Each fish removes about 10% of the remaining growth time. Time counts while the world is running, including while the chick's chunk is unloaded. Sleeping skips to morning without advancing the growth timer. Adults saved by the original release resize when loaded.

## Build from source

Run `./gradlew build` on macOS or Linux, or `gradlew.bat build` on Windows. The Gradle wrapper downloads the build dependencies on the first run. The project uses a Java 25 toolchain and Gradle 9.2.1. Run Gradle with a Java 25 JDK (`JAVA_HOME` should point to it). Find the resulting mod in `build/libs/`.

Use `./gradlew runClient` to open a development client.

Run `./gradlew runGameTestServer` for server integration checks covering saved penguins, nest hatching, loot, and item components. These development tests are excluded from the release JAR.

Craft four vanilla spectral arrows with one yellow Penguin Feather, one flint, and one stick in any arrangement.

## Penguin horn

Craft a Penguin goat horn by combining any goat horn with a black or yellow Penguin Feather. Blowing it calls all loaded penguins within 100 blocks toward the spot where it was blown. They follow for up to two minutes or until they arrive. Terrain can prevent a path. The horn has the normal goat horn use animation and a seven-second cooldown.

## Penguin Dance

Craft the Penguin Dance music disc with one diamond and eight Penguin Feathers, or find it in the Creative Tools & Utilities tab. Insert it into a jukebox to play a one-minute original electronic track with natural-sounding neural synthesized vocals about penguins. Penguins within 64 blocks bounce, sway, and flap while the disc plays. Swimming, fleeing, combat, and nest building take priority over dancing.

## Santa hats

Craft a Santa Hat with one red wool and one white wool in any arrangement. Wear it in the helmet slot, or right-click a penguin while holding it to put it on the penguin. A hat-wearing penguin dances, jumps, and moves in a small circle on safe dry ground. Sneak and right-click with an empty hand to take the hat back. Swimming, calls from a Penguin horn, fleeing, combat, and nesting can interrupt the dance.

## Penguin species

The original penguins remain in snowy taigas, groves, snowy slopes, and jagged peaks. New flocks include adults and chicks of the same species:

| Species | Minecraft biomes |
| --- | --- |
| Emperor | Snowy plains, ice spikes, frozen peaks, frozen oceans and deep frozen oceans |
| Gentoo | Snowy beaches, frozen rivers, and some stony shores |
| African | Sandy beaches, sharing the habitat with Blue penguins |
| Blue (little blue) | Sandy beaches and some stony shores |

Each species has its own adult and chick plumage. Emperor penguins are largest, Blue penguins are smallest, and chicks are half their species' adult size. Species persist through growth and world reloads. Nest chicks inherit one parent's species, even if the nest is moved to another biome; mixed-species pairs choose one parent for the clutch. Throwable eggs hatch the local biome's species. Existing penguins and nests without a species keep their original appearance. The rare yellow plumage remains exclusive to original penguins.

With cheats, spawn a particular species using `/summon coldpenguins:penguin ~ ~ ~ {Species:"emperor"}` (or `gentoo`, `african`, `blue`). Add `Age:-240000` inside the same tag for a chick.

## Species spawn eggs and concept models

The Spawn Eggs Creative tab contains Penguin, Emperor Penguin, Gentoo Penguin, African Penguin, and Little Blue Penguin Spawn Eggs. Each egg has its own name and palette and always creates its named species, regardless of biome. Use it on the ground for an adult, or on a penguin for a chick of the egg's species. Item IDs are `penguin_spawn_egg`, `emperor_penguin_spawn_egg`, `gentoo_penguin_spawn_egg`, `african_penguin_spawn_egg`, and `blue_penguin_spawn_egg` in the `coldpenguins` namespace.

The four species now follow the supplied adult/chick concept art: narrower upright adult bodies, dark adult faces with white eyes, species-specific head and breast markings, and distinct broad-bodied chicks with larger heads, short dark beaks, mottled down, and dark feet. The original penguin model remains available.

All adult and chick variants have separate pupils that shift with their gaze and make subtle idle eye movements. Pupils remain attached to the animated head during walking, swimming, dancing, and nesting.
