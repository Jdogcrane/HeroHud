# HeroHud

HeroHud is a minimalistic in-game indicator for stamina and prayer.

---

## Features

### Stamina Tracking
- **Effective Energy Calculation**: Factors in character weight and Agility levels to display "effective" stamina with extra layers/wheels.
- **Activity Indicators**: Integrated pulsers visualize active energy depletion while running or recovery while resting.
- **Selective Visibility**: Context-aware display options allow the HUD to appear during specific contexts or during specific energy thresholds.

### Health and Prayer Modules
- Dedicated Hitpoints and Prayer tracking with independent configuration.
- Support for multiple visualization styles, including Wheels, Bars, Pie Charts, and Liquid Orbs.

### Dynamic Visuals
- **Hue-Shifting**: Optional dynamic coloring that transitions from user-defined colors to yellow, orange, and red as stats deplete, while maintaining consistent brightness.
- **Dynamic Scaling**: Elements can be configured to scale proportionally with the game camera zoom levels.
- **Positioning**: Multiple anchor presets (Shoulders, Top) with support for pixel-perfect custom offsets.

---

## Visual Overviews


### Feature in Action
![HeroHud](https://github.com/user-attachments/assets/1cea28fa-f66e-481d-8d6a-70cfe9693443)


### Contextually Subtle UI
![java_hBs4mU07Bw](https://github.com/user-attachments/assets/f898c640-f89e-4575-aa5e-751179fd762e)

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
BSD 2-Clause License

