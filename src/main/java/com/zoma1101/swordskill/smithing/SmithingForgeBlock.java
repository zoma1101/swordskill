package com.zoma1101.swordskill.smithing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 鍛冶台。右クリックで {@link SmithingForgeMenu} を開く。
 *
 * <p>在庫を持たないのでブロックエンティティは不要。開いた GUI 側で材料を管理する。</p>
 */
public class SmithingForgeBlock extends Block {

    /** GUI のタイトル。 */
    public static final String TITLE_KEY = "block.swordskill.smithing_forge";

    public SmithingForgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hitResult) {
        open(level, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        open(level, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void open(Level level, Player player) {
        if (level.isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, p) -> new SmithingForgeMenu(containerId, inventory),
                Component.translatable(TITLE_KEY));
        serverPlayer.openMenu(provider);
    }
}
