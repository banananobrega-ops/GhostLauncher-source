# Mac Icon Setup Instructions

The Mac icon (`icon.icns`) currently shows the default Axolotl/Tauri icon instead of the Ghost logo. Follow these steps to create a proper Ghost icon for macOS.

## Requirements

- macOS system (required for iconutil)
- Ghost logo PNG files at multiple resolutions
- iconutil command-line tool (comes with Xcode command line tools)

## Steps to Create Ghost Icon

### 1. Prepare Ghost Logo Images

Create PNG files of your Ghost logo at these exact sizes and save them in a temporary folder (e.g., `ghost.iconset/`):

```
ghost.iconset/
├── icon_16x16.png      (16x16 pixels)
├── icon_16x16@2x.png   (32x32 pixels)
├── icon_32x32.png      (32x32 pixels)
├── icon_32x32@2x.png   (64x64 pixels)
├── icon_128x128.png    (128x128 pixels)
├── icon_128x128@2x.png (256x256 pixels)
├── icon_256x256.png    (256x256 pixels)
├── icon_256x256@2x.png (512x512 pixels)
├── icon_512x512.png    (512x512 pixels)
└── icon_512x512@2x.png (1024x1024 pixels)
```

**Note:** The folder MUST be named with `.iconset` extension.

### 2. Convert to .icns Format

Run this command in Terminal:

```bash
iconutil -c icns ghost.iconset -o icon.icns
```

This creates `icon.icns` file from your iconset folder.

### 3. Replace the Icon

Copy the generated `icon.icns` to replace the existing one:

```bash
cp icon.icns apps/app/icons/icon.icns
```

### 4. Rebuild the App

After replacing the icon, rebuild your Tauri app:

```bash
cd apps/app
npm run tauri build
```

Or for development:

```bash
npm run tauri dev
```

## Alternative: Using Existing PNG

If you already have a high-resolution Ghost logo PNG (preferably 1024x1024):

1. Use online tools like [iConvert Icons](https://iconverticons.com/online/) or [CloudConvert](https://cloudconvert.com/png-to-icns) to convert PNG to ICNS
2. Download the generated `.icns` file
3. Replace `apps/app/icons/icon.icns`
4. Rebuild the app

## Verify the Icon

After rebuilding:

1. The Ghost Launcher app icon should show the Ghost logo in Finder
2. The Dock icon should show the Ghost logo when the app is running
3. The app should show the Ghost logo in Spotlight search

## Current Icon Location

- Mac icon: `apps/app/icons/icon.icns`
- Windows icon: `apps/app/icons/icon.ico`
- Linux icons: `apps/app/icons/*.png`

## Notes

- The `.icns` format is required for macOS applications
- The iconset folder must contain all the listed resolutions for best quality
- Make sure your Ghost logo has a transparent background for best results
- The icon must be replaced BEFORE building the app - changing it after won't update the built application
