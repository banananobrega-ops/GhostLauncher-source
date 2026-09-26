# Ghost Client - Enhanced Features

## Overview

The Ghost Client mod has been significantly enhanced with new features for better social interaction, customization, and user experience.

## New Features

### 1. Enhanced Mod Menu (OneConfig Style)

**Access:** Press **Right Shift** to open/close

The mod menu has been completely redesigned with a modern, customizable interface inspired by OneConfig:

- **Beautiful UI Design**
  - Smooth rounded corners and shadows
  - Modern color palette with accent colors
  - Hover effects and visual feedback
  - Responsive layout that adapts to screen size

- **Sidebar Navigation**
  - Ghost Client branding with version badge
  - Easy tab switching between sections
  - Online friends counter
  - Player information footer

### 2. Enhanced Friends System

**Location:** Friends tab in mod menu

Real-time friends management with improved UI:

- **Friend Requests**
  - Send friend requests by username
  - Accept/decline incoming requests with visual buttons
  - Pending outgoing requests tracking

- **Friends List**
  - Online status indicators (green/gray dots)
  - Sorted by online status
  - Click to open chat

- **Real-time Chat**
  - Message bubbles (blue for you, gray for friends)
  - Automatic message wrapping
  - Optimistic UI updates
  - Poll-based sync (~2 seconds)

### 3. Friends HUD Overlay

**Toggle:** Enabled in Mods tab

In-game overlay showing:
- Up to 5 online friends
- Displayed in top-right corner
- Compact card design
- Non-intrusive during gameplay

### 4. Chat Notifications

**Toggle:** Enabled in Mods tab

Real-time message notifications:
- Pop-up notifications in bottom-right corner
- Shows sender name, message preview
- Avatar placeholder with sender's initial
- Click to open chat
- Auto-dismiss after 5 seconds
- Fade in/out animations

### 5. Capes System

**Features:**
- Custom cape uploads through launcher
- Capes visible to all Ghost Client users
- Supports PNG format (64x32 or multiples)
- Automatic texture loading and caching
- OptiFine-style cape rendering

### 6. Ghost Badge System

**Features:**
- Shows Ghost Client icon next to player names
- Visible above head and in Tab list
- Only Ghost Client users see badges
- Custom font-based rendering
- Works in multiplayer servers

### 7. Mod Toggles

**Location:** Mods tab in menu

Customize your experience:
- **Capes** - Show custom capes in-game
- **Badges** - Display Ghost icon on players
- **Friends HUD** - Show online friends overlay
- **Chat Popup** - Real-time message notifications

All settings are saved in `config/ghostclient.json`

## Technical Details

### Enhanced Menu Components

- `GhostMenuScreenEnhanced.java` - Main menu with improved UI
- `RoundedGui.java` - Utility for rounded rectangles
- `FriendsHudOverlay.java` - In-game overlay system

### Rendering System

- Custom HUD overlay rendering
- Smooth animations and transitions
- Performance-optimized drawing
- Non-blocking network operations

### Network Integration

- Polls backend API every ~2 seconds for updates
- Presence system (online/offline detection)
- Message delivery with optimistic UI
- Automatic reconnection handling

## Configuration

Settings are stored in `config/ghostclient.json`:

```json
{
  "showCapes": true,
  "showBadges": true,
  "showFriendsOnHud": true,
  "chatPopupEnabled": true
}
```

## Backend Requirements

The mod requires these API endpoints (see `LOVABLE_PROMPT_FRIENDS.md`):

- `POST /api/public/gc/api/presence/ping` - Mark player as online
- `POST /api/public/gc/api/friends/request` - Send friend request
- `POST /api/public/gc/api/friends/accept` - Accept friend request
- `POST /api/public/gc/api/friends/decline` - Decline friend request
- `POST /api/public/gc/api/friends/remove` - Remove friend
- `GET /api/public/gc/api/friends/list` - Get friends list
- `POST /api/public/gc/api/chat/send` - Send message
- `GET /api/public/gc/api/chat/poll` - Poll for new messages

## Usage

### Opening the Menu

1. Press **Right Shift** at any time (in-game or in menus)
2. Menu toggles open/close
3. Click anywhere or press ESC to close

### Adding Friends

1. Open menu with Right Shift
2. Go to "Friends" tab
3. Enter friend's username in text field
4. Click "Add Friend"
5. Wait for them to accept

### Chatting

1. Open menu with Right Shift
2. Go to "Friends" tab
3. Click on a friend in the list
4. Type message and press Enter or click "Send"

### Managing Settings

1. Open menu with Right Shift
2. Go to "Mods" tab
3. Click on any card to toggle that feature
4. Settings save automatically

## Visual Design

### Color Palette

- **Background:** Dark gray/blue tones
- **Accent:** Blue (#6B8AFF)
- **Online:** Green (#5AE07B)
- **Offline:** Gray (#5B6172)
- **Success:** Green (#4CAF50)
- **Error:** Red (#F44336)

### UI Elements

- Rounded corners (6-12px radius)
- Subtle shadows and glows
- Smooth hover effects
- Card-based layout
- Consistent spacing and padding

## Performance

- Lightweight rendering (minimal GPU usage)
- Efficient network polling
- Cached textures and data
- Async operations for all network calls
- No blocking on main thread

## Compatibility

- **Minecraft:** 1.21.11
- **Loader:** Fabric (no Fabric API required)
- **Java:** 21+
- **Multiplayer:** Compatible with vanilla servers

## Known Limitations

- No websocket support (uses polling instead)
- Maximum 500 characters per message
- Friend requests require exact username match
- Capes only visible to Ghost Client users

## Future Enhancements

Potential improvements for future versions:
- WebSocket support for real-time updates
- Group chats
- Voice chat integration
- Custom emoji/reactions
- Friend nicknames
- Status messages
- More customization options
