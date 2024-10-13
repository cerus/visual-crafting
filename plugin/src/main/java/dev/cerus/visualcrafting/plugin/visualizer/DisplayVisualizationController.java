package dev.cerus.visualcrafting.plugin.visualizer;

import dev.cerus.visualcrafting.api.math.MatrixMath;
import dev.cerus.visualcrafting.api.version.FakeItemDisplay;
import dev.cerus.visualcrafting.api.version.Feature;
import dev.cerus.visualcrafting.api.version.VersionAdapter;
import static dev.cerus.visualcrafting.plugin.visualizer.DirectionProvider.getDirection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix3f;

public class DisplayVisualizationController implements VisualizationController {

    private final Map<Long, Visualization<DisplayGrid>> visualizationMap = new HashMap<>();
    private final VersionAdapter versionAdapter;

    public DisplayVisualizationController(final VersionAdapter versionAdapter) {
        this.versionAdapter = versionAdapter;
    }

    @Override
    public boolean accepts(final VersionAdapter versionAdapter) {
        return versionAdapter.getImplementedFeatures().contains(Feature.ITEM_DISPLAYS);
    }

    @Override
    public void entityClick(final Player player, final int eid) {
        // no op
    }

    @Override
    public void recipeSelected(final ItemStack[] matrix, final ItemStack result, final Player actor, final Block craftingTable) {
        if (!craftingTable.getRelative(BlockFace.UP).getType().isTransparent()) {
            return;
        }

        final long key = this.getBlockKey(craftingTable);
        if (!this.visualizationMap.containsKey(key)) {
            final DisplayGrid displayGrid = new DisplayGrid();
            displayGrid.setItems(matrix, result);
            displayGrid.adjustTo(craftingTable.getLocation().clone().add(0, 1, 0),
                    getDirection(actor.getLocation().getYaw()).getOppositeFace());
            final Visualization<DisplayGrid> visualization = new Visualization<>(craftingTable, actor, displayGrid);
            this.visualizationMap.put(key, visualization);
            displayGrid.spawnDisplays(this.versionAdapter);
            displayGrid.updateDisplays(this.versionAdapter);
        } else {
            final Visualization<DisplayGrid> visualization = this.visualizationMap.get(key);
            if (!visualization.player.getUniqueId().equals(actor.getUniqueId())) {
                // Someone else is using this crafting table
                return;
            }

            final DisplayGrid displayGrid = visualization.obj;
            displayGrid.setItems(matrix, result);
            displayGrid.adjustTo(craftingTable.getLocation().clone().add(0, 1, 0),
                    getDirection(actor.getLocation().getYaw()).getOppositeFace());
            displayGrid.updateDisplays(this.versionAdapter);
        }
    }

    @Override
    public void craftingCancelled(final Player actor, final Block craftingTable) {
        final long key = this.getBlockKey(craftingTable);
        if (!this.visualizationMap.containsKey(key)) {
            return;
        }

        final Visualization<DisplayGrid> visualization = this.visualizationMap.get(key);
        if (!visualization.player.getUniqueId().equals(actor.getUniqueId())) {
            return;
        }

        this.visualizationMap.remove(key);
        visualization.obj.destroyDisplays(this.versionAdapter);
    }

    private long getBlockKey(final Block block) {
        return this.getBlockKey(block.getX(), block.getY(), block.getZ());
    }

    // Stolen from Paper
    private long getBlockKey(final int x, final int y, final int z) {
        return ((long) x & 0x7FFFFFF) | (((long) z & 0x7FFFFFF) << 27) | ((long) y << 54);
    }

    private static class DisplayGrid {

        private static final double PIXEL_SIZE = 1d / 16d;
        private static final double FIRST_PIXEL_OFF = PIXEL_SIZE * 5;
        private static final double PIXEL_OFF = PIXEL_SIZE * 3;
        private static final ItemStack AIR = new ItemStack(Material.AIR);

        private final FakeItemDisplay[] craftingMatrix = new FakeItemDisplay[3 * 3 * 3];
        private final FakeItemDisplay craftingResult;
        private final int[] matrixEids = new int[3 * 3 * 3];
        private int resultEid;

        public DisplayGrid() {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    this.setMatrixCell(col, row, this.createDisplay());
                }
            }
            this.craftingResult = this.createDisplay();
        }

        private FakeItemDisplay createDisplay() {
            return new FakeItemDisplay(null, AIR, MatrixMath.rotationX((float) Math.toRadians(90)),
                    MatrixMath.rotationZ(0f), MatrixMath.translation(0f, 0f, 0f), FakeItemDisplay.Transform.GUI);
        }

        public void spawnDisplays(final VersionAdapter versionAdapter) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    final int eid = versionAdapter.spawnItemDisplay(this.getMatrixCell(col, row));
                    this.setMatrixEid(col, row, eid);
                }
            }
            this.setResultEid(versionAdapter.spawnItemDisplay(this.craftingResult));
        }

        public void updateDisplays(final VersionAdapter versionAdapter) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    versionAdapter.updateItemDisplay(this.getMatrixEid(col, row), this.getMatrixCell(col, row));
                }
            }
            versionAdapter.updateItemDisplay(this.getResultEid(), this.getCraftingResult());
        }

        public void destroyDisplays(final VersionAdapter versionAdapter) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    versionAdapter.destroyEntity(this.getMatrixEid(col, row));
                }
            }
            versionAdapter.destroyEntity(this.getResultEid());
        }

        public void setItems(final ItemStack[] matrix, final ItemStack result) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    final ItemStack item = matrix[row * 3 + col];
                    this.getMatrixCell(col, row).setItemStack(item == null ? AIR : item);
                }
            }
            this.craftingResult.setItemStack(result == null ? AIR : result);
        }

        public void adjustTo(final Location loc, final BlockFace facing) {
            switch (facing) {
                case NORTH -> this.adjustPos(loc, 1 - FIRST_PIXEL_OFF, 0, -PIXEL_OFF, 1 - FIRST_PIXEL_OFF, -PIXEL_OFF, 0);
                case EAST -> this.adjustPos(loc, FIRST_PIXEL_OFF, PIXEL_OFF, 0, 1 - FIRST_PIXEL_OFF, 0, -PIXEL_OFF);
                case SOUTH -> this.adjustPos(loc, FIRST_PIXEL_OFF, 0, PIXEL_OFF, FIRST_PIXEL_OFF, PIXEL_OFF, 0);
                case WEST -> this.adjustPos(loc, 1 - FIRST_PIXEL_OFF, -PIXEL_OFF, 0, FIRST_PIXEL_OFF, 0, PIXEL_OFF);
            }
            this.adjustRot(facing);
        }

        private void adjustPos(final Location baseLoc, final double startX, final double addXRow, final double addXCol, final double startZ, final double addZRow, final double addZCol) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    final FakeItemDisplay item = this.getMatrixCell(col, row);
                    final double x = startX + (row * addXRow) + (col * addXCol);
                    final double z = startZ + (row * addZRow) + (col * addZCol);
                    //item.setLocation(baseLoc.clone().add(x, 0, z));
                    item.setLocation(baseLoc);
                    item.setTranslation(MatrixMath.translation((float) x, 0.005f, (float) z));
                }
            }

            final double x = startX + addXCol + this.ceil(addXRow) * -0.21;
            final double z = startZ + addZCol + this.ceil(addZRow) * -0.21;
            //this.craftingResult.setLocation(baseLoc.clone().add(x, 0, z));
            this.craftingResult.setLocation(baseLoc);
            this.craftingResult.setTranslation(MatrixMath.translation((float) x, 0.005f, (float) z));
        }

        private void adjustRot(final BlockFace facing) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    final FakeItemDisplay item = this.getMatrixCell(col, row);
                    item.setRotationZ(this.rotationZ(facing));
                }
            }
            this.craftingResult.setRotationZ(this.rotationZ(facing));
        }

        private Matrix3f rotationZ(final BlockFace face) {
            return switch (face) {
                case NORTH -> MatrixMath.rotationZ((float) Math.toRadians(0));
                case EAST -> MatrixMath.rotationZ((float) Math.toRadians(90));
                case SOUTH -> MatrixMath.rotationZ((float) Math.toRadians(180));
                case WEST -> MatrixMath.rotationZ((float) Math.toRadians(270));
                default -> throw new UnsupportedOperationException();
            };
        }

        private double ceil(final double f) {
            return f < 0 ? -this.ceil(-f) : Math.ceil(f);
        }

        public void setMatrixCell(final int col, final int row, final FakeItemDisplay display) {
            this.craftingMatrix[row * 3 + col] = display;
        }

        public FakeItemDisplay getMatrixCell(final int col, final int row) {
            return this.craftingMatrix[row * 3 + col];
        }

        public FakeItemDisplay getCraftingResult() {
            return this.craftingResult;
        }

        public void setMatrixEid(final int col, final int row, final int eid) {
            this.matrixEids[row * 3 + col] = eid;
        }

        public int getMatrixEid(final int col, final int row) {
            return this.matrixEids[row * 3 + col];
        }

        public int getResultEid() {
            return this.resultEid;
        }

        public void setResultEid(final int resultEid) {
            this.resultEid = resultEid;
        }

    }

}
