#!/usr/bin/env python3
"""Generate per-weapon-type trail textures for SwordSkill.

Output: src/main/resources/assets/swordskill/textures/entity/weapon_trail/<type>.png

Design (ribbon/streak trail, NOT a slash arc):
- U axis (left->right) = across the blade width.  This axis carries the whole
  cross profile: a wide, near-opaque bright band with a soft feathered edge so
  the vertex-color tint (skill color) multiplies cleanly.
- V axis (top->bottom) = along the trail length.  MUST be uniform along V
  because the trail ribbon is split into many small quads each spanning a V
  sub-range; any V variation would look serrated.
- RGB is pure white; only alpha carries the shape so the skill color tint
  shows through via vertex-color multiplication.  The trail is rendered with
  additive blending and three stacked layers (outer glow / body / white core),
  so the texture should be broad and bright rather than a thin needle.

Run from the repo root:  python tools/textures/generate_weapon_trail.py
"""

from pathlib import Path
from math import exp

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[2]
OUT_DIR = REPO_ROOT / "src/main/resources/assets/swordskill/textures/entity/weapon_trail"

# Texture size: wide in U (smooth feathering across the blade),
# short in V (uniform, gets tiled along the streak).
WIDTH = 128   # U = across the blade width
HEIGHT = 16   # V = along the trail (uniform)


def profile(x: float, bands, feather_base: float, feather_tip: float,
            glow_sigma: float, glow_amp: float) -> float:
    """Cross-section alpha at normalized U coordinate x in [0, 1].

    bands: list of (center, half_width_base, half_width_tip) plateau bands.
        The base side (x < center, grip side) uses feather_base and the tip
        side (x >= center, blade tip) uses feather_tip.  The tip side is given
        a tighter feather so the leading edge reads as a crisp boundary
        instead of a soft blur.
    glow: broad soft gaussian underneath the base side to avoid a harsh cutoff.
    """
    alpha = 0.0
    for center, half_base, half_tip in bands:
        if x >= center:
            d = x - center
            half = half_tip
            feather = feather_tip
        else:
            d = center - x
            half = half_base
            feather = feather_base
        if d <= half:
            band_alpha = 1.0
        else:
            band_alpha = exp(-0.5 * ((d - half) / feather) ** 2)
        alpha = max(alpha, band_alpha)
    if glow_amp > 0.0:
        alpha = max(alpha, glow_amp * exp(-0.5 * ((x - 0.45) / glow_sigma) ** 2))
    # Border fade so the outermost silhouette stays soft instead of a hard
    # rectangular cutoff when the quad is stretched.
    edge_fade = min(1.0, x / 0.05, (1.0 - x) / 0.05)
    alpha *= max(0.0, edge_fade)
    return min(1.0, alpha)


# Per-type cross profiles.  (See SkillData.WeaponType for the enum values.)
# bands = [(center, half_base, half_tip)], feather_base, feather_tip,
# (glow_sigma, glow_amp).
# Wide bright plateau; the blade-tip side uses a tight feather so the trail's
# leading boundary is crisp ("きびきび") while the grip side keeps a soft glow.
PROFILES = {
    "one_handed_sword": dict(
        bands=[(0.5, 0.26, 0.30)], feather_base=0.12, feather_tip=0.06,
        glow=(0.30, 0.50)),
    "two_handed_sword": dict(
        bands=[(0.5, 0.30, 0.35)], feather_base=0.14, feather_tip=0.07,
        glow=(0.34, 0.55)),
    "katana": dict(
        bands=[(0.5, 0.24, 0.28)], feather_base=0.11, feather_tip=0.06,
        glow=(0.28, 0.50)),
    "rapier": dict(
        bands=[(0.5, 0.20, 0.24)], feather_base=0.10, feather_tip=0.05,
        glow=(0.25, 0.45)),
    "spear": dict(
        bands=[(0.5, 0.16, 0.20)], feather_base=0.09, feather_tip=0.05,
        glow=(0.22, 0.42)),
    "axe": dict(
        bands=[(0.5, 0.36, 0.42)], feather_base=0.16, feather_tip=0.07,
        glow=(0.38, 0.60)),
    "scythe": dict(
        bands=[(0.5, 0.34, 0.40)], feather_base=0.15, feather_tip=0.07,
        glow=(0.36, 0.58)),
    "mace": dict(
        bands=[(0.5, 0.28, 0.33)], feather_base=0.14, feather_tip=0.07,
        glow=(0.32, 0.55)),
    "claw": dict(
        bands=[(0.5, 0.32, 0.38)], feather_base=0.18, feather_tip=0.08,
        glow=(0.34, 0.55)),
    "dagger": dict(
        bands=[(0.5, 0.18, 0.22)], feather_base=0.09, feather_tip=0.05,
        glow=(0.24, 0.50)),
    "dualsword": dict(
        bands=[(0.30, 0.14, 0.16), (0.70, 0.14, 0.16)],
        feather_base=0.10, feather_tip=0.06, glow=(0.30, 0.45)),
}


def build_texture(params: dict) -> Image.Image:
    img = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    pixels = img.load()
    for u in range(WIDTH):
        x = (u + 0.5) / WIDTH
        alpha = profile(x, params["bands"], params["feather_base"],
                        params["feather_tip"], params["glow"][0], params["glow"][1])
        # Pure white RGB so the additive skill-color tint is not dimmed.
        value = (255, 255, 255, int(round(255 * alpha)))
        for v in range(HEIGHT):
            pixels[u, v] = value
    return img


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    params = dict(PROFILES)
    # Default fallback texture for unknown / NONE weapon types (sword-like).
    params["default"] = dict(
        bands=[(0.5, 0.27, 0.31)], feather_base=0.12, feather_tip=0.06,
        glow=(0.30, 0.50))
    for name in sorted(params):
        img = build_texture(params[name])
        img.save(OUT_DIR / f"{name}.png")
        print(f"wrote {OUT_DIR / (name + '.png')}  {WIDTH}x{HEIGHT}")


if __name__ == "__main__":
    main()
