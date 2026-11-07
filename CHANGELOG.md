## v21.1.3
* Added multiple recipe logics and threads per machine, so one machine can run several recipes at once
* Added Astral Sorcery support with lumen and starlight capabilities, traits and recipe conditions
* Added item slots that hold more than a stack, up to a full int
* Added a trait option to keep its data in the dropped machine item, and an item slot option to drop nothing
* Improved the recipe search to go through an index and run only when something a machine uses changes
* Fixed blueprints changing shared recipe data through tags they only read, and added a copy option to the custom data read
* Fixed storage changes not getting a machine saved while its storage renderer is off
* Fixed PneumaticCraft heat moved by neighbours not getting saved while a machine's tick is cancelled
* Fixed the ME interface, pattern provider and PneumaticCraft heat traits being saved off the server thread
* Fixed JEI not showing the recipe ID of MBD2 recipes
* Fixed rebuilt recipes repeating the recipe type's template and sharing lists with their builder
* Fixed the fluid tank GUI IO not restricting clicking fluid in and out of its slots
* Fixed the recipe type search box listing types twice and ignoring the typed word
