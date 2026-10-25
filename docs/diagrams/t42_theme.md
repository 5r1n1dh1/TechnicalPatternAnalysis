# T-42 — Theme Flow (system-follow + light mode + override toggle)

Light/dark + system follow flow: user picks a global preference (SYSTEM / LIGHT / DARK);
`TPATheme(mode)` resolves `darkTheme = isSystemInDarkTheme()` when SYSTEM and forces it
otherwise. `Glass.glassSurface` uses the light token (`0xCCFFFFFF` fill, `0x33000000` border)
for LIGHT, the dark variant for others; all hard-coded site colors were swapped for tokens.

```mermaid
flowchart TD
    U[User opens app] --> V{SharedPreferences:<br/>theme_mode?}
    V|light|L[lightColorScheme]
    V|dark|D[darkColorScheme]
    V|missing -> SYSTEM
    SYSTEM --> S[isSystemInDarkTheme()]
    S -- dark system --> D_scheme[DarkColorScheme]
    S -- light system  --> L_scheme[LightColorScheme]
    D_scheme --> T1[lightTheme=false]
    L_scheme --> T1
    T1 --> M[MaterialTheme resolves colorScheme]
    T2[USER OVERRIDES via settings/overflow] -.-> V
    T2 --> D_scheme
    T2 --> L_scheme
    M --> G[material3 views use tokens]
    G --> CB[Glass surface: 0xCCFFFFFF fill / 0x33000000 border]
    G --> CF[semantic tokens: onSurface 0xFF0F172A]
    CB -.-> L|LIGHT path verified|CF
```
