# GPU Compatibility Fixes for Ghost Launcher

## Problem
Sodium mod causing GPU conflicts on some systems (friend's PC), while other launchers don't have this issue.

## Root Cause
Minecraft with Sodium requires specific JVM arguments for optimal GPU compatibility. Other launchers include compatibility flags by default.

## Solution: Add GPU Compatibility JVM Arguments

### Implementation Location
File: `packages/app-lib/src/launcher/mod.rs`

### Required JVM Arguments for GPU Compatibility

Add these arguments to the default JVM args for better GPU compatibility:

```java
// Graphics compatibility flags
-Dorg.lwjgl.opengl.Window.undecorated=false
-Dorg.lwjgl.opengl.Display.enableHighDPI=true

// Prevent GPU driver crashes with Sodium
-Dorg.lwjgl.util.DebugLoader=false

// Force dedicated GPU on laptops with dual graphics
-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=false

// Mesa/AMD GPU compatibility
-Dorg.lwjgl.system.SharedLibraryExtractPath=./natives

// Fix for Intel iGPU issues with Sodium
-XX:+UnlockExperimentalVMOptions
-XX:+UseG1GC
-XX:G1NewSizePercent=20
-XX:G1ReservePercent=20
-XX:MaxGCPauseMillis=50
-XX:G1HeapRegionSize=32M
```

### Sodium-Specific Launch Args

When Sodium is detected in the mods folder, add:

```java
-Dsodium.checks.enable=false
-Dorg.lwjgl.system.allocator=system
```

## Implementation Steps

### 1. Detect Sodium Installation

Add detection in the launcher to check if Sodium is installed:

```rust
fn has_sodium_mod(instance_path: &Path) -> bool {
    let mods_path = instance_path.join("mods");
    if let Ok(entries) = std::fs::read_dir(mods_path) {
        for entry in entries.flatten() {
            let file_name = entry.file_name();
            let name = file_name.to_string_lossy().to_lowercase();
            if name.contains("sodium") && name.ends_with(".jar") {
                return true;
            }
        }
    }
    false
}
```

### 2. Add GPU Compatibility Args to Default Profile

Modify the default Java arguments to include GPU compatibility flags automatically.

### 3. Detect GPU Type and Apply Specific Fixes

```rust
fn detect_gpu_vendor() -> Option<String> {
    // Use WGPU or system info to detect GPU
    // Return "nvidia", "amd", "intel", or "unknown"
}

fn get_gpu_specific_args(vendor: &str) -> Vec<String> {
    match vendor {
        "nvidia" => vec![
            "-XX:+UseNUMA".to_string(),
        ],
        "amd" => vec![
            "-Dorg.lwjgl.system.allocator=system".to_string(),
        ],
        "intel" => vec![
            "-Dsodium.checks.enable=false".to_string(),
            "-XX:+UnlockExperimentalVMOptions".to_string(),
        ],
        _ => vec![],
    }
}
```

## Quick Fix for Users

### Manual Workaround

Users can add these arguments manually in instance settings → Java Arguments:

**For Intel GPUs:**
```
-Dsodium.checks.enable=false -XX:+UnlockExperimentalVMOptions
```

**For AMD GPUs:**
```
-Dorg.lwjgl.system.allocator=system
```

**For NVIDIA GPUs:**
```
-XX:+UseNUMA
```

### Update LWJGL Natives

The launcher should download and use the latest LWJGL natives for better GPU compatibility:
- LWJGL 3.3.3+ has better AMD GPU support
- LWJGL 3.3.2+ has fixes for Intel iGPU crashes

## Testing

Test with:
1. Friend's PC with GPU conflict
2. Various GPU vendors (NVIDIA, AMD, Intel)
3. Laptop with dual graphics (iGPU + dGPU)
4. Sodium + Iris combination
5. Different Minecraft versions (1.16+, 1.18+, 1.20+)

## Additional Resources

- Sodium GitHub Issues: https://github.com/CaffeineMC/sodium-fabric/issues
- LWJGL GPU Compatibility: https://www.lwjgl.org/customize
- JVM GPU Flags: https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/envvars002.html
