package net.cobaltmc.cobaltage.block.abstracts;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.cobaltmc.cobaltage.block.signal.SignalType;
import net.cobaltmc.cobaltage.util.interfaces.mixin.signalgetters.SignalGetterByType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RedstoneSide;

import java.util.Map;

public abstract class WireBlock extends Block {

    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public static final EnumProperty<RedstoneSide> NORTH = BlockStateProperties.NORTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> SOUTH = BlockStateProperties.SOUTH_REDSTONE;
    public static final EnumProperty<RedstoneSide> EAST = BlockStateProperties.EAST_REDSTONE;
    public static final EnumProperty<RedstoneSide> WEST = BlockStateProperties.WEST_REDSTONE;
    public static final Map<Direction, EnumProperty<RedstoneSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
            Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
    );
    protected boolean shouldSignal = true;

    public WireBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(POWER, 0)
                .setValue(NORTH, RedstoneSide.SIDE)
                .setValue(SOUTH, RedstoneSide.SIDE)
                .setValue(EAST, RedstoneSide.SIDE)
                .setValue(WEST, RedstoneSide.SIDE));
    }

    public int getBlockSignalByType(SignalType signalType, Level level, BlockPos blockPos) {
        this.shouldSignal = false;
        int i = ((SignalGetterByType)level).cobaltage$getBestNeighborSignalByType(signalType, blockPos);
        this.shouldSignal = true;
        return i;
    }

}
