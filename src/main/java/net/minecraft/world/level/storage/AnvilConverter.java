package net.minecraft.world.level.storage;

/**
 * Copyright Mojang AB.
 * 
 * Don't do evil.
 */


import java.io.File;
import java.util.concurrent.TimeUnit;

public class AnvilConverter {

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsageAndExit();
        }

        // Handle optional third argument for number of threads
        int numThreads = 1;
        if (args.length == 3) {
            try {
                numThreads = Integer.parseInt(args[2]);
                if (numThreads < 0) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid number of threads: " + args[2]);
                System.out.println("");
                printUsageAndExit();
                return;
            }
        }

        File baseFolder;
        try {
            baseFolder = new File(args[0]);
            if (!baseFolder.exists()) {
                throw new RuntimeException(args[0] + " doesn't exist");
            } else if (!baseFolder.isDirectory()) {
                throw new RuntimeException(args[0] + " is not a folder");
            }
        } catch (Exception e) {
            System.err.println("Base folder problem: " + e.getMessage());
            System.out.println("");
            printUsageAndExit();
            return;
        }

        AnvilLevelStorageSource storage = new AnvilLevelStorageSource(baseFolder);
        if (!storage.isConvertible(args[1])) {
            System.err.println("World called " + args[1] + " is not convertible to the Anvil format");
            System.out.println("");
            printUsageAndExit();
            return;
        }

        // Remove old .mca region files if they exist (they will be regenerated)
        File regionFolder = new File(baseFolder, args[1] + "/region");
        int regionCount = 0;
        if (regionFolder.exists()) {
            File[] regionFiles = regionFolder.listFiles();
            if (regionFiles != null) {
                for (File regionFile : regionFiles) {
                    if (regionFile.getName().endsWith(".mca")) {
                        regionFile.delete();
                        regionCount++;
                    }
                }
            }
        }
        if (regionCount > 0) {
            System.out.println("Deleted " + regionCount + " old mca region files");
        }

        // Rename level.dat_mcr to level.dat
        File levelDatMcr = new File(baseFolder, args[1] + "/level.dat_mcr");
        File levelDat = new File(baseFolder, args[1] + "/level.dat");
        if (levelDatMcr.exists()) {
            if (levelDat.exists()) {
                levelDat.delete();
            }
            levelDatMcr.renameTo(levelDat);
            System.out.println("Renamed level.dat_mcr to level.dat");
        }

        System.out.println("Converting map!");

        // Duration duration = new Duration();
        long startTime = System.currentTimeMillis();

        storage.convertLevel(args[1], new ProgressListener() {
            private long timeStamp = System.currentTimeMillis();

            public void progressStartNoAbort(String string) {
            }

            public void progressStart(String string) {
            }

            public void progressStagePercentage(int i) {
                if ((System.currentTimeMillis() - timeStamp) >= 1000L) {
                    timeStamp = System.currentTimeMillis();
                    System.out.println("Converting... " + i + "%");
                }
            }

            public void progressStage(String string) {
            }
        }, numThreads);

        long endTime = System.currentTimeMillis();
        long durationMillis = endTime - startTime;

        // Calculate minutes, seconds, and milliseconds
        long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) -
                TimeUnit.HOURS.toMinutes(hours);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) -
                TimeUnit.MINUTES.toSeconds(minutes);
        long millis = durationMillis - TimeUnit.MINUTES.toMillis(minutes) -
                TimeUnit.SECONDS.toMillis(seconds);

        // Format and print the duration time
        String duration = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
        System.out.println("Conversion completed in: " + duration);


        System.out.println("Done!");
        System.out.println("To revert, replace level.dat with level.dat_mcr. Old mcr region files have not been modified.");
    }

    private static void printUsageAndExit() {
        System.out.println("Map converter for Minecraft, from format \"McRegion\" to \"Anvil\". (c) Mojang AB 2012");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\tjava -jar AnvilConverter.jar <base folder> <world name>");
        System.out.println("Where:");
        System.out.println("\t<base folder>\tThe full path to the folder containing Minecraft world folders");
        System.out.println("\t<world name>\tThe folder name of the Minecraft world to be converted");
        System.out.println("Example:");
        System.out.println("\tjava -jar AnvilConverter.jar /home/jeb_/minecraft world");
        System.exit(1);
    }

}
