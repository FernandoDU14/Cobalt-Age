![Cobalt Age Title](https://cdn.modrinth.com/data/pmieAiUD/images/338f64a95e6c1b6f64dfd4c59909b473dcbbe5f7.png)

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/txWQN8CtEV)
[![GitHub Repo](https://img.shields.io/badge/GitHub-Cobalt--Age-blue?style=for-the-badge&logo=github)](https://github.com/FernandoDU14/Cobalt-Age)
[![Support us on Ko-fi](https://img.shields.io/badge/Ko--fi-Donate-0775a8?style=for-the-badge&logo=kofi&logoColor=blue)](https://ko-fi.com/cobaltage)

</div>

# About

**Cobalt Age** adds a new Overworld mineral: **Cobalt**.

The cobalt comes with its own set: Cobalt Ore (Stone and Deepslate), Raw Cobalt, Cobalt Ingot, Cobalt Nugget, Cobalt Block and Raw Cobalt Block.

By that, you may combine a **Cobalt Nugget** with Redstone, obtaining the **Cobalt Dust**, the base of a new energy system that behaves similarly to Redstone while providing several unique advantages:

* **Optimized Modern Signal Engine**, eventually toggable in configs, which does not cause even a minimum TPS or FPS change.
* **Fully independent from Redstone** signals.
* Can be **waterlogged**.
* Underwater automation and circuitry (consequence of Waterloggability)
* Prevents water flows from breaking dust networks (consequence of Waterloggability)
* Other than the **Cross** and **Dot** states, which are already in Redstone, it has the **Retracted** state. Allowin the Cobalt Dust to be smarter than redstone (check the last image in "Cobalt Age Showcase" down here for clarifications)
* Comes with a set of Cobalt Energy Blocks (CEBs):
  * Cobalt Dust
  * Cobalt Dust Block
  * Cobalt Repeater
  * Cobalt Comparator
  * Cobalt Torch
  * Cobalt Rail
  * Cobalt Relay
  * Cobalt Lamp
  * Converter
* Comes with a set of Decorative Blocks:
  * Cobalt Block
  * Raw Cobalt Block

As you may guess, the Converter will work as a bridge for the two systems.


<details>
  <summary><b>Spoiler: Cobalt Age Showcase</b></summary>

Complete Cobalt Age Set:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/8c62956fd665ebcbc4b5c1a2b9da64386651ef1e.png" width="600">

Cobalt Block is compatible with the Beacon:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f286a6a10b49206eb21d6fa36380037aab3ec387.png" width="600">

The new beacon GUI, the cobalt ingot has been added in the GUI.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f0f20e6f3c316db775db8575b887e1e39f6fc661.png" width="600">

Water does not break the blocks:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/9fbce9537f742639174d6f05761735af7569c610.png" width="600">

A closer look into the retracted state:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c11253b7c3f55c2d53ce56a89d1909f16db8128d.gif" width="600">



</details>

## High-Performance Signal Engine

Cobalt wires eventually use a **modern optimized signal propagation system** (toggable with `/cobaltage on` and `/cobaltage off`) capable of dammatically reduce computation costs compared to vanilla Redstone. The reason are mainly because the new engine:

1. Minimizes the number of times a cobalt wire checks its surroundings to determine its power level.

2. Minimizes the the number of block and shape updates emitted.

3. Minimizes memory allocation using bit masks instead of calling the world states

4. Minimizes cpu calculations saving each network in a virtual ram

5. Skips rendering block updates when signal changes

Unlike alternative optimization approaches, cobalt wires and relays eventually preserve vanilla update order and behaviour, including mechanics that rely on update timing and ordering, such as dupe machines.

Benefits are:
* **No computational cost**
* **No TPS change**
* **No FPS change**
* Maintains **compatibility with all mods** which may include more signal absorbers (Consumers) and more signal emitters (Emitters), or even signal modification mods like Alternate Current (which does optimisation to redstone in a similar way Cobalt Age does).

<details>
  <summary><b>Performance Tests</b></summary>
Scatter Test in a 100 x 100 wire linked web with 20 observer clocks. Cobalt dust does not cause not even a minimum server lag or client FPS drops (10 ms over 50 ms), while redstone dust cause a huge lag and a huge delay of 2.8 seconds from a tick to another one (130+ ms over 50ms). You can see the delay in the redstone update signals in the shown videos.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/bf714ef2ceeb743557d05f8af0736569816c275c.gif" width="600">
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/684fb57e4a285c13bc14b1d532861ceab9c6c08e.gif" width="600">
</details>

## Independent Energy Systems

Cobalt Energy Blocks (CEBs) and Redstone Energy Blocks (REBs) completely operate independently. They **emit signals on different channels**. You may think the system will break when you try to transfer the a signal through a block which is in the set of the other signal channel (example a redstone torch behind a cobalt dust block). Well, it will not break, as the **signal channels will not interfere with each other** (because of their independent signal channel).

Two reciprocal examples may be:
* Redstone Dust Blocks can receive a cobalt signal from a CEBs and because of that emit a cobalt signal to their neighbours
* Cobalt Dust Blocks can receive a redstone signal from a REBs and because of that emit a redstone signal to their neighbours

A pratical meaning of this may be:

* A redstone repater poiting a Cobalt Dust Block, which then transfers the energy to a redstone torch, and viceversa at switched roles.

<details>
  <summary><b>Spoiler: Practical example of independence</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c87c6887d0747a4aaa11492dfad20d88a5f03dc3.gif" width="600">

</details>

<details>
  <summary><b>Spoiler: Parallel Wires</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/2ddbfd49a3af63083fbf0f61a4956351725d607f.png" width="600">

</details>

<details>
  <summary><b>Spoiler: More independece examples</b></summary>


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/14ff842ece2cc859da4cfb8707bd1c551961c4aa.png" width="600">

</details>


<details>
    <summary><b>Spoiler: Even more examples</b></summary>

Showcase of the Glass Diode for Cobalt Wires. The logic has been inherited by Redstone Wires.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/efab130c1c7e28c4aa6512de7773c344a870f1ac.png" width="600">

ON / OFF Showncase images for mixed blocks underwater and surface.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c21df3c9a33d592c43a742aca0e6347d3d95cee1.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/336f536ad29d8e5541418083ab3373c8dcf5c433.png" width="600">

</details>

### Cobalt Rail

A faster alternative to Powered Rails, which has 24 blocks per second (BPS) as minecart speed by default (but you can change it as you want thorugh a gamerule), as it is meant for long distance travels. The vanilla minecart speed for Powered Rails is 8 BPS.

**Note:** Supports experimental `minecart improvements` feature.

The custom gamerule that has been added lets you **change the speed of both cobalt and powered rails**, the format is the following:
`gamerule cobaltage:max_minecart_speed_<type> <value>`, where 'type' is the type of rail and 'value' is the speed in BPS. Keep in mind that it is also **mandatory to change the gamerule `max_minecart_speed`** as this works as the maximum minecart speed theoretically reachable. The real maximum speed will be defined by the two gamerules though, so it is suggested to set the `max_minecart_speed` to a value which is greater or equals the maxium between the cobalt and powered rail minecart speed.

Despite the fact that the speed of both types of rails can be modified, we recommend that at least one of the rails keeps the vanilla minecart speed (8 BPS).

### Converter
The converter works as a bridge between cobalt and redstone signal worlds. It is a block that let the player transform the energy signal from one type to the other one. This also helps to ensure there is a way to inject moddded Emitters signals to cobalt wires if they are planned to be used.


Further properties:
* Reads energy the same way as comparators / repeaters. It will emit the same energy as it takes in input as a converted output, and it will do it the same way as comparators / repeaters.

<details>
  <summary><b>Spoiler: Converter</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/142887088c12929925af6f69e5f4af197c9596fd.png" width="600">


Converter ON / OFF Lever behavior. You can also see the indipendence of wires.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/e5ad8e5a5164abb6336d2efa44489db7dc562152.png" width="600">


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/28c7a05c7103672793fed6f248ab1b72a3f78b37.png" width="600">


</details>

### Cobalt Lamp
The Cobalt Lamp will emit the same signal as it receives from input, and it can only read Cobalt Energy.

### Relay

The relay is a block that can be used to do vertical signal transmission. It works as a dust, but only powers upwards and downwards. It's properties are:

* Transmitting only upward and downward.
* Connecting horizontally only to Cobalt Dust.
* It is a glassy block, allowing diode-style logic when you use it with redstone
* You can attach without consequences Cobalt Torches to its faces, since the flow will be inside the block.

<details>
  <summary><b>Spoiler: Cobalt Relay</b></summary>
ON / OFF Showcase for the Relay.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/754094314bfc18d69d8fdd7549bd221bb2908a92.png" width="600">


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/76ffb32e037623e48b9ca25b7340f2fc3414f09d.png" width="600">
</details>

### A new look: Armor trims and material templates

This mod also adds new looks for your armors. It adds Cobalt Ingot and Cobalt Dust items to be used as material template in the smithing table.
Also a new armor trim template has been added: Dust Armor Trim. This new trim pattern can be obtained, as a **temporary feature**, by trading with the wandering trader.

<details>
    <summary><b>Spoiler: Dust Armor Trim & Cobalt Materials </b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f291de41b98ede4a7dbba74600b0a8433076c364.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/19babec8816b4a4abde1a59abe590e59be0a8c5e.png" width="600">

</details>

---
# Resource packs

We have included 3 optional resource packs on this mod. One makes the cobalt rails have a 3D appearance. The second one lets the player see the power level the cobalt dust is emitting on a similar way to how the VanillaTweaks resource pack does. The last one puts the Beacon GUI in dark mode (Like Default Dark Mode does).

<details>
  <summary><b>Spoiler</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/5368099fe8b96ddd4f9e60e7d95446930b68e7b7.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f291748632f28ec0c60dbd42e570d7ed3fe648c0.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/13470ab2b32a65ebd8f2887f320ec6c841627717.png" width="600">

</details>


---
# Credits

Thanks to:

- [Vanilla Tweaks](https://vanillatweaks.net/picker/resource-packs/), for the models for both the 3d rails and the power level resourcepacks.
- [Copper rails](https://modrinth.com/mod/copperrails), as it was used as inspiration for the cobalt rails.


---

# From Authors

You can use this mod freely in your modpacks, but please give credit to the original authors and link back to this page. If you want to make a video about this mod, please also give credit and link back to this page.

If you want to suggest a feature or report a bug, please open an issue on the GitHub repository!

<a href="https://ko-fi.com/cobaltage">
    <img src="https://img.shields.io/badge/Ko--fi-Donate-0775a8?style=for-the-badge&logo=kofi&logoColor=blue" alt="Support us on Ko-fi">
  </a>