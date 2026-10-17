import unittest
from dataclasses import replace
import json
from pathlib import Path
from tempfile import TemporaryDirectory

from convert_java_skills import (
    extract_sound_events,
    extract_projectile_events,
    make_definition,
    parse_registration,
    read_rush_start_ticks,
    split_java_arguments,
    strip_java_comments,
)


class ConvertJavaSkillsTests(unittest.TestCase):
    def test_split_java_arguments_respects_nested_calls_and_literals(self):
        arguments = split_java_arguments(
            'getNextSkillId(), "slash, one", List.of(ONE_HANDED_SWORD, KATANA), false, 3'
        )
        self.assertEqual(
            [
                "getNextSkillId()",
                '"slash, one"',
                "List.of(ONE_HANDED_SWORD, KATANA)",
                "false",
                "3",
            ],
            arguments,
        )

    def test_strip_java_comments_preserves_comment_markers_in_strings(self):
        source = 'String sound = "mod:hit//fast"; // remove this\n/* and this */ int tick = 3;'
        self.assertEqual('String sound = "mod:hit//fast"; \n int tick = 3;', strip_java_comments(source))

    def test_extract_sound_events_maps_legacy_sound_helpers_to_ticks(self):
        source = """
            if (FinalTick == 8) {
                SimpleSkillSound(level, player.position());
            } else if (FinalTick == 12) {
                StrongSkillSound(level, player.position());
                GodSkillSound(level, player.position());
            }
        """
        self.assertEqual(
            [
                {"tick": 8, "sound": "minecraft:entity.player.attack.sweep", "volume": 1.0, "pitch": 1.47},
                {"tick": 12, "sound": "minecraft:entity.player.attack.strong", "volume": 1.0, "pitch": 0.6},
                {"tick": 12, "sound": "minecraft:item.totem.use", "volume": 1.0, "pitch": 0.6},
            ],
            extract_sound_events(source),
        )

    def test_extract_zero_movement_attack_effect_as_hitbox(self):
        source = """
            public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
                if (FinalTick == 4) {
                    Vec3 lookVec = player.getLookAngle();
                    Vec3 spawnPos = player.position().add(0, player.getEyeHeight() * 0.75, 0)
                            .add(lookVec.scale(2.0));
                    double damage = BaseDamage(player) * 2.5f;
                    double knockbackForce = BaseKnowBack(player) * 0.5f;
                    Vector3f size = new Vector3f(6.2f, 3f, 2.4f);
                    int duration = 12;
                    Vec3 rotation = new Vec3(-6, 0, 135);
                    String texture = NomalSkillTexture();
                    spawnAttackEffect(level, spawnPos, rotation, size, player, damage,
                            knockbackForce, duration, texture, Vec3.ZERO);
                }
            }
        """
        hitboxes = []
        events, warnings = extract_projectile_events(source, 8, hitboxes)
        self.assertEqual([], warnings)
        self.assertEqual([], events)
        self.assertEqual(
            [{
                "tick": 4,
                "hitbox_enabled": True,
                "offset": [0.0, 1.215, 2.0],
                "size": [6.2, 3.0, 2.4],
                "damage_multiplier": 2.5,
                "damage_type": "BASE",
                "knockback": 0.5,
                "knockback_multiplier": 0.5,
                "effects": [],
                "sound": "",
            }],
            hitboxes,
        )

    def test_extract_zero_movement_attack_effect_in_helper_as_hitbox(self):
        source = """
            public void execute(Level level, ServerPlayer player, int FinalTick, int SkillID) {
                if (FinalTick == 6) {
                    performSlash(level, player, 0, 0.1f, 1.8, AxeRedSkillTexture());
                }
            }
            private void performSlash(Level level, ServerPlayer player, int slashIndex,
                    float knockback, double Damage, String Texture) {
                Vec3 spawnPos = player.position().add(0, 1.2, 0).add(player.getLookAngle().scale(2));
                double damage = BaseDamage(player) * Damage;
                double knockbackForce = BaseKnowBack(player) * knockback;
                Vector3f size = slashIndex == 3
                        ? new Vector3f(0.75f, 0.75f, 4f) : new Vector3f(6.4f, 3f, 6.4f);
                int duration = 12;
                Vec3 rotation = new Vec3(0, 0, 0);
                spawnAttackEffect(level, spawnPos, rotation, size, player, damage,
                        knockbackForce, duration, Texture, Vec3.ZERO);
            }
        """
        hitboxes = []
        events, warnings = extract_projectile_events(source, 32, hitboxes)
        self.assertEqual([], warnings)
        self.assertEqual([], events)
        self.assertEqual(1, len(hitboxes))
        self.assertEqual(6, hitboxes[0]["tick"])
        self.assertEqual([6.4, 3.0, 6.4], hitboxes[0]["size"])
        self.assertEqual(1.8, hitboxes[0]["damage_multiplier"])
        self.assertEqual(0.1, hitboxes[0]["knockback"])

    def test_extract_rush_damage_formula_and_random_rotation_fallback(self):
        project_root = Path(__file__).resolve().parents[2]
        source = strip_java_comments(
            (project_root / "src/main/java/com/zoma1101/swordskill/swordskills/skill/katana/GenGetu.java")
            .read_text(encoding="utf-8")
        )
        events, warnings = extract_projectile_events(source, 20)
        self.assertTrue(any("ランダム分岐" in warning for warning in warnings))
        self.assertEqual(1, len(events))
        self.assertEqual(8, events[0]["tick"])
        self.assertEqual("RUSH", events[0]["damage_type"])
        self.assertEqual(3.5, events[0]["damage_multiplier"])
        self.assertEqual(0, events[0]["travel_distance"])
        self.assertEqual([0.0, 0.81, 2.0], events[0]["offset"])
        self.assertEqual([0.0, 0.0, -35.0], events[0]["rotation"])
        self.assertEqual([7.2, 2.0, 2.4], events[0]["size"])
        self.assertEqual("skill_particle_sword/red", events[0]["particle"])

    def test_parse_registration_maps_registry_fields(self):
        registration = parse_registration(
            [
                "getNextSkillId()",
                '"slash_one"',
                "80",
                "5.0",
                "SkillData.SkillType.SIMPLE",
                "SlashOne.class",
                "List.of(ONE_HANDED_SWORD, KATANA)",
                "false",
                "6",
                "20",
            ],
            ["ONE_HANDED_SWORD", "KATANA"],
            20,
            42,
        )
        self.assertEqual(42, registration.legacy_id)
        self.assertEqual("slash_one", registration.name)
        self.assertEqual(80, registration.cooldown)
        self.assertEqual(5.0, registration.sp_cost)
        self.assertEqual(["ONE_HANDED_SWORD", "KATANA"], registration.weapons)
        self.assertEqual(6, registration.final_tick)
        definition = make_definition(registration, 6)
        self.assertEqual(42, definition["id"])
        self.assertTrue(definition["replace_builtin"])
        self.assertTrue(definition["legacy_fallback"])
        self.assertNotIn("rush_start_ticks", definition)

        rush_definition = make_definition(
            replace(registration, skill_type="RUSH"), 10, rush_start_ticks=8
        )
        self.assertEqual(8, rush_definition["rush_start_ticks"])
        self.assertEqual("start", rush_definition["rush_start_animation"])
        self.assertEqual("move", rush_definition["rush_move_animation"])
        self.assertEqual("finish", rush_definition["rush_finish_animation"])

        data_definition = make_definition(registration, 6, projectiles=[{"tick": 1}], legacy_fallback=False)
        self.assertEqual([{"tick": 1}], data_definition["projectiles"])
        self.assertFalse(data_definition["legacy_fallback"])

    def test_rush_start_duration_falls_back_to_longest_available_animation(self):
        with TemporaryDirectory() as directory:
            animation_path = Path(directory) / "gengetu.json"
            animation_path.write_text(json.dumps({
                "animations": {
                    "gengetu.upper": {"animation_length": 0.4583},
                    "gengetu.under": {"animation_length": 0.5},
                }
            }), encoding="utf-8")
            self.assertEqual(10, read_rush_start_ticks(Path(directory), "gengetu"))

    def test_rush_start_duration_prefers_start_animation_when_present(self):
        with TemporaryDirectory() as directory:
            animation_path = Path(directory) / "rush.json"
            animation_path.write_text(json.dumps({
                "animations": {
                    "rush.start": {"animation_length": 0.25},
                    "rush.move": {"animation_length": 1.0},
                }
            }), encoding="utf-8")
            self.assertEqual(5, read_rush_start_ticks(Path(directory), "rush"))


if __name__ == "__main__":
    unittest.main()
