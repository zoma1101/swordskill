path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\java\com\zoma1101\swordskill\entity\custom\AttackEffectEntity.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('    private String easing = "linear";\n', '')

old = ('    public void setEasing(String easing) {\n'
       '        this.easing = (easing == null || easing.isBlank()) ? "linear" : easing;\n'
       '    }\n\n')
content = content.replace(old, '')

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')