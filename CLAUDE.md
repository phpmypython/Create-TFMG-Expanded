# CLAUDE.md

Guidance for AI coding agents (and humans) working in this repository.

## What this is

**Create: TFMG Expanded** is a continuation of Create: The Factory Must Grow by DrMangoTea — a NeoForge 1.21.1 mod
adding industrial metallurgy, oil processing, chemistry, engines and electricity to Create — on its own line, on top of
which this project adds root-cause fixes and new industrial mechanics (sulfur recovery, pipe goggle readouts, pipeline
pump stations, more on the issue board). The tree is based on the original mod and is MIT-licensed throughout. AI
tooling is used openly here, under an experienced maintainer's direction. Read
[README.md](README.md) ("Why it exists" and "How it's built") and
[CONTRIBUTING.md](CONTRIBUTING.md) first — the contribution rules there apply to agents exactly as they do to
people. Nothing is merged until a human (me) reviews it.

Mod id `tfmg`. Java 21, NeoForge 21.1, Create 6.0.x, Ponder 1.0.x, Registrate. Java sources live under
`src/main/java/com/drmangotea/tfmg/`; hand-written assets under `src/main/resources/`; generated assets and data
under `src/generated/resources/` (committed, produced by datagen — never edit by hand).

## Building and running

```
./gradlew build                 # jar in build/libs/
./gradlew runData               # regenerate src/generated/resources after recipe/lang/model/tag changes
./gradlew runClient             # dev client
./gradlew runServer             # dev dedicated server
```

* Needs a JDK 21 on `JAVA_HOME`. The build is a Groovy `build.gradle` on NeoForge ModDevGradle.
* `runData` boots a headless game and takes minutes. Run it whenever you touch anything under
  `datagen/`, ponder scene text, recipe generators or lang, and commit the regenerated files with the change.
* Adding a fluid or item needs its texture (e.g. a bucket sprite) before datagen will pass.
* Every push is built by GitHub Actions (`.github/workflows/build.yml`). Releases are cut by pushing a `v*`
  tag or running the Release workflow (`release.yml`); the version becomes `mod_version`, the jar name and the
  GitHub release. Versions are semantic — `MAJOR.MINOR.PATCH` — on this project's own line: fixes bump patch,
  new mechanics bump minor. The workflow rejects anything else and keeps `updates.json` (the in-game update
  checker's manifest) current.

## Where things are

| Area | Package under `com.drmangotea.tfmg` |
|------|-------------------------------------|
| Registries (blocks, items, fluids, block entities, tags, recipe types) | `registry/` |
| Machines (blast furnace, blast stove, coke oven, distillation, vats, winding machine, air intake, casting) | `content/machinery/` |
| Electricity (networks, generators, motors, polarizer, cables, transformers) | `content/electricity/` |
| Engines and fuels | `content/engines/`, `registry/TFMGEngineFuelTypes` |
| Pipes and decoration | `content/decoration/` |
| Recipe types and serializers | `recipes/` |
| Data generators (recipes, tags, lang, models) | `datagen/` |
| Ponder scenes, tags and registration | `ponder/` (schematics in `src/main/resources/assets/tfmg/ponder/*.nbt`) |
| JEI / other mod integration | `integration/` |
| Config | `config/` |

## How work is done here

* **Root cause with evidence.** Find the actual failing code path before changing anything. Decompiling Create
  or upstream jars, reading world/region data and reproducing on a dedicated server are all normal here.
  Do not swap out a subsystem on a theory; make the smallest change that fixes the verified cause.
* **Verify in game.** State what was tested and where (client, dedicated server, both). Compiling is not
  verification. Gameplay and content changes (recipes, balance, new mechanics, ponders) go on a branch and are
  playtested by a maintainer before they land on `1.21.1`; verified bug fixes may land directly.
* **One change per commit**, message written so a reviewer can follow the reasoning without you present.
  Professional tone; a `Co-Authored-By` trailer for an AI assistant is fine.
* **World-safe.** Keep ids, data components and NBT formats compatible with existing worlds. Note that adding
  a fluid, block or item is a registry change: clients and servers must update together.
* **Match the surrounding code.** Same naming, comment density and idioms as the file you are in. Don't add
  dependencies or reinvent something Create already provides.
* **Upstream.** This tree is based on `DrMango14/Create-The_Factory_Must_Grow` at commit `ed19921e` (tag `base-1.2.3`).
  Nothing after that commit is merged, cherry-picked or retyped from upstream; fixes are re-derived here from the bug
  itself.

## Verified facts that are easy to get wrong

These were established by reading Create 6.0.10 / TFMG code and confirmed in game. Check them against the
current code before relying on them, but do not "correct" them from memory.

* **Registrate `FluidEntry.get()` returns the FLOWING fluid; `getSource()` the still one.**
  `BaseFlowingFluid.isSame` accepts both, but `FluidTank.fill` / `FluidStack.isSameFluidSameComponents` do not.
  Validators and recipe outputs usually want `getSource()`.
* **Create fluid networks capture their source once.** `FluidNetwork` keeps the source capability provider
  it first obtained, and `BlockCapabilityCache` providers go permanently invalid on the first invalidation.
  A block entity that calls `invalidateCapabilities()` every tick (or every lazy tick) will silently stall
  every pump network attached to it. Only invalidate when the handler object actually changes.
* **Pump pressure is |RPM| divided among branches, no distance decay**, BFS range 16
  (`PumpBlockEntity.distributePressureTo`); the electric pump overrides this method, which is the hook for any
  pressure model. Equal in/out pressure at a face resolves to OUTBOUND (`PipeConnection.manageFlows`).
* **Smart fluid pipe filters only apply when the smart pipe is the block touching the source**
  (`SmartPipeBehaviour.canPullFluidFrom`). To pull one fluid out of a multi-output vat: vat → smart pipe →
  pump → tank, never vat → pump → smart pipe.
* **Mechanical pumps are `ICogWheel`s** with rotation axis = flow axis: drive them with a cogwheel beside them
  on a parallel axis, not with a shaft on the pipe faces. The air intake takes its shaft on
  `FACING.getOpposite()` and exposes its fluid capability on every face.
* **Polarizer power check:** it charges only while `getPowerUsage() >= recipe.energy` (2000 W by default), with
  `getPowerUsage() = V * I`, `I = V / 30 Ω` while charging → needs ≥ 245 V. Small generator:
  `V = min(255, (|RPM| − 40) × 1.4)`, so ≥ 215 RPM. The generator's own wattage is irrelevant to this check.
* **Blast stove** gate is `timer >= 1000 / (width² × height × 3)` and, as in the original mod, the timer is a
  one-time warm-up — do not reset it per cycle (that was a 20× nerf). Output is 25 mB hot air per tick once
  warm; the blast furnace consumes 20 mB per progress tick.
* **Vat heat** is the sum of `BoilerHeater` values under the footprint (kindled blaze burner 1, seething 2,
  firebox 2); recipes gate on `heatLevel`.
* **Winding machine** belt input: accept one item per insert into an empty slot.

## Ponder scenes

* Register a block's own scene before any cross-cutting scene that also lists that block: scenes play in
  registration order. The `tags` argument of `addStoryBoard` only sets the scene's related tags; a block
  appears in an index category only if added via `HELPER.addToTag(TAG).add(...)` in `TFMGPonderTags`.
* Ground every scene in the real-world process, then say exactly what the block needs in game (which face
  takes rotation, heat level, voltage, fluids). Use in-game names for blocks and fluids; do not introduce a
  second name for something the game already names (e.g. say "Furnace Gas", not "sweet gas").
* Schematics are vanilla structure NBT (`DataVersion` 3955 for 1.21.1). Ship them with their rotation
  sources (creative motors, cogs) placed outside the shown selections so players can place the schematic as a
  working build; block-entity NBT such as smart-pipe filters is honoured.
* The default camera sits north-north-west of the base plate looking south-east; `rotateCameraY` is relative
  and eases exponentially. Captions shown while the camera is still creeping wobble (Ponder only pixel-snaps
  text once `settled()`); after a turn, idle 60–70 ticks and then snap the yaw to its chase target before the
  next caption. Item entities cannot be identified with the identify key — add a `showControls(...).withItem`
  bubble instead.
* Ponder text durations: roughly 5 ticks per word plus 30; idle 10–20 ticks longer than the text.

## Datagen and assets

* Recipes, tags, lang and models are generated. Change the generator under `datagen/`, run `runData`, commit
  both. Ponder text keys (`tfmg.ponder.<scene>.text_N`) are generated from the scene code.
* Fluid tank icons share a sprite with a colour stripe; a new fluid needs `textures/item/<name>_bucket.png`.

## Issue tracking

Bugs, agreed features, and not-yet-fleshed-out ideas are tracked as GitHub issues (labels `bug`, `feature`,
`idea`, `needs-verification`, `ponder`) on the repository project board. File the issue before starting
non-trivial work; link it from the pull request.
