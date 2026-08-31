## v21.1.0
* Added machine blueprints: a node graph that gives a machine its logic, edited in-game and stored with the definition
* Added runtime values so a machine can override its definition's config per placed instance
* Added twelve built-in blueprints, ready to bind and to read as worked examples
* Added recipe content nodes so a blueprint can change what a recipe consumes and produces, not just how much
* Added recipe payload nodes for entity, Mekanism, Create and PneumaticCraft capabilities
* Added an auto IO side panel to the machine UI, built entirely from a blueprint, with its look in a stylesheet packs can override
* Added Photon effect support
* Added KubeJSRenderer and CustomRendererEvent for custom render logic from KubeJS (Thanks for the PR #234, @Tcat2000)
* Improved the machine FX editor so one view owns both effect lists and the preview
* Fixed Auto IO not working on proxyWhileFormed predicate proxies
* Fixed proxyWhileFormed ports not rendering BER-based renderers
* Fixed two machine events that were hooked everywhere but never posted
* Fixed two recipe conditions crashing the game through missing toString calls (Thanks for the PR #233, @Tcat2000)
* Fixed some default models having bad hand display settings (Thanks for the PR #232, @Tcat2000)
