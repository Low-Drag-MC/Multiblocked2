Multiblocked 2
==================
Multiblocked2 (mbd2) is the more powerful visual custom machine/multi-block structure mod after mbd1(multiblocked). Mbd2 is an extremely flexible yet vanilla-esque multiblock mod, that embraces aspects of MultiblockTweaker and Modular Machinery.

Original project can be found here: [**Multiblocked**](https://www.curseforge.com/minecraft/mc-mods/multiblocked "multiblocked")

Discord: [https://discord.com/invite/sDdf2yD9bh](https://discord.com/invite/sDdf2yD9bh)

Github: https://github.com/Low-Drag-MC/Multiblocked2

CurseForge: https://www.curseforge.com/minecraft/mc-mods/multiblocked2

Modrinth: https://modrinth.com/mod/multiblocked2

QQ: 827533873

```gradle
repositories{
    maven {
        name "firstdarkdev"
        url "https://maven.firstdark.dev/snapshots"
    }
}

dependencies {
    implementation fg.deobf("com.lowdragmc.ldlib:ldlib-forge-{minecraft_version}:{latest_version}") { transitive = false }
    implementation fg.deobf("com.lowdragmc.multiblocked2:Multiblocked2:{minecraft_version}-{latest_version}") { transitive = false }
}
```

What's new
------------------
### Demo
![demo](https://i.ibb.co/2ZLFgFb/demo.gif "demo")

Demo: A fire pedestal burn coals to emi fire and light surroundings. Just imagine how amazing it is to create such a cool machine without writing a single line of code!
### New Editor
![editor](https://i.ibb.co/XVFC0mm/editor.png "editor")

New editor: more powerful, more modern and easier to use. mbd2 helps you work without code, provides a unity-like game design engine, more efficient customization of your machine.

### Logic Node Graph
![node graph](https://i.ibb.co/7bqL3j6/nodegraph.png "node graph")

Node Graph: The node graph is a brand-new way to configure event logic. You no longer need KubeJS to define complex functionality. If you're a user of Blender's Node system, Unreal's Blueprints, or Unity's Shader Graph, you're going to love this new feature.

MBD2 allows you to listen to various machine events through the node graph and set up a wide range of execution logic, giving you immense creative power.
