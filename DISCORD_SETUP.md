# Discord Rich Presence Setup for Ghost Launcher

## Problem
Discord RPC not showing "Playing Ghost Launcher" on Windows, Mac, or Linux.

## Solution

### 1. Create Discord Application

1. Go to https://discord.com/developers/applications
2. Click "New Application"
3. Name it "Ghost Launcher"
4. Copy the **Application ID**

### 2. Upload Ghost Icon as Rich Presence Asset

1. In your Discord application, go to "Rich Presence" → "Art Assets"
2. Upload the ghost icon as an asset named **"ghost"** (must match the code)
3. Upload a larger version as **"ghost_large"** if needed

### 3. Set Environment Variable

Add the Application ID to your build environment:

**Windows (PowerShell):**
```powershell
$env:GHOST_DISCORD_CLIENT_ID="YOUR_APP_ID_HERE"
```

**Mac/Linux (Bash):**
```bash
export GHOST_DISCORD_CLIENT_ID="YOUR_APP_ID_HERE"
```

**Or add to `.env` file in the project root:**
```
GHOST_DISCORD_CLIENT_ID=YOUR_APP_ID_HERE
```

### 4. Rebuild the Launcher

After setting the environment variable, rebuild the launcher so it uses the correct Discord App ID.

## Current Implementation

The Discord RPC is configured in:
- `packages/app-lib/src/state/discord.rs` - Main Discord integration
- Asset name: `"ghost"` 
- Large text: `"Ghost Launcher"`
- Activity states:
  - "Idling..." when in launcher
  - "Playing {instance_name}" when running Minecraft

## Testing

1. Make sure Discord is running
2. Launch Ghost Launcher
3. Check Discord profile - should show "Playing Ghost Launcher"
4. Launch a Minecraft instance - should show "Playing {profile_name}"

## Mac Icon Issue

The Mac app icon needs to be properly set in the `.icns` file. Check:
- `apps/app/icons/icon.icns` - should contain the ghost icon
- If it shows the default Tauri icon, rebuild the `.icns` file with the ghost logo

### Rebuild Mac Icon:
```bash
# Install iconutil if needed (comes with Xcode)
iconutil -c icns ghost_icon.iconset -o icon.icns
```

The iconset should contain ghost images at:
- icon_16x16.png
- icon_32x32.png
- icon_128x128.png
- icon_256x256.png
- icon_512x512.png
- icon_512x512@2x.png (1024x1024)
