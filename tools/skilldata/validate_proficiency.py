"""data/<ns>/proficiency/<category>.json（スキル修得熟練度の上書き）を検証する。

使い方:
    python tools/skilldata/validate_proficiency.py src/main/resources/data/swordskill

引数はデータパックのルート（`data` の親）。
"""

import io
import json
import os
import sys

MAX_VALUE = 1_000_000


def load_json(path):
    with io.open(path, encoding="utf-8-sig") as handle:
        return json.load(handle)


def main():
    if len(sys.argv) != 2:
        print("usage: validate_proficiency.py <datapack root>")
        return 2

    root = sys.argv[1]
    skilldata_dir = os.path.join(root, "data", "swordskill", "skilldata")
    proficiency_dir = os.path.join(root, "data", "swordskill", "proficiency")

    if not os.path.isdir(skilldata_dir):
        print("skilldata directory not found: " + skilldata_dir)
        return 2
    if not os.path.isdir(proficiency_dir):
        print("proficiency directory not found: " + proficiency_dir)
        return 2

    skills = {}
    for name in sorted(os.listdir(skilldata_dir)):
        if not name.endswith(".json"):
            continue
        data = load_json(os.path.join(skilldata_dir, name))
        category = data.get("proficiency_category")
        if not category:
            weapons = data.get("weapons") or []
            category = weapons[0] if weapons else "NONE"
        skills[data["name"]] = {
            "category": category,
            "assignable": data.get("assignable", True),
        }

    errors = []
    entries = 0
    files = 0

    for name in sorted(os.listdir(proficiency_dir)):
        if not name.endswith(".json"):
            continue
        files += 1
        category = name[:-5].upper()
        table = load_json(os.path.join(proficiency_dir, name)).get("skills")
        if not isinstance(table, dict):
            errors.append("%s: 'skills' object is required" % name)
            continue
        for skill, value in table.items():
            entries += 1
            info = skills.get(skill)
            if info is None:
                errors.append("%s: unknown skill '%s'" % (name, skill))
            elif info["category"] != category:
                errors.append("%s: '%s' belongs to %s" % (name, skill, info["category"]))
            elif not info["assignable"]:
                errors.append("%s: '%s' is not assignable" % (name, skill))
            if not isinstance(value, int) or isinstance(value, bool):
                errors.append("%s: '%s' must be an integer" % (name, skill))
            elif value < 0 or value > MAX_VALUE:
                errors.append("%s: '%s' must be within 0..%d" % (name, skill, MAX_VALUE))

    print("category files: %d" % files)
    print("skill entries : %d" % entries)
    if errors:
        print("errors:")
        for error in errors:
            print("  - " + error)
        return 1
    print("all proficiency override files are valid")
    return 0


if __name__ == "__main__":
    sys.exit(main())
