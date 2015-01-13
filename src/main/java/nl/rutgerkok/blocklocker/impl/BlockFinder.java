package nl.rutgerkok.blocklocker.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nl.rutgerkok.blocklocker.BlockData;
import nl.rutgerkok.blocklocker.ProtectionSign;
import nl.rutgerkok.blocklocker.SignParser;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.material.Attachable;
import org.bukkit.material.Chest;
import org.bukkit.material.Directional;
import org.bukkit.material.MaterialData;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class BlockFinder {
    public static final BlockFace[] DOOR_ATTACHMENT_FACES = { BlockFace.UP, BlockFace.DOWN };
    public static final BlockFace[] CHEST_LINKING_FACES = { BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST };
    public static final BlockFace[] TRAP_DOOR_ATTACHMENT_FACES = { BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST };
    private static final BlockFace[] SIGN_ATTACHMENT_FACES = { BlockFace.NORTH, BlockFace.EAST,
            BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP };

    private SignParser parser;

    BlockFinder(SignParser parser) {
        this.parser = parser;
    }

    /**
     * Finds all attached signs to a block, that are valid protection signs.
     *
     * @param block
     *            The block to check attached signs on.
     * @return The signs.
     */
    public Collection<ProtectionSign> findAttachedSigns(Block block) {
        Collection<ProtectionSign> signs = new ArrayList<ProtectionSign>();
        for (BlockFace face : SIGN_ATTACHMENT_FACES) {
            Block atPosition = block.getRelative(face);
            Material material = atPosition.getType();
            if (material != Material.WALL_SIGN && material != Material.SIGN_POST) {
                continue;
            }
            Sign sign = (Sign) atPosition.getState();
            if (!isAttachedSign(sign, atPosition, block)) {
                continue;
            }
            Optional<ProtectionSign> parsedSign = parser.parseSign(sign);
            if (parsedSign.isPresent()) {
                signs.add(parsedSign.get());
            }
        }
        return signs;
    }

    /**
     * Finds all attached signs to a block, that are valid protection signs.
     *
     * @param blocks
     *            The blocks to check attached signs on.
     * @return The signs.
     */
    public Collection<ProtectionSign> findAttachedSigns(Collection<Block> blocks) {
        if (blocks.size() == 1) {
            // Avoid creating a builder, iterator and extra set
            return findAttachedSigns(blocks.iterator().next());
        }

        ImmutableSet.Builder<ProtectionSign> signs = ImmutableSet.builder();
        for (Block block : blocks) {
            signs.addAll(findAttachedSigns(block));
        }
        return signs.build();
    }

    /**
     * Searches for containers of the same type attached to this container.
     *
     * @param block
     *            The container.
     * @return List of attached containers, including the given container.
     */
    public List<Block> findContainerNeighbors(Block block) {
        // Currently only chests share an inventory
        // Minecraft connects two chests next to each other that have the same
        // direction. We simply check for that condition, taking both normal
        // and trapped chests into account
        if (!(BlockData.get(block) instanceof Chest)) {
            return Collections.singletonList(block);
        }

        Material chestMaterial = block.getType(); // CHEST or TRAPPED_CHEST
        BlockFace chestFacing = ((Directional) BlockData.get(block)).getFacing();

        for (BlockFace face : CHEST_LINKING_FACES) {
            Block atPosition = block.getRelative(face);
            if (atPosition.getType() != chestMaterial) {
                continue;
            }

            MaterialData materialData = BlockData.get(atPosition);
            if (!(materialData instanceof Directional)) {
                continue;
            }

            BlockFace facing = ((Directional) materialData).getFacing();
            if (!facing.equals(chestFacing)) {
                continue;
            }

            return ImmutableList.of(block, atPosition);
        }

        return Collections.singletonList(block);
    }

    /**
     * Gets the block that supports the given block. If the returned block is
     * destroyed, the given block is destroyed too.
     *
     * For blocks that are self-supporting (most blocks in Minecraft), the
     * method returns the block itself.
     * 
     * @param sign
     *            The sign.
     * @return The block the sign is attached on.
     */
    public Block findSupportingBlock(Block block) {
        MaterialData data = BlockData.get(block);
        if (data instanceof Attachable) {
            return block.getRelative(((Attachable) data).getAttachedFace());
        }
        return block;
    }

    /**
     * Gets the parser for signs.
     *
     * @return The parser.
     */
    public SignParser getSignParser() {
        return parser;
    }

    /**
     * Checks if the sign at the given position is attached to the container.
     * Doens't check the text on the sign.
     *
     * @param sign
     *            The sign to check.
     * @param signBlock
     *            The block the sign is on ({@link Block#getState()}
     *            {@code .equals(sign)} must return true)
     * @param attachedTo
     *            The block the sign must be attached to. If this is not the
     *            case, the method returns false.
     * @return True if the direction and header of the sign are valid, false
     *         otherwise.
     */
    private boolean isAttachedSign(Sign sign, Block signBlock, Block attachedTo) {
        BlockFace requiredFace = signBlock.getFace(attachedTo);
        MaterialData materialData = sign.getData();
        BlockFace actualFace = ((org.bukkit.material.Sign) materialData).getAttachedFace();
        return (actualFace == requiredFace);
    }
}
