# HeroHud

HeroHud is a RuneLite plugin that provides a customizable, integrated Heads-Up Display (HUD) for tracking character statistics. By anchoring visual indicators to the player model, HeroHud offers an alternative to fixed interface elements, allowing players to monitor vital stats while focusing on gameplay.

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
<!-- Placeholder for gameplay screenshot/GIF showing the HUD anchored to the player -->
![HeroHud Gameplay Placeholder](https://via.placeholder.com/800x450?text=Gameplay+Demonstration+Placeholder)

### Configuration Options
<!-- Placeholder for settings panel screenshot -->
![HeroHud Settings Placeholder](https://via.placeholder.com/300x600?text=Settings+Panel+Placeholder)

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

This project is licensed under the MIT License.
