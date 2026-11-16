path = r'D:\MOD\Mods\NeoForge[1.21.1]\Swordskill\src\main\resources\data\swordskill\skilldata\horizontal_square.json'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old1 = '      "player_pitch_x_multiplier": -2,\n'
new1 = '      "player_pitch_x_multiplier": -2,\n      "pitch_limit_degrees": 30,\n'
old2 = '      "player_pitch_x_multiplier": -1,\n      "player_pitch_z_multiplier": 1,'
new2 = '      "player_pitch_x_multiplier": -1,\n      "pitch_limit_degrees": 30,\n      "player_pitch_z_multiplier": 1,'
old3 = '      "player_pitch_x_multiplier": -1,\n      "player_pitch_z_multiplier": -1,'
new3 = '      "player_pitch_x_multiplier": -1,\n      "pitch_limit_degrees": 30,\n      "player_pitch_z_multiplier": -1,'

assert content.count(old1) == 1, 'old1 count: ' + str(content.count(old1))
assert content.count(old2) == 1, 'old2 count: ' + str(content.count(old2))
assert content.count(old3) == 1, 'old3 count: ' + str(content.count(old3))

content = content.replace(old1, new1, 1)
content = content.replace(old2, new2, 1)
content = content.replace(old3, new3, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print('done')