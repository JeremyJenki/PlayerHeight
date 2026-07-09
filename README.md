# PlayerHeight
 
Player scale and attribute management plugin for Minecraft (Paper).

## What does it do?

In vanilla, changing the scale attribute alone does not effect player move speed, jump distance, health or any other traits, leading to unrealistic movement and unbalanced gameplay.

This plugin fixes that by making each relevant attribute scale proportionally to the player's base scale.

Smaller players will accurately walk slower, have weaker jumps and deal less attack damage, while larger players will be inversely affected.

## Size Potions

PlayerHeight comes with a custom potion module `/ph give`. Size potions can grow or shrink players easily while integrating nicely with survival worlds.

The plugin does not currently make these potions craftable or lootable. Admins will need to set up that side of things themselves.

Alternatively, you can give players the `playerheight.self` permission to let them change it on the fly.

## Requirements

- Paper 1.21.4+
- PlaceholderAPI (optional)

### Commands

- `/ph setscale <value>`  `alias: /setscale`
- `/ph scale [player]`  `alias: /scale`
- `/ph scale <player> <value|+delta|-delta>`
- `/ph give <player> <delta> [amount]`
- `/ph reload`

### Permissions

`playerheight.admin`
`playerheight.check`
`playerheight.self`

### Placeholders

- `%playerheight_scale%`
- `%playerheight_cm%`
- `%playerheight_imperial%`

## Installing

Place the PlayerHeight .jar in `/plugins`.

## Uninstalling

1. Remove the PlayerHeight .jar and plugin folder
2. (Optional) Run AttributeCleanup.jar to reset all leftover player attributes (instructions provided)

## Config
```
# PlayerHeight Configuration
# -------------------------

language: en_US

# Set the minimum and maximum player scale.
# Anything beyond these bounds are clamped.
scale:
  min: 0.1
  max: 2.0
  precision: 2  # decimal places -- 2 = 0.01 steps, 3 = 0.001 steps


# Per-world scale override.
# The player's height will be clamped to the min/max values while within these worlds.
worlds:
  world_example:
    min_scale: 0.5
    max_scale: 1.3

# Feedback shown to the player when their scale changes.
feedback:
  actionbar: true
  particles: true
  # Colors to use for potions and particle effects.
  shrink_color: "76E3FF"
  grow_color: "FFA262"
  neutral_color: "C8C8C8"
  # height_unit: cm or imperial (feet and inches)
  height_unit: imperial

# Stack sizes for potions by tier. Only affects newly created potions.
# Tier is determined by delta size: I = 0.01-0.09, II = 0.10-0.49, III = 0.50-0.99, IV = 1.0+
potion_stack_sizes:
  tier_1: 16
  tier_2: 4
  tier_3: 1
  tier_4: 1

# Attributes are linearly interpolated between keyframes; minimum two points required.
# Remove an attribute block entirely to stop managing it.
attributes:

  # Walk speed: Vanilla = 0.20.
  # Managed via Bukkit API to bypass FOV change. Not an attribute.
  walk_speed:
    keyframes:
      0.1: 0.14
      0.5: 0.18
      0.7: 0.20
      1.0: 0.20
      1.3: 0.26
      2.0: 0.32

  # Max health: Vanilla = 20.0.
  max_health:
    keyframes:
      0.1: 2.0
      0.5: 10.0
      0.7: 20.0
      1.0: 20.0
      1.5: 20.0
      2.0: 20.0

  # Attack damage: Vanilla = 2.0.
  attack_damage:
    keyframes:
      0.1: 0.5
      0.7: 2.0
      1.0: 2.0
      1.5: 4.0
      2.0: 7.0

  # Knockback resistance: Vanilla = 0.0.
  knockback_resistance:
    keyframes:
      0.1: 0.0
      1.0: 0.0
      1.1: 0.0
      2.0: 0.5

  # Block break speed: Vanilla = 1.0.
  block_break_speed:
    keyframes:
      0.1: 0.1
      0.7: 1.0
      1.0: 1.0
      2.0: 1.0

  # Jump strength: Vanilla = 0.41999998688697815.
  jump_strength:
    keyframes:
      0.1: 0.30
      0.3: 0.38
      0.5: 0.41999998688697815
      1.0: 0.41999998688697815
      1.5: 0.50
      2.0: 0.60

  # Step height: Vanilla = 0.6.
  step_height:
    keyframes:
      0.1: 0.2
      0.5: 0.6
      1.0: 0.6
      1.5: 0.8
      2.0: 1.1

  # Safe fall distance: Vanilla = 3.0.
  safe_fall_distance:
    keyframes:
      0.1: 12.0
      0.5: 3.0
      0.7: 3.0
      1.0: 3.0
      1.5: 4.0
      2.0: 5.0

  # Entity interaction range: Vanilla = 3.0.
  entity_interaction_range:
    keyframes:
      0.1: 2.0
      0.5: 3.0
      1.0: 3.0
      1.5: 3.0
      2.0: 3.0

  # Block interaction range: Vanilla = 4.5.
  block_interaction_range:
    keyframes:
      0.1: 2.0
      0.5: 4.5
      1.0: 4.5
      1.5: 4.5
      2.0: 4.5

  # Attack knockback: Vanilla = 0.0.
  attack_knockback:
    keyframes:
      0.1: 0.0
      1.0: 0.0
      1.1: 0.0
      2.0: 1.0
```
