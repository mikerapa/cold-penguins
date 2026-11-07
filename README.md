# Cold Penguins

A mod for Minecraft Java Edition 1.21.1 with NeoForge 21.1.232 or a newer 21.1 release.

## Play

1. Install NeoForge for Minecraft 1.21.1.
2. Remove the previous Cold Penguins JAR if you are upgrading. Put `cold-penguins-1.9.1.jar` in that installation's `mods` folder. For multiplayer, install it on the server and on each client.
3. Launch the NeoForge profile. Look for penguins in snowy plains, snowy beaches, snowy taigas, and other frozen biomes. Natural spawn attempts happen on dry land and form flocks of five to ten with both adults and chicks. Terrain and the passive mob cap can leave smaller flocks. Penguins from the same spawn group remember their family and try to stay within 30 blocks of each other, including after a world reload. In Creative mode, the Penguin Spawn Egg is in the Spawn Eggs tab. With cheats enabled, you can also use `/summon coldpenguins:penguin` in an open area.

Right-click a penguin with an empty main hand to pet it. Hearts and a contact call show that it was petted. Penguins occasionally call while walking and sometimes flap their flippers on land. The call is usually a trill, with a one-in-five chance of a honk. Both calls use field recordings. The trill is from a little penguin, and the honk is from a Galápagos penguin. See the audio credits packaged in the JAR. Penguins honk when they breed or take damage. Adults fight back against their attacker, while chicks flee. Penguins can dive and swim at any depth without drowning. They jump up toward shore waypoints to climb out onto full-block banks and ice. While swimming, they hunt and eat cod and salmon within 100 blocks. They head for land if no prey is present or the three-minute swim limit is approaching. At the limit, a penguin still in the water returns directly to a safe shore if one is available. Penguins keep looking for an exit when no safe shore can be found. Attacks against players deal one damage point (half a heart) before armor and other reductions. Petting drops one Penguin Feather. Yellow-and-white adults drop yellow feathers, while other penguins drop black feathers. Penguins also drop a feather of the same color on a damaging hit they survive or when they die, subject to Minecraft's mob loot rule.

Craft four Black-Tipped Arrows with one flint, one stick, and one Penguin Feather in any arrangement. The arrow has the regular arrow silhouette with an extended black tip. Use them in bows, crossbows, or dispensers. They travel at standard arrow speed in air and cover twice the distance per movement step underwater. Reduced water drag gives them approximately four times a normal arrow's horizontal range in uninterrupted water.

Adult penguins are one block tall. Feed two adults raw cod or raw salmon near a patch of dry land or snow. The pair walks to a suitable site and works together for about four seconds, pecking at stones as it builds a nest. Each finished nest contains one to three eggs. The eggs hatch after three Minecraft days (72,000 ticks), including nights skipped by sleeping, leaving an empty rock nest. Incubation progress survives a world reload, and moving the clock backward does not extend it. If the nest is unloaded when the eggs are due, they hatch when the chunk ticks again.

Each new penguin has a one-in-one-hundred chance to grow up with yellow-and-white plumage. To collect throwable Penguin Eggs in Survival, break an occupied nest before the eggs hatch. The eggs are also available in Creative mode. Each impact has a 30% chance to hatch one chick.

Chicks are half a block tall, with grey feathers, orange feet and beaks, and black eyes. They become black-and-white adults after 240,000 world ticks (ten Minecraft days, or 3 hours and 20 minutes at normal speed). Time counts while the world is running, including while the chick's chunk is unloaded. Sleeping skips to morning without advancing the growth timer. Adults saved by the original release resize when loaded.

## Build from source

Run `./gradlew build` on macOS or Linux, or `gradlew.bat build` on Windows. The Gradle wrapper downloads the build dependencies on the first run. The project uses a Java 21 toolchain. Find the resulting mod in `build/libs/`.

Use `./gradlew runClient` to open a development client.

Craft four vanilla spectral arrows with one yellow Penguin Feather, one flint, and one stick in any arrangement.

## Penguin horn

Craft a Penguin goat horn by combining any goat horn with a black or yellow Penguin Feather. Blowing it calls all loaded penguins within 100 blocks toward the spot where it was blown. They follow for up to two minutes or until they arrive. Terrain can prevent a path. The horn has the normal goat horn use animation and a seven-second cooldown.

## Penguin Dance

Craft the Penguin Dance music disc with one diamond and eight Penguin Feathers, or find it in the Creative Tools & Utilities tab. Insert it into a jukebox to play a one-minute original electronic track with natural-sounding neural synthesized vocals about penguins. Penguins within 64 blocks bounce, sway, and flap while the disc plays. Swimming, fleeing, combat, and nest building take priority over dancing.

## Santa hats

Craft a Santa Hat with one red wool and one white wool in any arrangement. Wear it in the helmet slot, or right-click a penguin while holding it to put it on the penguin. A hat-wearing penguin dances, jumps, and moves in a small circle on safe dry ground. Sneak and right-click with an empty hand to take the hat back. Swimming, calls from a Penguin horn, fleeing, combat, and nesting can interrupt the dance.
