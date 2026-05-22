# Trait Definition Type Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Move trait definition construction and capability registration to stable `TraitDefinitionType` instances while preserving existing trait registry ids and project data.

**Architecture:** Keep the `mbd2:trait_definition_type` registry name, but change its values from definition suppliers to unique type objects. Each `TraitDefinition` returns its type, and each type creates definitions and registers block capabilities once per machine definition.

**Tech Stack:** Java 21, NeoForge 1.21.1 capabilities, LDLib2 `AutoRegistry` / `ILDLRegister`, MBD persisted parser codecs, IntelliJ IDEA MCP build validation.

---

### Task 1: Add the Type Abstraction

**Files:**
- Create: `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinitionType.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinition.java`

**Step 1: Add the new type interface**

Create `TraitDefinitionType.java`:

```java
package com.lowdragmc.mbd2.common.trait;

import com.lowdragmc.lowdraglib2.registry.ILDLRegister;
import com.lowdragmc.mbd2.common.machine.definition.MBDMachineDefinition;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.function.Supplier;

public interface TraitDefinitionType<T extends TraitDefinition>
        extends ILDLRegister<TraitDefinitionType<?>, Supplier<TraitDefinitionType<?>>> {
    T createDefinition();

    default void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
    }
}
```

**Step 2: Change `TraitDefinition` to dispatch by type**

Update `TraitDefinition` so it no longer implements `ILDLRegister<TraitDefinition, Supplier<TraitDefinition>>`. Add:

```java
public abstract TraitDefinitionType<?> type();
```

Update `EMPTY` to return `EmptyTraitDefinition.TYPE`.

**Step 3: Verify compile failure**

Run IntelliJ file problems on `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinition.java`.
Expected: errors at registry generic usage and concrete trait definitions missing `type()`.

### Task 2: Move the Registry to Type Values

**Files:**
- Modify: `src/main/java/com/lowdragmc/mbd2/api/registry/MBDRegistries.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinition.java`

**Step 1: Update registry generics**

Change:

```java
AutoRegistry.LDLibRegister<TraitDefinition, Supplier<TraitDefinition>>
```

to:

```java
AutoRegistry.LDLibRegister<TraitDefinitionType<?>, Supplier<TraitDefinitionType<?>>>
```

Use `TraitDefinitionType.class` as the registry base class if the generic API accepts it. If Java rejects `TraitDefinitionType<?>.class`, use the raw class with a localized suppress warning.

**Step 2: Update `TraitDefinition` codecs**

Change codec dispatch to use `TraitDefinition::type`; decoding should use `holder.value().createDefinition()`:

```java
return MBDRegistries.TRAIT_DEFINITION_TYPES.optionalCodec().dispatch(TraitDefinition::type,
        optional -> optional.map(holder ->
                MapCodec.assumeMapUnsafe(PersistedParser.createCodec(holder.value().createDefinition())))
                .orElseGet(() -> MapCodec.unit(EMPTY)));
```

Stream codec should mirror this:

```java
return MBDRegistries.TRAIT_DEFINITION_TYPES.streamCodec().dispatch(TraitDefinition::type,
        holder -> PersistedParser.createStreamCodec(holder.value().createDefinition()));
```

**Step 3: Update manual empty registration**

Register `EmptyTraitDefinition.TYPE` under `empty`, preserving the id.

**Step 4: Run red compile check**

Run IDEA build or `.\gradlew.bat compileJava`.
Expected: concrete trait definition classes fail until they expose `TYPE` and `type()`.

### Task 3: Convert Concrete Trait Definitions

**Files:**
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/entity/EntityHandlerTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/item/ItemSlotCapabilityTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/fluid/FluidTankCapabilityTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/forgeenergy/ForgeEnergyCapabilityTraitDefinition.java`

**Step 1: Move `@LDLRegister` to static type objects**

For non-capability traits:

```java
@LDLRegister(name = "entity_handler", registry = "mbd2:trait_definition_type", group = "trait", priority = -99)
public static final TraitDefinitionType<EntityHandlerTraitDefinition> TYPE = EntityHandlerTraitDefinition::new;

@Override
public TraitDefinitionType<?> type() {
    return TYPE;
}
```

Remove the class-level `@LDLRegister` after adding the type-level annotation.

**Step 2: Apply the same pattern to item/fluid/energy**

For the capability classes, use the simple capability type from Task 4 once it exists. Temporarily add `type()` returning `TYPE` and leave capability registration failing until Task 4.

**Step 3: Run file problems**

Run IDEA file problems on all four modified concrete trait definition files.
Expected: only capability type helper errors remain before Task 4.

### Task 4: Add Type-Level Simple Capability Registration

**Files:**
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/SimpleCapabilityTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/item/ItemSlotCapabilityTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/fluid/FluidTankCapabilityTraitDefinition.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/forgeenergy/ForgeEnergyCapabilityTraitDefinition.java`

**Step 1: Remove `ICapabilityProviderTrait` from `SimpleCapabilityTraitDefinition`**

Keep `getCapability()` and per-trait `getCapContent` helper behavior, but remove `registerCapability(...)` from the definition class.

**Step 2: Add nested simple capability type**

Add a nested class to `SimpleCapabilityTraitDefinition`:

```java
public abstract static class Type<D extends SimpleCapabilityTraitDefinition<T, C>, T, C extends @Nullable Object>
        implements TraitDefinitionType<D> {
    @Override
    public void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(getCapability(), definition.blockEntityType(), (be, context) -> {
            if (be instanceof IMachineBlockEntity machineBlockEntity &&
                    machineBlockEntity.getMetaMachine() instanceof MBDMachine machine) {
                return getCapContent(machine, context);
            }
            return null;
        });
    }

    protected T getCapContent(MBDMachine machine, C context) {
        var contents = machine.getAdditionalTraits().stream()
                .filter(SimpleCapabilityTrait.class::isInstance)
                .map(SimpleCapabilityTrait.class::cast)
                .filter(trait -> trait.getDefinition().type() == this)
                .map(trait -> (T) trait.getCapContent(trait.getCapabilityIO(context)))
                .toArray();
        return merge(contents);
    }

    protected abstract BlockCapability<T, C> getCapability();

    protected abstract T merge(Object[] contents);
}
```

Adjust generics to compile cleanly with Java. If needed, replace stream casting with a small loop and localized `@SuppressWarnings("unchecked")`.

**Step 3: Implement concrete merge strategies**

Item:

```java
@LDLRegister(name = "item_slot", registry = "mbd2:trait_definition_type", priority = -100)
public static final SimpleCapabilityTraitDefinition.Type<ItemSlotCapabilityTraitDefinition, IItemHandler, @Nullable Direction> TYPE =
        new SimpleCapabilityTraitDefinition.Type<>() {
            @Override
            public ItemSlotCapabilityTraitDefinition createDefinition() {
                return new ItemSlotCapabilityTraitDefinition();
            }

            @Override
            protected BlockCapability<IItemHandler, @Nullable Direction> getCapability() {
                return Capabilities.ItemHandler.BLOCK;
            }

            @Override
            protected IItemHandler merge(Object[] contents) {
                return new ItemHandlerList(Arrays.copyOf(contents, contents.length, IItemHandler[].class));
            }
        };
```

Fluid and energy follow the same pattern with `FluidHandlerList` and `EnergyStorageList`.

**Step 4: Remove definition-level merged `getCapContent` overrides if redundant**

If the new type merge handles all same-type traits, remove the overrides in item/fluid/energy definitions that manually scan `machine.getAdditionalTraits()`.

### Task 5: Deduplicate Registration in Machine Definition

**Files:**
- Modify: `src/main/java/com/lowdragmc/mbd2/common/machine/definition/MBDMachineDefinition.java`
- Delete or deprecate: `src/main/java/com/lowdragmc/mbd2/common/trait/ICapabilityProviderTrait.java`
- Modify: `src/main/java/com/lowdragmc/mbd2/common/trait/ITrait.java`

**Step 1: Replace instance-provider loop**

Use type-level dedupe:

```java
var registeredTypes = new HashSet<TraitDefinitionType<?>>();
for (var traitDefinition : machineSettings.traitDefinitions()) {
    var type = traitDefinition.type();
    if (registeredTypes.add(type)) {
        type.registerCapabilities(this, event);
    }
}
```

**Step 2: Remove stale imports and docs**

Remove `ICapabilityProviderTrait` imports and update `ITrait` docs to refer to `TraitDefinitionType#registerCapabilities`.

**Step 3: Decide interface fate**

If no active code references `ICapabilityProviderTrait`, delete the file. If integrations still compile against it, mark it `@Deprecated(forRemoval = true)` and leave it unused for one compatibility cycle.

### Task 6: Validate and Commit

**Files:**
- All files touched above.

**Step 1: IDEA inspections**

Run `get_file_problems(errorsOnly=true)` for:

- `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinition.java`
- `src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinitionType.java`
- `src/main/java/com/lowdragmc/mbd2/common/trait/SimpleCapabilityTraitDefinition.java`
- `src/main/java/com/lowdragmc/mbd2/api/registry/MBDRegistries.java`
- `src/main/java/com/lowdragmc/mbd2/common/machine/definition/MBDMachineDefinition.java`

Expected: no errors.

**Step 2: Build**

Run IDEA `build_project` first. If unavailable or insufficient, run:

```powershell
.\gradlew.bat compileJava
```

Expected: compile succeeds.

**Step 3: Compatibility spot check**

Search for old ids and confirm they remain unchanged:

- `empty`
- `entity_handler`
- `item_slot`
- `fluid_tank`
- `forge_energy_storage`

**Step 4: Commit only refactor files**

Check `git status --short` carefully because this worktree already has unrelated changes. Stage only files changed for this refactor:

```powershell
git add -- src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinitionType.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/TraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/SimpleCapabilityTraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/ITrait.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/ICapabilityProviderTrait.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/entity/EntityHandlerTraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/item/ItemSlotCapabilityTraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/fluid/FluidTankCapabilityTraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/common/trait/forgeenergy/ForgeEnergyCapabilityTraitDefinition.java `
    src/main/java/com/lowdragmc/mbd2/api/registry/MBDRegistries.java `
    src/main/java/com/lowdragmc/mbd2/common/machine/definition/MBDMachineDefinition.java
git commit -m "refactor: register trait capabilities by type"
```

Do not stage unrelated files already present in the worktree.
