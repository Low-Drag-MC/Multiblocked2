# Trait Definition Type Design

## Goal

Refactor trait definition registration so the registry stores stable trait type instances instead of raw definition constructors, while keeping existing persisted trait ids compatible.

## Context

`MBDMachineDefinition.registerCapabilities` currently runs before product-loaded `machineSettings` is fully populated in the post task. Capability registration also happens per `TraitDefinition`, which causes duplicate registrations when a machine has multiple definitions of the same capability trait type, such as several forge energy traits. NeoForge only needs one provider per `BlockCapability` and block entity type, and the first non-null provider result wins, so duplicate registration is both noisy and semantically fragile.

`ConfigPartSettings.ProxyCapability` is also awkward under the current design because the registration phase wants to know all possible block capabilities up front, while proxy behavior depends on controller traits that are only meaningful at runtime.

## Compatibility

Existing persisted ids must remain valid. Registry names such as `forge_energy_storage`, `item_slot`, `fluid_tank`, and `entity_handler` should continue to decode old project data without migration.

The `mbd2:trait_definition_type` registry name can stay the same, but its value should become a `TraitDefinitionType<?>` instead of a `Supplier<TraitDefinition>`.

## Recommended Architecture

Add `TraitDefinitionType<T extends TraitDefinition>` as the unique per-kind type object. The type owns creation and type-level behavior:

```java
public interface TraitDefinitionType<T extends TraitDefinition> {
    T createDefinition();

    default void registerCapabilities(MBDMachineDefinition definition, RegisterCapabilitiesEvent event) {
    }
}
```

`TraitDefinition` should expose its type directly:

```java
public abstract TraitDefinitionType<?> type();
```

`TraitDefinition.CODEC` and `TraitDefinition.STREAM_CODEC` should dispatch through `type()` and use the registered `TraitDefinitionType` to create or parse concrete definitions. This preserves the stored registry id while moving constructor ownership out of the definition class itself.

Capability registration should move from definition instances to type instances. `MBDMachineDefinition.registerCapabilities` should collect the relevant types from `machineSettings.traitDefinitions()`, deduplicate them, and call `type.registerCapabilities(this, event)` once per type.

## Capability Design

Introduce a reusable simple capability type for traits extending `SimpleCapabilityTraitDefinition<T, C>`.

The type registers one NeoForge provider for a machine definition and a `BlockCapability<T, C>`. The provider should:

1. Resolve the `MBDMachine` from the block entity.
2. Scan runtime `machine.getAdditionalTraits()`.
3. Select traits whose definition has the matching `TraitDefinitionType`.
4. Ask each trait for side/context-aware content.
5. Merge the results through a type-provided strategy.

Existing concrete capability definitions should provide a static `TYPE`:

- `ItemSlotCapabilityTraitDefinition.TYPE` merges with `ItemHandlerList`.
- `FluidTankCapabilityTraitDefinition.TYPE` merges with `FluidHandlerList`.
- `ForgeEnergyCapabilityTraitDefinition.TYPE` merges with `EnergyStorageList`.

`ICapabilityProviderTrait` can be removed from the active API because capability behavior belongs to the type, not individual persisted definitions.

## Proxy Capability Direction

This refactor should leave a clean hook for part proxy capabilities without requiring full proxy implementation in the first pass.

The key change is that supported block capabilities are known from `TraitDefinitionType` instances at registration time. Runtime provider logic can later inspect `ConfigPartSettings.proxyControllerCapabilities()` and controller traits to return proxy contents. That avoids needing fully loaded part settings just to decide whether a block entity type can ever expose a capability.

## First Implementation Scope

The first implementation should:

- Add `TraitDefinitionType`.
- Change `MBDRegistries.TRAIT_DEFINITION_TYPES` to register trait type instances while preserving registry ids.
- Update `TraitDefinition.CODEC` and `STREAM_CODEC`.
- Add static `TYPE` instances to current concrete trait definitions.
- Move simple capability registration to type-level code.
- Deduplicate capability registration per machine definition.
- Remove or stop using `ICapabilityProviderTrait`.
- Keep project data format compatible.
- Validate with IntelliJ build or `compileJava`.

Proxying controller capabilities can be implemented in a later pass using the new type-level hook.
