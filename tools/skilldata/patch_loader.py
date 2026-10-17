from pathlib import Path

file_path = Path(r"d:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\data\SkillDataLoader.java")
content = file_path.read_text(encoding="utf-8")

old_code = """        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        definitions.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(),
                                entry.getKey().toString())));
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to load skill data {}: {}", entry.getKey(), exception.getMessage());
                    }
                });"""

new_code = """        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        LOGGER.warn("Skipping non-object skilldata file {}: expected a JSON object", entry.getKey());
                        return;
                    }
                    try {
                        definitions.add(parse(entry.getKey(), GsonHelper.convertToJsonObject(entry.getValue(),
                                entry.getKey().toString())));
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to load skill data {}: {}", entry.getKey(), exception.getMessage());
                    }
                });"""

new_content = content.replace(old_code.replace("\r\n", "\n"), new_code.replace("\r\n", "\n")).replace(old_code, new_code)
file_path.write_text(new_content, encoding="utf-8")
print("SkillDataLoader patched successfully")
