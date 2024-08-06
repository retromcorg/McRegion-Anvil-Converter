package net.minecraft.world.level.storage;

/**
 * Copyright Mojang AB.
 * 
 * Don't do evil.
 */

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.NbtIo;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.storage.OldChunkStorage;
import net.minecraft.world.level.chunk.storage.OldChunkStorage.OldLevelChunk;
import net.minecraft.world.level.chunk.storage.RegionFile;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AnvilLevelStorageSource {

    private File baseDir;

    public AnvilLevelStorageSource(File dir) {
        baseDir = dir;
    }

    public boolean isConvertible(String levelId) {

        // check if there is old file format level data
        CompoundTag levelData = getDataTagFor(levelId);
        if (levelData == null || levelData.getInt("version") != AnvilLevelStorage.MCREGION_VERSION_ID) {
            return false;
        }

        return true;
    }

    private CompoundTag getDataTagFor(String levelId) {
        File dir = new File(baseDir, levelId);
        if (!dir.exists()) return null;

        File dataFile = new File(dir, "level.dat");
        if (dataFile.exists()) {
            try {
                CompoundTag root = NbtIo.readCompressed(new FileInputStream(dataFile));
                CompoundTag tag = root.getCompound("Data");
                return tag;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        dataFile = new File(dir, "level.dat_old");
        if (dataFile.exists()) {
            try {
                CompoundTag root = NbtIo.readCompressed(new FileInputStream(dataFile));
                CompoundTag tag = root.getCompound("Data");
                return tag;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void saveDataTag(String levelId, CompoundTag dataTag) {
        File dir = new File(baseDir, levelId);
        if (!dir.exists()) return;

        File dataFile = new File(dir, "level.dat");
        if (dataFile.exists()) {
            try {
                CompoundTag root = new CompoundTag();
                root.put("Data", dataTag);

                NbtIo.writeCompressed(root, new FileOutputStream(dataFile));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean convertLevel(String levelId, ProgressListener progress, int threadCount) {

        progress.progressStagePercentage(0);

        ArrayList<File> normalRegions = new ArrayList<File>();
        ArrayList<File> netherRegions = new ArrayList<File>();
        ArrayList<File> enderRegions = new ArrayList<File>();
//
        File baseFolder = new File(baseDir, levelId);
        File netherFolder = new File(baseFolder, LevelStorage.NETHER_FOLDER);
        File enderFolder = new File(baseFolder, LevelStorage.ENDER_FOLDER);

        System.out.println("Scanning folders...");

        // find normal world
        addRegionFiles(baseFolder, normalRegions);

        // find hell world
        if (netherFolder.exists()) {
            addRegionFiles(netherFolder, netherRegions);
        }
        if (enderFolder.exists()) {
            addRegionFiles(enderFolder, enderRegions);
        }

        int totalCount = normalRegions.size() + netherRegions.size() + enderRegions.size();
        System.out.println("Total conversion count is " + totalCount);

        CompoundTag levelData = getDataTagFor(levelId);

        //int threadCount = Runtime.getRuntime().availableProcessors();

        // Print in what mode we are running
        if (threadCount == 0) {
            System.out.println("Running sequentially");
        } else {
            System.out.println("Running " + threadCount + " threads in parallel");
        }

        AtomicInteger currentCount = new AtomicInteger(0);

        // convert normal world
        convertRegions(new File(baseFolder, "region"), normalRegions, null, currentCount, totalCount, progress, threadCount);
        // convert hell world
        convertRegions(new File(netherFolder, "region"), netherRegions, null, currentCount, totalCount, progress, threadCount);
        // convert end world
        convertRegions(new File(enderFolder, "region"), enderRegions, null, currentCount, totalCount, progress, threadCount);

        makeMcrLevelDatBackup(levelId);

        levelData.putInt("version", AnvilLevelStorage.ANVIL_VERSION_ID);
        saveDataTag(levelId, levelData);

        return true;
    }

    private void makeMcrLevelDatBackup(String levelId) {
        File dir = new File(baseDir, levelId);
        if (!dir.exists()) {
            System.out.println("Warning: Unable to create level.dat_mcr backup");
            return;
        }

        File dataFile = new File(dir, "level.dat");
        if (!dataFile.exists()) {
            System.out.println("Warning: Unable to create level.dat_mcr backup");
            return;
        }

        File newName = new File(dir, "level.dat_mcr");
        if (!dataFile.renameTo(newName)) {
            System.out.println("Warning: Unable to create level.dat_mcr backup");
        }
    }

    private void convertRegions(File baseFolder, ArrayList<File> regionFiles, BiomeSource biomeSource, AtomicInteger currentCount, int totalCount, ProgressListener progress, int numThreads) {
        // Create an ExecutorService with a fixed thread pool

        if (numThreads <= 0) {
            //Program is being run sequentially
            for (File regionFile : regionFiles) {
                convertRegion(baseFolder, regionFile, biomeSource, currentCount, totalCount, progress);
            }
        } else {
            //Program is being run in parallel
            ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

            // Submit tasks for each region file
            for (File regionFile : regionFiles) {
                executorService.submit(() -> {
                    convertRegion(baseFolder, regionFile, biomeSource, currentCount, totalCount, progress);
                });
            }

            // Shut down the executor service and wait for all tasks to complete
            executorService.shutdown();
            try {
                executorService.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Update progress after all tasks are completed
        int percent = (int) Math.round(100.0d * (double) totalCount / (double) totalCount);
        progress.progressStagePercentage(percent);
    }

    private void convertRegion(File baseFolder, File regionFile, BiomeSource biomeSource, AtomicInteger currentCounter, int totalCount, ProgressListener progress) {

        try {
            String name = regionFile.getName();

            RegionFile regionSource = new RegionFile(regionFile);
            RegionFile regionDest = new RegionFile(new File(baseFolder, name.substring(0, name.length() - RegionFile.MCREGION_EXTENSION.length()) + RegionFile.ANVIL_EXTENSION));

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (regionSource.hasChunk(x, z) && !regionDest.hasChunk(x, z)) {
                        DataInputStream regionChunkInputStream = regionSource.getChunkDataInputStream(x, z);
                        if (regionChunkInputStream == null) {
                            System.out.println("Failed to fetch input stream");
                            continue;
                        }
                        CompoundTag chunkData = NbtIo.read(regionChunkInputStream);
                        regionChunkInputStream.close();

                        CompoundTag compound = chunkData.getCompound("Level");
                        {
                            OldLevelChunk oldChunk = OldChunkStorage.load(compound);

                            CompoundTag tag = new CompoundTag();
                            CompoundTag levelData = new CompoundTag();
                            tag.put("Level", levelData);
                            OldChunkStorage.convertToAnvilFormat(oldChunk, levelData, biomeSource);

                            DataOutputStream chunkDataOutputStream = regionDest.getChunkDataOutputStream(x, z);
                            NbtIo.write(tag, chunkDataOutputStream);
                            chunkDataOutputStream.close();
                        }
                    }
                }

                // Update progress
                int currentCount = currentCounter.get();

                int basePercent = (int) Math.round(100.0d * (double) (currentCount * 1024) / (double) (totalCount * 1024));
                int newPercent = (int) Math.round(100.0d * (double) ((x + 1) * 32 + currentCount * 1024) / (double) (totalCount * 1024));
                if (newPercent > basePercent) {
                    progress.progressStagePercentage(newPercent);
                }
            }

            // Increment the counter
            currentCounter.incrementAndGet();

            regionSource.close();
            regionDest.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addRegionFiles(File baseFolder, ArrayList<File> regionFiles) {

        File regionFolder = new File(baseFolder, "region");
        File[] list = regionFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(RegionFile.MCREGION_EXTENSION);
            }
        });

        if (list != null) {
            for (File file : list) {
                regionFiles.add(file);
            }
        }
    }

}
