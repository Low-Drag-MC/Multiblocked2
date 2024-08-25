//package com.lowdragmc.mbd2.common.gui.editor.multiblock;
//
//import com.lowdragmc.mbd2.api.pattern.BlockPattern;
//import com.lowdragmc.mbd2.api.pattern.Predicates;
//import com.lowdragmc.mbd2.api.pattern.TraceabilityPredicate;
//import com.lowdragmc.mbd2.api.pattern.predicates.PredicateBlocks;
//import com.lowdragmc.mbd2.api.pattern.predicates.SimplePredicate;
//import com.lowdragmc.mbd2.api.pattern.util.RelativeDirection;
//import net.minecraft.core.BlockPos;
//import net.minecraft.core.Direction;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.Block;
//import net.minecraft.world.level.block.Blocks;
//
//import java.lang.reflect.Array;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Map;
//import java.util.Set;
//
//public class EditableBlockPattern {
//    public RelativeDirection[] structureDir;
//    public String[][] pattern;
//    public int[][] aisleRepetitions;
//    public Map<String, SimplePredicate> predicates; // 0-any, 1-air, 2-controller
//    public Map<Character, Set<String>> symbolMap;
//
//    public EditableBlockPattern() {
//        predicates = new HashMap<>();
//        symbolMap = new HashMap<>();
//        structureDir = new RelativeDirection[] {RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT};
//        predicates.put("any", SimplePredicate.ANY);
//        predicates.put("air", SimplePredicate.AIR);
//        symbolMap.computeIfAbsent(' ', key -> new HashSet<>()).add("any"); // any
//        symbolMap.computeIfAbsent('-', key -> new HashSet<>()).add("air"); // air
//        symbolMap.computeIfAbsent('@', key -> new HashSet<>()).add("controller"); // controller
//    }
//
//    public EditableBlockPattern(Level world, ResourceLocation location, BlockPos controllerPos, Direction facing, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
//        this();
//        pattern = new String[1 + maxX - minX][ 1 + maxY - minY];
//        if (facing == Direction.WEST) {
//            structureDir = new RelativeDirection[] {RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.BACK};
//        } else if (facing == Direction.EAST) {
//            structureDir = new RelativeDirection[] {RelativeDirection.RIGHT, RelativeDirection.UP, RelativeDirection.FRONT};
//        } else if (facing == Direction.NORTH) {
//            structureDir = new RelativeDirection[] {RelativeDirection.BACK, RelativeDirection.UP, RelativeDirection.RIGHT};
//        } else if (facing == Direction.SOUTH) {
//            structureDir = new RelativeDirection[] {RelativeDirection.FRONT, RelativeDirection.UP, RelativeDirection.LEFT};
//        }
//        aisleRepetitions = new int[pattern.length][2];
//        for (int[] aisleRepetition : aisleRepetitions) {
//            aisleRepetition[0] = 1;
//            aisleRepetition[1] = 1;
//        }
//
//        predicates.put("controller", new PredicateComponent(location)); // controller
//
//        Map<Block, Character> map = new HashMap<>();
//        map.put(Blocks.AIR, ' ');
//
//        char c = 'A'; // auto
//
//        for (int x = minX; x <= maxX; x++) {
//            for (int y = minY; y <= maxY; y++) {
//                StringBuilder builder = new StringBuilder();
//                for (int z = minZ; z <= maxZ; z++) {
//                    BlockPos pos = new BlockPos(x, y, z);
//                    if (controllerPos.equals(pos)) {
//                        builder.append('@'); // controller
//                    } else {
//                        Block block = world.getBlockState(pos).getBlock();
//                        if (!map.containsKey(block)) {
//                            map.put(block, c);
//                            String name = String.valueOf(c);
//                            predicates.put(name, new PredicateBlocks(block));
//                            symbolMap.computeIfAbsent(c, key -> new HashSet<>()).add(name); // any
//                            c++;
//                        }
//                        builder.append(map.get(block));
//                    }
//                }
//                pattern[x - minX][y - minY] = builder.toString();
//            }
//        }
//    }
//
//    public void changeDir(RelativeDirection charDir, RelativeDirection stringDir, RelativeDirection aisleDir) {
//        if (charDir.isSameAxis(stringDir) || stringDir.isSameAxis(aisleDir) || aisleDir.isSameAxis(charDir)) return;
//        char[][][] newPattern = new char
//                [structureDir[0].isSameAxis(aisleDir) ? pattern[0][0].length() : structureDir[1].isSameAxis(aisleDir) ? pattern[0].length : pattern.length]
//                [structureDir[0].isSameAxis(stringDir) ? pattern[0][0].length() : structureDir[1].isSameAxis(stringDir) ? pattern[0].length : pattern.length]
//                [structureDir[0].isSameAxis(charDir) ? pattern[0][0].length() : structureDir[1].isSameAxis(charDir) ? pattern[0].length : pattern.length];
//        for (int i = 0; i < pattern.length; i++) {
//            for (int j = 0; j < pattern[0].length; j++) {
//                for (int k = 0; k < pattern[0][0].length(); k++) {
//                    char c = pattern[i][j].charAt(k);
//                    int x = 0, y = 0, z = 0;
//                    if (structureDir[2].isSameAxis(aisleDir)) {
//                        if (structureDir[2] == aisleDir) {
//                            x = i;
//                        } else {
//                            x = pattern.length - i - 1;
//                        }
//                    } else if (structureDir[2].isSameAxis(stringDir)) {
//                        if (structureDir[2] == stringDir) {
//                            y = i;
//                        } else {
//                            y = pattern.length - i - 1;
//                        }
//                    } else if (structureDir[2].isSameAxis(charDir)) {
//                        if (structureDir[2] == charDir) {
//                            z = i;
//                        } else {
//                            z = pattern.length - i - 1;
//                        }
//                    }
//
//                    if (structureDir[1].isSameAxis(aisleDir)) {
//                        if (structureDir[1] == aisleDir) {
//                            x = j;
//                        } else {
//                            x = pattern[0].length - j - 1;
//                        }
//                    } else if (structureDir[1].isSameAxis(stringDir)) {
//                        if (structureDir[1] == stringDir) {
//                            y = j;
//                        } else {
//                            y = pattern[0].length - j - 1;
//                        }
//                    } else if (structureDir[1].isSameAxis(charDir)) {
//                        if (structureDir[1] == charDir) {
//                            z = j;
//                        } else {
//                            z = pattern[0].length - j - 1;
//                        }
//                    }
//
//                    if (structureDir[0].isSameAxis(aisleDir)) {
//                        if (structureDir[0] == aisleDir) {
//                            x = k;
//                        } else {
//                            x = pattern[0][0].length() - k - 1;
//                        }
//                    } else if (structureDir[0].isSameAxis(stringDir)) {
//                        if (structureDir[0] == stringDir) {
//                            y = k;
//                        } else {
//                            y = pattern[0][0].length() - k - 1;
//                        }
//                    } else if (structureDir[0].isSameAxis(charDir)) {
//                        if (structureDir[0] == charDir) {
//                            z = k;
//                        } else {
//                            z = pattern[0][0].length() - k - 1;
//                        }
//                    }
//                    newPattern[x][y][z] = c;
//                }
//            }
//        }
//
//        pattern = new String[newPattern.length][newPattern[0].length];
//        for (int i = 0; i < pattern.length; i++) {
//            for (int j = 0; j < pattern[0].length; j++) {
//                StringBuilder builder = new StringBuilder();
//                for (char c : newPattern[i][j]) {
//                    builder.append(c);
//                }
//                pattern[i][j] = builder.toString();
//            }
//        }
//
//        aisleRepetitions = new int[pattern.length][2];
//        for (int[] aisleRepetition : aisleRepetitions) {
//            aisleRepetition[0] = 1;
//            aisleRepetition[1] = 1;
//        }
//
//        structureDir = new RelativeDirection[]{charDir, stringDir, aisleDir};
//    }
//
//    public BlockPattern build() {
//        int[] centerOffset = new int[5];
//        TraceabilityPredicate[][][] predicate = (TraceabilityPredicate[][][]) Array.newInstance(TraceabilityPredicate.class, this.pattern.length, this.pattern[0].length, this.pattern[0][0].length());
//        for (int i = 0, minZ = 0, maxZ = 0; i < this.pattern.length; minZ += aisleRepetitions[i][0], maxZ += aisleRepetitions[i][1], i++) {
//            for (int j = 0; j < this.pattern[0].length; j++) {
//                for (int k = 0; k < this.pattern[0][0].length(); k++) {
//                    Set<String> saves = symbolMap.get(this.pattern[i][j].charAt(k));
//                    if (saves == null || saves.isEmpty()) {
//                        predicate[i][j][k] = Predicates.any();
//                    } else {
//                        predicate[i][j][k] = new TraceabilityPredicate();
//                        for (String p : saves) {
//                            predicate[i][j][k] = predicate[i][j][k].or(new TraceabilityPredicate(predicates.get(p)));
//                            if (p.equals("controller")) {
//                                predicate[i][j][k].setController();
//                                centerOffset = new int[]{k, j, i, minZ, maxZ};
//                            }
//                        }
//                    }
//                }
//            }
//        }
//        return new BlockPattern(predicate, structureDir, aisleRepetitions, centerOffset);
//    }
//
//    public BlockPos getActualPosOffset(int x, int y, int z, Direction facing) {
//        int[] c0 = new int[]{x, y, z}, c1 = new int[3];
//        remapping(c0, c1, facing);
//        return new BlockPos(c1[0], c1[1], c1[2]);
//    }
//
//    public int[] getActualPatternOffset(BlockPos pos, Direction facing) {
//        int[] c0 = new int[]{pos.getX(), pos.getY(), pos.getZ()}, c1 = new int[3];
//        remapping(c0, c1, facing);
//        return c1;
//    }
//
//    public void remapping(int[] c0, int[] c1, Direction facing){
//        for (int i = 0; i < 3; i++) {
//            Direction realFacing = structureDir[i].getActualFacing(facing);
//            if (realFacing == Direction.UP) {
//                c1[1] = c0[i];
//            } else if (realFacing == Direction.DOWN) {
//                c1[1] = -c0[i];
//            } else if (realFacing == Direction.WEST) {
//                c1[0] = -c0[i];
//            } else if (realFacing == Direction.EAST) {
//                c1[0] = c0[i];
//            } else if (realFacing == Direction.NORTH) {
//                c1[2] = -c0[i];
//            } else if (realFacing == Direction.SOUTH) {
//                c1[2] = c0[i];
//            }
//        }
//    }
//
//
//    public void cleanUp() {
//        Set<Character> usedChar = new HashSet<>();
//        Set<String> usedPredicate = new HashSet<>();
//        for (String[] strings : pattern) {
//            for (String string : strings) {
//                for (char c : string.toCharArray()) {
//                    usedChar.add(c);
//                }
//            }
//        }
//        symbolMap.entrySet().removeIf(entry -> !usedChar.contains(entry.getKey()));
//        symbolMap.forEach((symbol, predicates) -> usedPredicate.addAll(predicates));
//        predicates.entrySet().removeIf(entry -> !usedPredicate.contains(entry.getKey()));
//    }
//
//    public int[] getCenterOffset() {
//        int[] centerOffset = new int[3];
//        for (int i = 0; i < pattern.length; i++) {
//            for (int j = 0; j < pattern[0].length; j++) {
//                for (int k = 0; k < pattern[0][0].length(); k++) {
//                    if (pattern[i][j].charAt(k) == '@') {
//                        centerOffset = new int[]{i, j, k};
//                        break;
//                    }
//                }
//            }
//        }
//        return centerOffset;
//    }
//
//    public EditableBlockPattern copy() {
//        var newPattern = new EditableBlockPattern();
//        System.arraycopy(this.structureDir, 0, newPattern.structureDir, 0, this.structureDir.length);
//
//        newPattern.pattern = new String[pattern.length][pattern[0].length];
//        for (int i = 0; i < pattern.length; i++) {
//            System.arraycopy(pattern[i], 0, newPattern.pattern[i], 0, pattern[i].length);
//        }
//
//        newPattern.aisleRepetitions = new int[aisleRepetitions.length][2];
//        for (int i = 0; i < aisleRepetitions.length; i++) {
//            System.arraycopy(aisleRepetitions[i], 0, newPattern.aisleRepetitions[i], 0, aisleRepetitions[i].length);
//        }
//
//        predicates.forEach((k, v) -> {
//            newPattern.predicates.put(k, Multiblocked.GSON.fromJson(Multiblocked.GSON.toJsonTree(v, SimplePredicate.class), SimplePredicate.class));
//            if (v instanceof PredicateComponent) {
//                ((PredicateComponent)newPattern.predicates.get(k)).definition = ((PredicateComponent) v).definition;
//            }
//        });
//        symbolMap.forEach((k, v) -> newPattern.symbolMap.put(k, new HashSet<>(v)));
//
//        return newPattern;
//    }
//}
