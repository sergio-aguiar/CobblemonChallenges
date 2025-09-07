# Cobblemon Challenges

This Fabric 1.21.1 mod allows you to define custom challenges to reward players for progression and create special events!
Compatible with LuckPerms permissions!

---

## **Commands**
- **/challenges**: Open the challenge menu gui where players can select a category such as Daily, Weekly, Monthly, or any custom challenge.
    - Permission: "challenges.commands.challenge"
- **/challenges [type]**: Open the challenge list directly without needing the menu.
    - Permission: "challenges.commands.challenge"
- **/challenges reload**: Reload all challenges and guis! **CURRENTLY DISABLED**
    - Permission: "challenges.commands.admin.reload"
- **/challenges reset [player]**: Reset all challenge progress for a specific player.
    - Permission: "challenges.commands.admin.restart"
- **/challenges resetall**: Reset all challenge progress for all players.
    - Permission: "challenges.commands.admin.restartall"
- **/challenges migrate**: Create a player-data-pools.yml file based on your current player-data.yml file to allow for data migration from the legacy mode to slot-pool mode.
    - Permission: "challenges.commands.admin.migrate"

## **Creating a New Challenge**

## **Configuring the Daily Challenges GUI**

### **Overview**
This section explains how to set up and customize the **Daily Challenges** gui for players. The GUI allows players to view and select challenges and track their progress.

---

### **GUI Configuration**

1. **Define the Window Title**
    - **`window-name`**: The title displayed at the top of the GUI. Use color codes to stylize it (e.g., `&l&eDaily Challenges` for bold, gold text).

2. **Structure the GUI Layout**
    - **`structure`**: Defines the rows and columns in the GUI. Ignores spaces.
        - **Format**: Each row must contain exactly 9 characters (excluding spaces).
        - **Maximum rows**: The layout cannot exceed 6 rows.
        - **Character mapping**:
            - `.`: Represents a blank space filled by challenge list contents.
            - Other characters define itemstacks

      **Example Layout**:
      ```
      "B # # # # # # # #"
      "# . . . . . . . #"
      "# . . . . . . . #"
      "# . . . . . . . #"
      "# . . . . . . . #"
      "< # # # # # # # >"
      ```

3. **Map Characters to Itemstacks**
   Each character in the **structure** corresponds to a specific itemstack. These are defined under **`ingredients`**.

    - **`material`**: Specifies the type of item (supports modded and vanilla items). Use the full name such as `cobblemon:master_ball` for modded items.
    - **`name`**: Custom display name for the item.
    - **`lore`**: Lore in the itemstack.
    - **Optional Behaviors**:
        - **`commands`**: List of commands executed when the item is clicked.
        - **`back-page`** / **`next-page`**: Define navigation buttons for paged GUIs.

      **Common Mappings**:
        - `B` (Back Button):
          ```yaml
          material: BARRIER
          name: "&l&eBack to Main Page"
          lore:
            - "&7Click me to go back to the challenge menu!"
          commands:
            - "execute as {player} run challenges"
          ```
        - `<` (Previous Page Button):
          ```yaml
          material: ARROW
          name: "&l&ePrevious"
          back-page: true
          lore:
            - "&7Click me to go to the previous page."
          ```
        - `>` (Next Page Button):
          ```yaml
          material: ARROW
          name: "&l&eNext"
          next-page: true
          lore:
            - "&7Click me to go to the next page."
          ```

---

### **Challenge List Configuration**

1. **Maximum Active Challenges**
    - **`max-challenges-per-player`**: Limits the number of challenges a player can actively attempt at the same time. Does not include challenges that don't need selection.

2. **Defining Individual Challenges**
   Each challenge is listed under **`challenges`**, with unique identifiers as the key.

    - **`display-name`**: Text shown in challenge completion/expiration/refresh messages. Default to the challenge name/id if missing.
    - **`needs-selection`**: If `true`, the player must manually start the challenge by clicking the itemstack.
    - **`time-limit`**: Duration for the challenge, formatted as `30d24h60m60s` (days, hours, minutes, seconds - as a string).
    - **`repeatable`**: Cooldown time for a challenge, formatted as `30d24h60m60s` (days, hours, minutes, seconds - but not as a string).
    - **`challenge-slot`**: The slot (in slot-pool mode) for the challenge (1, 2, 3, etc. - must be > 0). Challenges sharing a slot value will alternate randomly when reselecting a challenge on cooldown timer being over.
    - **`description`**: Text shown in the GUI and completion messages.

3. **Display Item**
    - **`material`**: The item displayed in the GUI.
    - **`name`**: Custom name for the item.
    - **`lore`**:
        - Use placeholders for additional information:
            - `{progression_status}`: Current challenge progress.
            - `{description}`: The description defined earlier.
            - `{tracking-tag}`: Countdown timer for challenge expiration/cooldown.

4. **Challenge Requirements**
    - **`requirements`**: The requirements needed to complete the challenge. Each task is listed under a unique numeric key to keep them sorted and allow duplicates.
    - Example requirement:
      ```yaml
      1:
        Catch_Pokemon:
          pokename: any
          shiny: false
          type: any
          ball: any
          amount: 10
          time_of_day: any
          is_legendary: false
          is_ultra_beast: false
          is_mythical: false
      ```

5. **Reward Commands**
    - **`rewards`**: Commands executed upon challenge completion.
        - Use `{player}` as a placeholder for the player's username.
        - Example rewards:
          ```yaml
          commands:
            - "give {player} cobblemon:ultra_ball 16"
            - "eco give {player} 2500"
          ```

---

### **Example Full Configuration**

```yaml
gui:
  window-name: "&l&eDaily Challenges"
  structure:
    - "B # # # # # # # #"
    - "# . . . . . . . #"
    - "# . . . . . . . #"
    - "# # . . . . . # #"
    - "< # # # # # # # >"

  ingredients:
    '#':
      material: BLACK_STAINED_GLASS_PANE
      name: " "
    'B':
      material: BARRIER
      name: "&l&eBack to Main Page"
      lore:
        - "&7Click me to go back to the challenge menu!"
      commands:
        - "execute as {player} run challenges"
    '<':
      material: ARROW
      name: "&l&ePrevious"
      back-page: true
      lore:
        - "&7Click me to go to the previous page."
    '>':
      material: ARROW
      next-page: true
      name: "&l&eNext"
      lore:
        - "&7Click me to go to the next page."

challenge-list:
  max-challenges-per-player: 1

  challenges:
    "CatchPokemonExample-Any":
      display-name: "Catch Any Pokémon"
      repeatable: 20h
      time-limit: "24h"
      needs-selection: true
      challenge-slot: 1
      description:
        - "&7Catch 1 Pokémon and receive these rewards!"
        - " "
        - "    &dRewards:"
        - "    &d- &7+100$"
      display-item:
        material: cobblemon:master_ball
        name: "&e&lCatch 1 Pokémon"
        lore:
          - "{progression_status}"
          - " "
          - "{description}"
          - " "
          - "{tracking-tag}"
      requirements:
        1:
          Catch_Pokemon:
            pokename: any
            shiny: false
            type: any
            ball: any
            amount: 1
            time_of_day: any
            is_legendary: false
            is_ultra_beast: false
            is_mythical: false
      rewards:
        commands:
          - "eco give {player} 100"
          
---