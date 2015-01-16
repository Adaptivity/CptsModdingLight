package coloredlightscore.src.helper;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;

import static coloredlightscore.src.asm.ColoredLightsCoreLoadingPlugin.CLLog;

public class CLWorldHelper {
    //Copied from the world class in 1.7.2, modified from the source from 1.6.4, made the method STATIC
    //Added the parameter 'World world, ' and then replaces all instances of world, with WORLD
    public static int getBlockLightValue_do(World world, int x, int y, int z, boolean par4) {
        if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
            if (par4 && world.getBlock(x, y, z).getUseNeighborBrightness()) {
                // heaton84 - should be world.getBlockLightValue_do,
                // switched to CLWorldHelper.getBlockLightValue_do
                // This will save an extra invoke
                int l1 = CLWorldHelper.getBlockLightValue_do(world, x, y + 1, z, false);
                int l = CLWorldHelper.getBlockLightValue_do(world, x + 1, y, z, false);
                int i1 = CLWorldHelper.getBlockLightValue_do(world, x - 1, y, z, false);
                int j1 = CLWorldHelper.getBlockLightValue_do(world, x, y, z + 1, false);
                int k1 = CLWorldHelper.getBlockLightValue_do(world, x, y, z - 1, false);

                if ((l & 0xf) > (l1 & 0xf)) {
                    l1 = l;
                }

                if ((i1 & 0xf) > (l1 & 0xf)) {
                    l1 = i1;
                }

                if ((j1 & 0xf) > (l1 & 0xf)) {
                    l1 = j1;
                }

                if ((k1 & 0xf) > (l1 & 0xf)) {
                    l1 = k1;
                }

                return l1;
            } else if (y < 0) {
                return 0;
            } else {
                if (y >= 256) {
                    y = 255;
                }

                //int cx = x >> 4;
                //int cz = z >> 4;
                Chunk chunk = world.getChunkFromChunkCoords(x >> 4, z >> 4);
                x &= 0xf;
                z &= 0xf;

                //CLLog.info("NEWTEST {},{}:{}", cx, cz, Integer.toBinaryString(chunk.getBlockLightValue(0, 0, 0, 15)));

                return chunk.getBlockLightValue(x, y, z, world.skylightSubtracted);
            }
        } else {
            return 15;
        }
    }

    //Use this one if you want color
    @SideOnly(Side.CLIENT)
    public static int getLightBrightnessForSkyBlocks(World world, int x, int y, int z, int lightValue) {
        int skyBrightness = world.getSkyBlockTypeBrightness(EnumSkyBlock.Sky, x, y, z);
        int blockBrightness = world.getSkyBlockTypeBrightness(EnumSkyBlock.Block, x, y, z);

        lightValue = ((lightValue & 0xf) | ((lightValue & 0x1e0) >> 1) | ((lightValue & 0x3c00) >> 2) | ((lightValue & 0x78000) >> 3));

        blockBrightness = ((blockBrightness & 0xf) | ((blockBrightness & 0x1e0) >> 1) | ((blockBrightness & 0x3c00) >> 2) | ((blockBrightness & 0x78000) >> 3));

        if ((blockBrightness & 0x000f) < (lightValue & 0x000f)) {
            blockBrightness = blockBrightness & 0xfff0 | lightValue & 0x000f;
        }
        if ((blockBrightness & 0x00f0) < (lightValue & 0x00f0)) {
            blockBrightness = blockBrightness & 0xff0f | lightValue & 0x00f0;
        }
        if ((blockBrightness & 0x0f00) < (lightValue & 0x0f00)) {
            blockBrightness = blockBrightness & 0xf0ff | lightValue & 0x0f00;
        }
        if ((blockBrightness & 0xf000) < (lightValue & 0xf000)) {
            blockBrightness = blockBrightness & 0x0fff | lightValue & 0xf000;
        }
        return skyBrightness << 20 | blockBrightness << 4;
    }

    public static int computeLightValue(World world, int parX, int parY, int parZ, EnumSkyBlock par1Enu) {
        if (par1Enu == EnumSkyBlock.Sky && world.canBlockSeeTheSky(parX, parY, parZ)) {
            return 15;
        } else {
            Block block = world.getBlock(parX, parY, parZ);

            int currentLight = 0;
            if (par1Enu != EnumSkyBlock.Sky) {
                currentLight = (block == null ? 0 : block.getLightValue(world, parX, parY, parZ));
            }
            int opacity = (block == null ? 0 : block.getLightOpacity(world, parX, parY, parZ));

            if (opacity >= 15 && currentLight > 0) {
                opacity = 1;
            }

            if (opacity < 1) {
                opacity = 1;
            }

            if (opacity >= 15) {
                return 0;
            }
            else if ((currentLight&15) >= 14) {
                return currentLight;
            }
            else {
                for (int faceIndex = 0; faceIndex < 6; ++faceIndex) {
                    int l1 = parX + Facing.offsetsXForSide[faceIndex];
                    int i2 = parY + Facing.offsetsYForSide[faceIndex];
                    int j2 = parZ + Facing.offsetsZForSide[faceIndex];

                    int neighborLight = world.getSavedLightValue(par1Enu, l1, i2, j2);
                    int ll = neighborLight & 0x0000F;
                    int rl = neighborLight & 0x001E0;
                    int gl = neighborLight & 0x03C00;
                    int bl = neighborLight & 0x78000;

                    ll -= opacity & 0x0000F;
                    /* TODO: Colored Opacity
                    rl -= opacity & 0x001E0;
                    gl -= opacity & 0x03C00;
                    bl -= opacity & 0x78000;
                    */
                    //Use vanilla light opacity for now
                    rl =  Math.max(0, rl - (opacity << 5));
                    gl =  Math.max(0, gl - (opacity << 10));
                    bl =  Math.max(0, bl - (opacity << 15));

                    // For each component, retain greater of currentLight, (neighborLight - opacity)
                    if (ll > (currentLight & 0x0000F)) {
                        currentLight = (currentLight & 0x7BDE0) | ll; // 0x1E0 | 0x3C00 | 0x78000
                    }
                    if (rl > (currentLight & 0x001E0)) {
                        currentLight = (currentLight & 0x7BC0F) | rl; // 0x00F | 0x3C00 | 0x78000
                    }   
                    if (gl > (currentLight & 0x03C00)) {
                        currentLight = (currentLight & 0x781EF) | gl; // 0x00F | 0x01E0 | 0x78000
                    }
                    if (bl > (currentLight & 0x78000)) {
                        currentLight = (currentLight & 0x03DEF) | bl; // 0x00F | 0x01E0 | 0x03C00
                    }
                }
                return currentLight;
            }
        }
    }

    public static boolean updateLightByType(World world, EnumSkyBlock par1Enu, int par_x, int par_y, int par_z) {
        if (!world.doChunksNearChunkExist(par_x, par_y, par_z, 17)) {
            return false;
        } else {
            world.theProfiler.startSection("getBrightness");

            long savedLightValue = world.getSavedLightValue(par1Enu, par_x, par_y, par_z);
            long compLightValue = CLWorldHelper.computeLightValue(world, par_x, par_y, par_z, par1Enu);

            world.theProfiler.endStartSection("lightAddition");

            if ((((0x100000 | savedLightValue) - compLightValue) & 0x84210) > 0) { //compLightValue has components that are larger than savedLightValue, the block at the current position is brighter than the saved value at the current positon... it must have been made brighter somehow
                int idx_x;
                int idx_y;
                int idx_z;

                int offset_x;
                int offset_y;
                int offset_z;

                int max = (int) compLightValue & 0xF;
                int offset = 15 - max;

                for (int x = 0; x < (max * 2) + 1; x++) {
                    for (int y = 0; y < (max * 2) + 1; y++) {
                        for (int z = 0; z < (max * 2) + 1; z++) {
                            idx_x = x + offset;
                            idx_y = y + offset;
                            idx_z = z + offset;

                            offset_x = par_x + idx_x - 15;
                            offset_y = par_y + idx_y - 15;
                            offset_z = par_x + idx_z - 15;

                            int idx = idx_x * 31 * 31 + idx_y * 31 + idx_z;

                            Block block = world.getBlock(offset_x,offset_y, offset_z);
                            if (block == Blocks.air || block == null) continue;

                            int brightness = block.getLightValue(world, offset_x,offset_y, offset_z);
                            int opacity = block.getLightOpacity(world, offset_x,offset_y, offset_z);
                            if (opacity < 1) {
                                opacity = 1;
                            }

                            world.lightAdditionBlockList[idx] = opacity << 32 | brightness;
                        }
                    }
                }
                //:P - Heavily Inspired by: Player, creator of  (Thank you so much for the idea)

                while (max > 0) {
                    CLWorldHelper.iterateSpread(world, max, offset);
                    max--;
                    offset++;
                }

                max = (int) compLightValue & 0xF;
                offset = 15 - max;

                for (int x = 0; x < (max * 2) + 1; x++) {
                    for (int y = 0; y < (max * 2) + 1; y++) {
                        for (int z = 0; z < (max * 2) + 1; z++) {
                            idx_x = x + offset;
                            idx_y = y + offset;
                            idx_z = z + offset;

                            offset_x = par_x + idx_x - 15;
                            offset_y = par_y + idx_y - 15;
                            offset_z = par_x + idx_z - 15;

                            world.setLightValue(par1Enu, offset_x,offset_y, offset_z, (int)(world.lightAdditionBlockList[idx_x * 31 * 31 + idx_y * 31 + idx_z] & 0xFFFF));
                        }
                    }
                }
            }



            world.theProfiler.endStartSection("lightSubtraction");

            if ((((0x100000 | compLightValue) - savedLightValue) & 0x84210) > 0) { //savedLightValue has components that are larger than compLightValue
                // degrade

                world.theProfiler.endStartSection("lightBackfill");

                //Backfill

            }
            world.theProfiler.endSection();
            return true;
        }
    }

    private static void iterateSpread(World world, int max, int offset) {
        /* These occur in the same order as net.minecraft.util.Facing */
        long sideLightValues[] = {0,0,0,0,0,0};

        int idx_x;
        int idx_y;
        int idx_z;

        int neighbor_x;
        int neighbor_y;
        int neighbor_z;

        for (int x = 0; x < (max * 2) + 1; x++) {
            for (int y = 0; y < (max * 2) + 1; y++) {
                for (int z = 0; z < (max * 2) + 1; z++) {
                    idx_x = x + offset;
                    idx_y = y + offset;
                    idx_z = z + offset;

                    for (int sideIdx = 0; sideIdx < 6; ++sideIdx) {
                        neighbor_x = idx_x + Facing.offsetsXForSide[sideIdx];
                        neighbor_y = idx_y + Facing.offsetsYForSide[sideIdx];
                        neighbor_z = idx_z + Facing.offsetsZForSide[sideIdx];
                        if(neighbor_x > 0 && neighbor_y > 0 && neighbor_z > 0 && neighbor_x <= max && neighbor_y <= max && neighbor_z <= max) {
                            sideLightValues[sideIdx] = world.lightAdditionBlockList[neighbor_x * 31 * 31 + neighbor_y * 31 + neighbor_z];
                        }
                    }

                    world.lightAdditionBlockList[idx_x * 31 * 31 + idx_y * 31 + idx_z] = CLWorldHelper.pickLargestMinusOne(sideLightValues);
                }
            }
        }

    }

    private static long pickLargestMinusOne(long[] a) {
        long ret = 0;
        long ll;
        long rl;
        long gl;
        long bl;

        for (int i = 0; i < 6; i++) {
            if (a[i] == 0) continue;

            long opacity = (a[i] >> 32);
            if (opacity >= 15) continue;

            ll = Math.max((a[i] & 0x0000F) - (opacity), 0);
            if (ll > (ret&0x0000F)) {
                ret &= ~0x0000F;
                ret |= ll;
            }

            //TODO:
            //if (skylight) continue;

            rl = Math.max((a[i] & 0x001E0) - (opacity << 5), 0);
            if (rl > (ret&0x001E0)) {
                ret &= ~0x001E0;
                ret |= rl;
            }

            gl = Math.max((a[i] & 0x03C00) - (opacity << 10), 0);
            if (gl > (ret&0x03C00)) {
                ret &= ~0x03C00;
                ret |= gl;
            }

            bl = Math.max((a[i] & 0x78000) - (opacity << 15), 0);
            if (bl > (ret&0x78000)) {
                ret &= ~0x78000;
                ret |= bl;
            }
        }

        return ret;
    }


    //TODO: Remove nop()
    private static void nop() {
        return;
    }
}

