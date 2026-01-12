# ChangeLogs

## v1.0.38.a
* hot fix: disable formed rendering

## v1.0.38
* Fixed EMI workstation display bug (Thanks @FalAut)
* Added player access in MachineUIEvent (Thanks @FalAut)
* Fixed Schedule State Animation do not work (Thanks @FalAut)
* Bump up ldlib
* Fixed multiblock preview does not show block predicates with EMI (Thanks @FalAut)
* Fixed machine blocs are invisible in the create recipe

## v1.0.37
* Fixed RecipeUIEvents doesn't work
* Added player and hand for CatalystEvent

## v1.0.36
* Update zh_cn.json (thanks to @FalAut)
* Added `onRecipeFinish` event, called after output
* Added `onConsumeInputsAfterWorkingEvent` event, call after input handled

## v1.0.35
* Fixed cannot open ui while rejoining the world
* Fixed liquid bucket consumed while using the multiblock builder
* Added event to modify recipe/fuel recipe ui

## v1.0.34.a
* Fixed support for rendering radius and global renderer.

## v1.0.34
* Fixed proxy recipes incompatible with kjs recipe inject event.
* Added support for rendering radius and global renderer.
* Move trait dropping to the onDrop event.

## v1.0.33
* Fixed fly wheel instance doesn't render with embeddium
* Added searching feature for the biome / dimension condition
* Support the APIs of the customizable machine name

## v1.0.32
* Fixed Fuel Recipe Type doesn't show up in the JEI

## v1.0.31.a
* Fixed MBD Java Registry System

## v1.0.31
* Fixed World Preview renderer doesn't work correctly with forge model
* Added PNC Pressure Condition
* Improve XEI recipe viewer control. (added control for proxy recipe, and specific recipe)

## v1.0.30
* Fixed Forge Additional Capabilities doesn't work
* Added Light Condition for `sky light`, `block light` and `can see sky`
* Added Consume Inputs After Working (furnace-like machine)
* Added ME Interface Trait

## v1.0.29
* Fixed Mek EMI support
* Fixed Photon node crash
* Added an option for whether the block is `collisionShapeFullBlock`
* Added machine translated ru_ru lang file (thanks to @серый калчедан)
* Added fluid auto world io

## v1.0.28.b
* Fixes the pattern lock won't release.

## v1.0.28.a
* Fixes the pattern lock won't release.

## v1.0.28
* Added Create Trait UI to display the rpm and stress of the machine
* Added Trait Capability IO Override (thanks to @Tcat2000)
* Fixed crash while the ui name contains bracket-like characters
* Fixed PNC Proxy Block crash
* Improved the Tag Ingredient Copy

## v1.0.27
* Fixed Tank Widget GUI IO reversed
* Fixed disable recipe logic settings doesn't work
* Fixed After Recipe Working Event also triggers when structure is destroyed
* Fixed Multiblock proxy parts don't export pressure
* Added try-catch to prevent crash when trying to execute kjs scripts
* Added TransferProxyRecipeEvent to allow customizing transfer proxy recipe behavior
* Added MBDRecipeType#recipeBuilder to build dynamic recipe as kjs recipe event
* Added supports that the part can display its controller trait's ui and the controller can display its part's ui
* Bump up photon version, and fix photon version compatibility
