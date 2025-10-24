package vx.velvexa.metinstones.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.StructureGrowEvent;

import vx.velvexa.metinstones.vxMetin;

public class ProtectionListener implements Listener {

    private final vxMetin plugin;

    public ProtectionListener(vxMetin plugin) {
        this.plugin = plugin;
    }

    private boolean isStone(Block b) {
        return plugin.getSpawnManager().isActiveStone(b.getLocation());
    }

    @EventHandler public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isStone);
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isStone);
    }

    @EventHandler public void onBlockBurn(BlockBurnEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockFade(BlockFadeEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockFromTo(BlockFromToEvent e) {
        if (isStone(e.getToBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockSpread(BlockSpreadEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockIgnite(BlockIgniteEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockPhysics(BlockPhysicsEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) if (isStone(b)) { e.setCancelled(true); return; }
    }

    @EventHandler public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) if (isStone(b)) { e.setCancelled(true); return; }
    }

    @EventHandler public void onBlockDropItem(BlockDropItemEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockDispense(BlockDispenseEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onBlockPlace(BlockPlaceEvent e) {
        if (isStone(e.getBlockPlaced()) || isStone(e.getBlockAgainst())) e.setCancelled(true);
    }

    @EventHandler public void onBlockGrow(BlockGrowEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onLeavesDecay(LeavesDecayEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onStructureGrow(StructureGrowEvent e) {
        e.getBlocks().removeIf(b -> isStone(b.getBlock()));
    }

    @EventHandler public void onExplosionPrime(ExplosionPrimeEvent e) {
        Location loc = e.getEntity().getLocation();
        if (isStone(loc.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onEntityBlockForm(EntityBlockFormEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler public void onLightningStrike(LightningStrikeEvent e) {
        Location center = e.getLightning().getLocation();
        int radius = 2;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = center.getWorld().getBlockAt(center.clone().add(x, y, z));
                    if (isStone(b)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }
}
