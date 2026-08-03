# ChangeLogs
## v21.0.11
* Fixed gadgets item mode serialization (by @andriihorpenko)
* Fixed fog affecting in-world preview rendering (by @andriihorpenko)
* Fixed recipe conditions not checking multiblock parts
* Fixed ME interface fluid capacity being ignored by the config slot
* Fixed triggerGeckolibAnim not resolving resource-backed renderers
* Fixed Auto IO ignoring Allow Same Fluid
* Added separated area preview radius
* Fixed XEI structure parts being capped at one stack per block type

## v21.0.10
* Fixed ChemicalSlot syncing
* Fixed Multiblock XEI page candidates display and lookup

## v21.0.9
* Fixed failed to notify capability changes
* Fixed gradle dependencies chain
* Fixed dev env requiring mod installation

## v21.0.8
* Fixed crash when adding a controller candidate block
* Fixed machine level condition crash

## v21.0.7
* Fixed fuel recipe searching

## v21.0.6
* Fixed kjs RecipeSchema convertor

## v21.0.5.a
* Fixed kjs RecipeSchema convertor

## v21.0.5
* Fixed incorrect recipe slots bindings for 10+ indexes (Thanks for the PR #209, @andriihorpenko)
* Fixed Mekanism ChemicalSlot rendering (Thanks for the PR #210, @andriihorpenko)
* Switched to EMI synthetic id for multiblock info category (Thanks for the PR #211, @andriihorpenko)
* Bumped up ldlib2
* Fixed kjs event post

## v21.0.4
* Fixed KubeJS inputChemical/outputChemical parsing for Mekanism (Thanks for the PR #206, @andriihorpenko)
* Fixed capContent always returning non-null empty handler list (Thanks for the PR #207, @andriihorpenko)
* Fixed KubeJS recipes not syncing from dedicated server (Thanks for the PR #208, @andriihorpenko)

## v21.0.3
* Fixed typos in en_us.json (Thanks for the PR #202, @vainangei)
* Added KJS recipe support (Thanks for the PR #204, @andriihorpenko)
* Fixed EMI recipe id due to reference equality check (Thanks for the PR #205, @andriihorpenko)
* Fixed the huge structure rendering performance
* Fixed predicate missing logs
* Improved performance by conditional sync
* Fixed Auto IO behavior
* Added ALL IO selector

## v21.0.2
* Port ae2 integration to 1.21.1 (#199, thanks @OmiLabsDev)
* Fixed recipe execution
* Fixed the event name
* Fixed custom machine shape rendering
* Added system msg for gadget mode switching
* Fixed ingredient merge issue
* Fixed breaking particles
* Fixed the server loading crash
* Added ME Pattern Provider Trait

## v21.0.1
* Port to 1.21