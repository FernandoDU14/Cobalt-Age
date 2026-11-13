![Cobalt Age Title](https://cdn.modrinth.com/data/pmieAiUD/images/9a9bcb0d8e146267137e216dc3a5d71a8b141876.png)

<div align="center">

[![Discord](https://img.shields.io/badge/Discord-Join%20Server-7289DA?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/txWQN8CtEV)
[![GitHub Repo](https://img.shields.io/badge/GitHub-Cobalt--Age-blue?style=for-the-badge&logo=github)](https://github.com/FernandoDU14/Cobalt-Age)

</div>

#
This mod adds a new mineral to the overworld, the **cobalt**. It can be found between y=-64 and y=32, with it being more common the nearer you are to y=-16. It requires iron tools or superior to be mined, dropping **raw cobalt** which can then be smelted to obtain **cobalt ingots**.

The ingots can be used to either create a new type of rail, used as a trim material, or to make cobalt nuggets, which combined with redstone dust can create **cobalt dust**, and the dust can be used to craft a new set of redstone components made of cobalt.

This new cobalt components are waterloggeable, meaning you are now able to do redstone circuits under water or prevent them being broke if water touches them.

<details>
  <summary><b>Cobalt Age showcase:</b></summary>

Complete Cobalt Age Set:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/11f36771bfcbca8b02469aa3161c6f76b1dc870b.png" width="600">

Cobalt Block is compatible with the Beacon:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f286a6a10b49206eb21d6fa36380037aab3ec387.png" width="600">

The new beacon GUI, the cobalt ingot has been added in the GUI.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f0f20e6f3c316db775db8575b887e1e39f6fc661.png" width="600">

Water does not break the blocks:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/9fbce9537f742639174d6f05761735af7569c610.png" width="600">

A closer look into the retracted state:

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c11253b7c3f55c2d53ce56a89d1909f16db8128d.gif" width="600">

</details>

---

### High Performance Signal Engine

Cobalt wires use a **modern optimized signal propagation system** capable of reducing computation costs compared to vanilla Redstone.

Cobalt components preserve vanilla update order and behaviour, including mechanics that rely on update timing and ordering, such as dupe machines.

<details>
  <summary><b>Performance Tests</b></summary>
Scatter Test in a 100 x 100 wire linked web with 20 observer clocks. Cobalt dust does not cause not even a minimum server lag or client FPS drops (10 ms over 50 ms), while redstone dust cause a huge lag and a huge delay of 2.8 seconds from a tick to another one (130+ ms over 50ms). You can see the delay in the redstone update signals in the shown videos.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/bf714ef2ceeb743557d05f8af0736569816c275c.gif" width="600">
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/684fb57e4a285c13bc14b1d532861ceab9c6c08e.gif" width="600">
</details>

--- 

### Independent Energy Systems

Cobalt components are completely independent from redstone components, meaning they wont interact between each other in any way.

<details>
  <summary><b>Example of independence</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c87c6887d0747a4aaa11492dfad20d88a5f03dc3.gif" width="600">

</details>

<details>
  <summary><b>Parallel Wires</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/2ddbfd49a3af63083fbf0f61a4956351725d607f.png" width="600">

</details>

<details>
  <summary><b>More independece examples</b></summary>


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/14ff842ece2cc859da4cfb8707bd1c551961c4aa.png" width="600">

</details>


<details>
    <summary><b>Even more examples</b></summary>

Showcase of the Glass Diode for Cobalt Wires. The logic has been inherited by Redstone Wires.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/efab130c1c7e28c4aa6512de7773c344a870f1ac.png" width="600">

ON / OFF Showncase images for mixed blocks underwater and surface.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/c21df3c9a33d592c43a742aca0e6347d3d95cee1.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/336f536ad29d8e5541418083ab3373c8dcf5c433.png" width="600">

</details>

--- 

### Cobalt Rail

**Supports experimental minecart improvements**

The cobalt rails are a new type of rail similar to the powered rail that can reach higher speeds (by default a speed of 24 blocks per second).

To craft this type of rails you will need 6 cobalt ingots, one cobalt dust and one breeze rod (yes, kinda expensive, but with that speed is understandable). The crafting pattern is similar to the powered rail one.

There's also two new custom gamerules that lets you change the speed of both powered and cobalt rails. The gamerule syntax is `cobaltage:max_minecart_speed_<type> <value>` with `type` being the type of rail, and `value` the speed of the rail, which is a value between 1 and 1000 both included.

---

### Converter
The converter is a new redstone component that acts like a bridge between cobalt and redstone. It receives power from one type and outputs the same amount of power but from the other type. For example: if the converter receives a power level of 15 of cobalt, it will output a power level 15 of redstone.

To craft one you need one redstone torch, one quartz, one cobalt torch and three stone blocks.

<details>
  <summary><b>Converter showcase</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/142887088c12929925af6f69e5f4af197c9596fd.png" width="600">


Converter ON / OFF Lever behavior. You can also see the indipendence of wires.
<img src="https://cdn.modrinth.com/data/pmieAiUD/images/e5ad8e5a5164abb6336d2efa44489db7dc562152.png" width="600">


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/28c7a05c7103672793fed6f248ab1b72a3f78b37.png" width="600">


</details>

---

### Cobalt Lamp

The cobalt lamp is a new light block that will emit an amount of light based on the power received, accepting only cobalt as the energy source.

It can be crafted with two cobalt blocks, one quartz, one glowstone, and one cobalt dust.

---

### Relay

The relay is a block that can be used to do vertical signal transmission. It works as a dust, powering only upwards and downwards.

It can be crafted using 6 glass panes, two cobalt ingots and one cobalt dust. It will also require **silk touch** to be mined.

<details>
  <summary><b>Cobalt Relay showcase</b></summary>
ON / OFF Showcase for the Relay.

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/754094314bfc18d69d8fdd7549bd221bb2908a92.png" width="600">


<img src="https://cdn.modrinth.com/data/pmieAiUD/images/76ffb32e037623e48b9ca25b7340f2fc3414f09d.png" width="600">
</details>

---

### A new look

Both cobalt ingot and dust can be used as **trim materials**, giving your armor two new blue tones.

Also a new armor trim template has been added: **Dust**. It can be obtained by trading with a wandering trader (though this is something temporal).

<details>
    <summary><b>Dust Armor Trim & Cobalt Materials </b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f291de41b98ede4a7dbba74600b0a8433076c364.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/19babec8816b4a4abde1a59abe590e59be0a8c5e.png" width="600">

</details>

---
### Resource packs

Theres's 3 optional resource packs on this mod. One makes the cobalt rails have a 3D appearance. The second one lets the player see the power level the cobalt dust is emitting on a similar way to how the VanillaTweaks resource pack does. The last one puts the Beacon GUI in dark mode (Like Default Dark Mode does).

<details>
  <summary><b>Spoiler</b></summary>

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/5368099fe8b96ddd4f9e60e7d95446930b68e7b7.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/f291748632f28ec0c60dbd42e570d7ed3fe648c0.png" width="600">

<img src="https://cdn.modrinth.com/data/pmieAiUD/images/13470ab2b32a65ebd8f2887f320ec6c841627717.png" width="600">

</details>


---
### Credits

Thanks to:

- [Vanilla Tweaks](https://vanillatweaks.net/picker/resource-packs/), for the models for both the 3d rails and the power level resourcepacks.
- [Copper rails](https://modrinth.com/mod/copperrails), as it was used as inspiration for the cobalt rails.


---
### From Author

You can use this mod freely in your modpacks, but please give credit to the original authors and link back to this page. If you want to make a video about this mod, please also give credit and link back to this page.

If you want to suggest a feature or report a bug, please open an issue on the GitHub repository!


