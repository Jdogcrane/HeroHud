# HeroHud

HeroHud is a RuneLite plugin that provides a customizable, integrated ingame indicators for tracking character vitals. 
By anchoring visual indicators to the player model, HeroHud offers an alternative to fixed overlay interface elements, allowing players to monitor vital in a subtle and immersive way while focusing on gameplay.

---

## Features

### Stamina Tracking
- **Effective Energy Calculation**: Optionally factors in character weight and Agility levels to display "effective" stamina layers.
- **Activity Indicators**: Integrated pulsers visualize active energy depletion while running or recovery while resting.
- **Selective Visibility**: Context-aware display options allow the HUD to appear only when running or during specific energy thresholds.

### Health and Prayer Modules
- Dedicated Hitpoints and Prayer tracking with independent configuration.
- Support for multiple visualization styles including Wheels, Bars, Pie Charts, and Liquid Orbs.

### Dynamic Visuals
- **Hue-Shifting**: Optional dynamic coloring that transitions from user-defined colors to yellow, orange, and red as stats deplete, while maintaining consistent brightness.
- **Dynamic Scaling**: Elements can be configured to scale proportionally with the game camera zoom levels.
- **Positioning**: Multiple anchor presets (Shoulders, Top) with support for pixel-perfect custom offsets.

---

## Visual Overviews

### Feature in Action

![HeroHud Gameplay Placeholder](https://imgur.com/i4Gr5gE.gif)

### Contextually Subtle UI
![HeroHud Gameplay Placeholder](https://imgur.com/y3akXkk.gif)

### Configuration Options

![HeroHud Settings](https://i.imgur.com/UfbfaSZ.png)

### Customize your style (WIP)

![HeroHud Custom](https://imgur.com/NY2txc2.png)

---

## Configuration

Settings are categorized by statistic (**Stamina**, **Health**, **Prayer**) to allow for granular control over the interface:

- **Visibility Modes**: Global overrides for `Always Show`, `Show in Combat`, or `Hide in Combat`.
- **Thresholds & Timers**: Define specific percentage ranges or inactivity durations for HUD visibility.
- **Style Customization**: Adjust fill/background colors, opacity, border thickness, and element sizing.

---

## Installation

1. Open the **RuneLite** configuration panel.
2. Search for **HeroHud** in the Plugin Hub.
3. Select **Install**.

---

## License

/*
* Copyright (c) 2026, Joshua Crane <Joshua.Crane.me@gmail.com>
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* 1. Redistributions of source code must retain the above copyright notice, this
*    list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
* ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
* ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
  */
