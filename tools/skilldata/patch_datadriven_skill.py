from pathlib import Path

file_path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\swordskills\DataDrivenSkill.java")
content = file_path.read_text(encoding="utf-8")

old_code = """    private static void applyMotion(ServerPlayer player, DataSkillDefinition.Motion motion) {
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 addedMotion = forward.scale(motion.forward())
                .add(right.scale(motion.strafe()))
                .add(0, motion.vertical(), 0);
        if (addedMotion.lengthSqr() > 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(addedMotion));
            player.hurtMarked = true;
        }
    }"""

new_code = """    private static void applyMotion(ServerPlayer player, DataSkillDefinition.Motion motion) {
        if (motion.forward() == 0 && motion.strafe() == 0 && motion.vertical() == 0) {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.hurtMarked = true;
            return;
        }
        Vec3 forward = player.getLookAngle().normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 addedMotion = forward.scale(motion.forward())
                .add(right.scale(motion.strafe()))
                .add(0, motion.vertical(), 0);
        if (addedMotion.lengthSqr() > 0) {
            player.setDeltaMovement(player.getDeltaMovement().add(addedMotion));
            player.hurtMarked = true;
        }
    }"""

assert old_code in content, "old_code target not found"
new_content = content.replace(old_code, new_code)
file_path.write_text(new_content, encoding="utf-8")
print("Successfully updated DataDrivenSkill.java")
