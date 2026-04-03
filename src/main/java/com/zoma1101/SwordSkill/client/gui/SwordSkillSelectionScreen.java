package com.zoma1101.swordskill.client.gui;
 
import com.zoma1101.swordskill.SwordSkill;
import com.zoma1101.swordskill.client.handler.ClientSkillSlotHandler;
import com.zoma1101.swordskill.client.screen.Keybindings;
import com.zoma1101.swordskill.network.CheckSkillUnlockedPacket;
import com.zoma1101.swordskill.network.NetworkHandler;
import com.zoma1101.swordskill.network.SkillSlotSelectionPacket;
import com.zoma1101.swordskill.network.SkillUnlockPacket;
import com.zoma1101.swordskill.swordskills.SkillData;
import com.zoma1101.swordskill.swordskills.SwordSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
 
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
 
import static com.zoma1101.swordskill.config.ServerConfig.UnlockedSkill;
import static com.zoma1101.swordskill.item.SampleItemRegistry.UNLOCKITEM;
import static net.minecraft.resources.ResourceLocation.fromNamespaceAndPath;
 
@OnlyIn(Dist.CLIENT)
public class SwordSkillSelectionScreen extends Screen {
    public Set<Integer> unlockedSkills = new HashSet<>();
    public int selectedSkillIndex = 0;
    private Set<SkillData.WeaponType> weaponType;
    public boolean martialArtsUnlocked = false; // ★追加
 
    private static final Map<SkillData.WeaponType, ResourceLocation> WEAPON_ICON_CACHE = new HashMap<>();
 
    public SwordSkillSelectionScreen() {
        super(Component.translatable("gui." + SwordSkill.MOD_ID + ".title"));
        if (Minecraft.getInstance().player != null) {
            weaponType = ClientSkillSlotHandler.getCurrentWeaponTypes();
            // 最初の下図が非表示対象（未解禁の体術など）なら、次の表示可能なスキルを探す
            if (!isSkillDisplayable(SwordSkillRegistry.SKILLS.get(selectedSkillIndex))) {
                selectedSkillIndex = getNextDisplayableSkillIndex(selectedSkillIndex);
            }
            NetworkHandler.INSTANCE.sendToServer(new CheckSkillUnlockedPacket());
        }
    }
 
    public void updateUnlockedSkills(int[] skillIds) {
        this.unlockedSkills.clear();
        for (int id : skillIds) {
            this.unlockedSkills.add(id);
        }
    }
 
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A) {
            selectedSkillIndex = getPreviousDisplayableSkillIndex(selectedSkillIndex);
        } else if (keyCode == GLFW.GLFW_KEY_D) {
            selectedSkillIndex = getNextDisplayableSkillIndex(selectedSkillIndex);
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
 
        if (unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get()) {
            if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_0.getKey().getValue()) {
                saveSkillToSlot(0);
                return true;
            } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_1.getKey().getValue()) {
                saveSkillToSlot(1);
                return true;
            } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_2.getKey().getValue()) {
                saveSkillToSlot(2);
                return true;
            } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_3.getKey().getValue()) {
                saveSkillToSlot(3);
                return true;
            } else if (keyCode == Keybindings.INSTANCE.SwordSkill_Use_Key_4.getKey().getValue()) {
                saveSkillToSlot(4);
                return true;
            }
        }
        return handled;
    }
 
    private void ViewButton() {
        this.clearWidgets();
        SkillData selectedSkill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
 
        if (selectedSkill != null && selectedSkill.getType() == SkillData.SkillType.TRANSFORM || !UnlockedSkill.get()) {
            int index = 0;
            for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                    .equals(SkillData.SkillType.TRANSFORM_FINISH); i++) {
                index = selectedSkillIndex + i + 1;
            }
 
            if (!unlockedSkills.contains(index)) {
                Button unlockDerivedButton = Button
                        .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock_derived"),
                                button -> unlockDerivedSkill())
                        .bounds(width / 2 - 80, height - 65, 160, 20)
                        .build();
                addRenderableWidget(unlockDerivedButton);
            }
        }
 
        if (unlockedSkills.contains(selectedSkillIndex) || !UnlockedSkill.get()) {
            Button selectButton = Button
                    .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".select"),
                            button -> openSlotSelectionScreen())
                    .bounds(width / 2 - 80, height - 40, 160, 20)
                    .build();
            addRenderableWidget(selectButton);
        } else {
            Button unlockButton = Button
                    .builder(Component.translatable("gui." + SwordSkill.MOD_ID + ".unlock"),
                            button -> unlockSkill(selectedSkillIndex))
                    .bounds(width / 2 - 80, height - 40, 160, 20)
                    .build();
            addRenderableWidget(unlockButton);
        }
    }
 
    private void openSlotSelectionScreen() {
        Minecraft.getInstance().setScreen(new DecideSkillScreen(selectedSkillIndex));
    }
 
    private void unlockSkill(int selectedSkill) {
        if (this.minecraft != null && this.minecraft.player != null && !this.minecraft.player.isCreative()) {
            Inventory inventory = this.minecraft.player.getInventory();
            for (int i = 0; i < inventory.getContainerSize(); ++i) {
                if (inventory.getItem(i).is(UNLOCKITEM.get())) {
                    inventory.removeItem(i, 1);
                    break;
                }
            }
        }
        NetworkHandler.INSTANCE.sendToServer(new SkillUnlockPacket(selectedSkill));
    }
 
    private void unlockDerivedSkill() {
        int index = 0;
        for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                .equals(SkillData.SkillType.TRANSFORM_FINISH) && unlockedSkills.contains(selectedSkillIndex + i); i++) {
            index = selectedSkillIndex + i + 1;
        }
        unlockSkill(index);
    }
 
    private void saveSkillToSlot(int slotIndex) {
        if (minecraft != null && minecraft.player != null) {
            SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
            if (skill != null) {
                String playerWeaponType = ClientSkillSlotHandler.getCurrentWeaponName();
                if (playerWeaponType != null && !playerWeaponType.equals("None")) {
                    NetworkHandler.INSTANCE
                            .sendToServer(new SkillSlotSelectionPacket(skill.getId(), slotIndex, playerWeaponType));
                }
            }
        }
    }
 
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        int centerX = width / 2;
        int centerY = height / 2;
        ViewButton();
        SkillData skill = SwordSkillRegistry.SKILLS.get(selectedSkillIndex);
 
        if (skill != null && !skill.isHide()) {
            // --- レイアウト座標の設定 ---
            int infoStartX = centerX - 50; // テキストなどの開始X
            int iconX = centerX - 95;      // スキルアイコンのX
            int iconY = centerY - 65;      // アイコンの基準Y
            
            // 1. スキル名
            guiGraphics.drawString(font,
                    Component.translatable(SwordSkill.MOD_ID + ".skill." + skill.getName()).getString(), 
                    infoStartX, iconY + 5, 0x00FFAA);
 
            // 2. コスト情報
            guiGraphics.drawString(font,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".cooldown", skill.getCooldown()).getString(),
                    infoStartX, iconY + 17, 0X00FF7F);
            guiGraphics.drawString(font,
                    Component.translatable("gui." + SwordSkill.MOD_ID + ".sp_cost", skill.getSpCost()).getString(),
                    infoStartX, iconY + 27, 0X00BFFF);
 
            // 3. 使用可能な武器アイコン
            int weaponIconX = infoStartX;
            int weaponIconY = iconY + 40;
            for (SkillData.WeaponType type : skill.getAvailableWeaponTypes()) {
                ResourceLocation iconTexture = getWeaponIconTexture(type);
                guiGraphics.blit(iconTexture, weaponIconX, weaponIconY, 0, 0, 16, 16, 16, 16);
 
                if (mouseX >= weaponIconX && mouseX < weaponIconX + 16 && mouseY >= weaponIconY && mouseY < weaponIconY + 16) {
                    guiGraphics.renderTooltip(font,
                            Component.translatable(SwordSkill.MOD_ID + ".weapon." + type.name().toLowerCase()), mouseX,
                            mouseY);
                }
                weaponIconX += 18;
            }
 
            // 4. スキルアイコン (大きく表示)
            if (skill.getIconTexture() != null) {
                guiGraphics.blit(skill.getIconTexture(), iconX, iconY, 0, 0, 40, 40, 40, 40);
            }
 
            // 5. 説明文 (アイコンや武器表示の下から開始して重なりを回避)
            int descriptionY = centerY + 5;
            String descriptionKey = SwordSkill.MOD_ID + ".skill." + skill.getName() + ".description";
            String description = Component.translatable(descriptionKey).getString();
            String[] descriptionLines = description.split("\n");
 
            int currentY = descriptionY;
            for (String line : descriptionLines) {
                guiGraphics.drawString(font, line, centerX - 120, currentY, 0xFFFFFF);
                currentY += font.lineHeight + 2;
            }
 
            // 6. 派生スキルの表示 (説明文のさらに下)
            HashSet<Integer> derivedSkillIds = getDerivedSkills();
            if (!derivedSkillIds.isEmpty()) {
                currentY += 5;
                guiGraphics.drawString(font,
                        Component.translatable("gui." + SwordSkill.MOD_ID + ".skill.derived").getString() + ":",
                        centerX - 120, currentY, 0xFFAA00);
                currentY += font.lineHeight + 2;
 
                for (Integer derivedSkillId : derivedSkillIds) {
                    if (derivedSkillId >= 0 && derivedSkillId < SwordSkillRegistry.SKILLS.size()) {
                        SkillData derivedSkill = SwordSkillRegistry.SKILLS.get(derivedSkillId);
                        String localizedName = "- " + Component
                                .translatable(SwordSkill.MOD_ID + ".skill." + derivedSkill.getName()).getString();
                        
                        int color = unlockedSkills.contains(derivedSkillId) ? 0x87CEEB : 0xAAAAAA;
                        guiGraphics.drawString(font, localizedName, centerX - 120, currentY, color);
                        currentY += font.lineHeight + 2;
                    }
                }
            }
        }
 
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }
 
    @Override
    public void onClose() {
        super.onClose();
    }

    private boolean isSkillDisplayable(SkillData skill) {
        if (skill == null || skill.isHide()) return false;
        
        // 体術スキルの場合、解禁フラグをチェック
        if (skill.getAvailableWeaponTypes().contains(SkillData.WeaponType.MARTIAL_ARTS) && !martialArtsUnlocked) {
            return false;
        }

        return skill.getAvailableWeaponTypes().stream().anyMatch(weaponType::contains);
    }
 
    private int getPreviousDisplayableSkillIndex(int currentIndex) {
        int previousIndex = currentIndex;
        int total = SwordSkillRegistry.SKILLS.size();
        for (int i = 0; i < total; i++) {
            previousIndex = (previousIndex - 1 + total) % total;
            if (isSkillDisplayable(SwordSkillRegistry.SKILLS.get(previousIndex))) {
                return previousIndex;
            }
        }
        return currentIndex;
    }
 
    private int getNextDisplayableSkillIndex(int currentIndex) {
        int nextIndex = currentIndex;
        int total = SwordSkillRegistry.SKILLS.size();
        for (int i = 0; i < total; i++) {
            nextIndex = (nextIndex + 1) % total;
            if (isSkillDisplayable(SwordSkillRegistry.SKILLS.get(nextIndex))) {
                return nextIndex;
            }
        }
        return currentIndex;
    }
 
    private ResourceLocation getWeaponIconTexture(SkillData.WeaponType type) {
        return WEAPON_ICON_CACHE.computeIfAbsent(type, t -> fromNamespaceAndPath(SwordSkill.MOD_ID,
                "textures/gui/weapon_icons/" + t.name().toLowerCase() + ".png"));
    }
 
    private HashSet<Integer> getDerivedSkills() {
        HashSet<Integer> TransformSkillList = new HashSet<>();
        if (SwordSkillRegistry.SKILLS.get(selectedSkillIndex).getType().equals(SkillData.SkillType.TRANSFORM)) {
            for (int i = 0; !SwordSkillRegistry.SKILLS.get(selectedSkillIndex + i).getType()
                    .equals(SkillData.SkillType.TRANSFORM_FINISH); i++) {
                TransformSkillList.add(selectedSkillIndex + i + 1);
            }
        }
        return TransformSkillList;
    }
}