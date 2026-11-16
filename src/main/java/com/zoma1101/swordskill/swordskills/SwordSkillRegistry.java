package com.zoma1101.swordskill.swordskills;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * スキル定義の登録所。
 *
 * <p>標準のスキルはすべて {@code data/<namespace>/skilldata/*.json} のデータ定義で登録される。
 * Java でしか表現できない挙動が必要な場合だけ {@link #registerJavaSkill(SkillData)} で
 * {@link ISkill} の実装を登録できる。</p>
 *
 * <p>スキル ID は 0 から連番でなければならない。クライアントのスキル選択画面が
 * ID をそのまま一覧の並び順として使っているため。</p>
 */
public final class SwordSkillRegistry {

    /** ID → スキル。データ定義の読み込みで作り直される。 */
    public static final Map<Integer, SkillData> SKILLS = new LinkedHashMap<>();

    /** スキルID（文字列） → スキル。旧数値 ID は内部互換として残す。 */
    public static final Map<String, SkillData> SKILLS_BY_ID = new LinkedHashMap<>();

    /**
     * Java 実装として登録されたスキル。データ定義は同じ ID を
     * {@code replace_builtin: true} で上書きできる。
     */
    private static final Map<Integer, SkillData> JAVA_SKILLS = new LinkedHashMap<>();

    private SwordSkillRegistry() {
    }

    /**
     * Java 実装のスキルを登録する。MOD 初期化時（クライアント・サーバーの両方）に呼ぶこと。
     *
     * <p>データ定義（JSON）はこの後に読み込まれる。ID が同じデータ定義は
     * {@code replace_builtin: true} のときだけこのスキルを置き換える。</p>
     */
    public static synchronized void registerJavaSkill(SkillData skill) {
        Objects.requireNonNull(skill, "skill");
        if (skill.getSkillClass() == DataDrivenSkill.class) {
            throw new IllegalArgumentException(
                    "DataDrivenSkill は registerJavaSkill では登録できません（JSON の skilldata を使ってください）: "
                            + skill.getName());
        }
        if (JAVA_SKILLS.containsKey(skill.getId())) {
            throw new IllegalArgumentException("Java スキルの ID が重複しています: " + skill.getId());
        }
        JAVA_SKILLS.put(skill.getId(), skill);
        SKILLS.put(skill.getId(), skill);
        SKILLS_BY_ID.put(skill.getName(), skill);
    }

    /** 登録済みの Java スキル（読み取り専用）。 */
    public static synchronized Map<Integer, SkillData> getJavaSkills() {
        return Map.copyOf(JAVA_SKILLS);
    }

    /**
     * データ定義でレジストリを作り直す。Java スキルを土台に、JSON 定義を ID 順で載せる。
     *
     * <p>{@code id} が 0 かつ {@code replace_builtin} でない定義は、空いている最小の ID を自動で振る。</p>
     */
    public static synchronized void replaceDataDrivenSkills(Collection<DataSkillDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        List<DataSkillDefinition> ordered = definitions.stream()
                .sorted(Comparator.comparingInt(DataSkillDefinition::legacyId)
                        .thenComparing(DataSkillDefinition::id))
                .toList();

        Map<Integer, DataSkillDefinition> resolved = new LinkedHashMap<>();
        Set<Integer> usedIds = new HashSet<>();
        Set<String> usedNames = new HashSet<>();
        List<DataSkillDefinition> autoIdDefinitions = new ArrayList<>();
        for (DataSkillDefinition definition : ordered) {
            if (definition.id().isBlank() || definition.legacyId() < 0) {
                throw new IllegalArgumentException("Invalid data skill id: " + definition.id());
            }
            if (!usedNames.add(definition.id())) {
                throw new IllegalArgumentException("Duplicate data skill id: " + definition.id());
            }
            int intId = definition.legacyId();
            if (intId <= 0 && definition.replaceBuiltin()) {
                // 旧 Java スキルを名前で置き換える場合はその内部 ID を引き継ぐ
                for (SkillData javaSkill : JAVA_SKILLS.values()) {
                    if (javaSkill.getName().equals(definition.id())) {
                        intId = javaSkill.getId();
                        break;
                    }
                }
            }
            if (intId > 0) {
                if (!usedIds.add(intId)) {
                    throw new IllegalArgumentException("Duplicate legacy skill ID: " + intId
                            + " (" + definition.id() + ")");
                }
                if (JAVA_SKILLS.containsKey(intId) && !definition.replaceBuiltin()) {
                    throw new IllegalArgumentException("Skill ID " + definition.id()
                            + " conflicts with a Java skill; set replace_builtin=true to override it");
                }
                resolved.put(intId, definition.withLegacyId(intId));
            } else {
                autoIdDefinitions.add(definition);
            }
        }

        int nextAvailableId = 0;
        for (DataSkillDefinition definition : autoIdDefinitions) {
            while (usedIds.contains(nextAvailableId) || JAVA_SKILLS.containsKey(nextAvailableId)) {
                nextAvailableId++;
            }
            int assignedId = nextAvailableId++;
            usedIds.add(assignedId);
            resolved.put(assignedId, definition.withLegacyId(assignedId));
        }

        Map<Integer, SkillData> rebuilt = new LinkedHashMap<>(JAVA_SKILLS);
        resolved.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> rebuilt.put(entry.getKey(), createDataSkill(entry.getValue())));

        if (!rebuilt.isEmpty()) {
            int highestId = rebuilt.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
            if (rebuilt.size() != highestId + 1) {
                throw new IllegalArgumentException("Skill IDs must be contiguous from 0; found IDs " + rebuilt.keySet()
                        + ". 新規スキルは id にスキル名を指定すると自動採番されます");
            }
        }

        SKILLS.clear();
        SKILLS.putAll(rebuilt);
        SKILLS_BY_ID.clear();
        for (SkillData skill : rebuilt.values()) {
            SKILLS_BY_ID.put(skill.getName(), skill);
        }
    }

    /** スキルID（文字列）でスキルを取得する。 */
    public static SkillData get(String skillId) {
        return skillId == null ? null : SKILLS_BY_ID.get(skillId);
    }

    /** 内部の数値 ID からスキルID（文字列）を取得する。 */
    public static String idOf(int intId) {
        SkillData skill = SKILLS.get(intId);
        return skill == null ? null : skill.getName();
    }

    /** スキルID（文字列）から内部の数値 ID を取得する。見つからなければ -1。 */
    public static int intIdOf(String skillId) {
        SkillData skill = get(skillId);
        return skill == null ? -1 : skill.getId();
    }

    private static SkillData createDataSkill(DataSkillDefinition definition) {
        return new SkillData(definition.legacyId(), definition.name(), definition.cooldown(), definition.spCost(),
                definition.type(), DataDrivenSkill.class, definition.weapons(), definition.hidden(),
                definition.durationTicks(), definition.transformLimitTicks(), definition);
    }

    /** クライアントへ同期するデータ定義（ID 順）。 */
    public static synchronized List<DataSkillDefinition> getDataDrivenDefinitions() {
        return SKILLS.values().stream()
                .filter(skill -> skill.getDataDefinition() != null)
                .sorted(Comparator.comparingInt(SkillData::getId))
                .map(SkillData::getDataDefinition)
                .toList();
    }
}
