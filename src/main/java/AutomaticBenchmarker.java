import net.minecraft.world.level.storage.AnvilConverter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.*;

public class AutomaticBenchmarker {

    private static final Logger logger = Logger.getLogger(AutomaticBenchmarker.class.getName());

    public static void main(String[] args) {
        setupLogger();

        // Notice: This program performs automatic benchmarking of the AnvilConverter application.
        logger.info("Starting automatic benchmarking of the AnvilConverter application.");

        // Fake arguments for the AnvilConverter application
        String baseFolderPath = "E:\\RetroMC-1\\";
        String worldName = "retromc";
        int[] threadCounts = {32, 16, 8, 4, 2, 1, 0}; // 0 runs the converter sequentially on the main thread

        for (int numThreads : threadCounts) {
            logger.info("Running benchmark with " + numThreads + " threads.");
            AnvilConverter.main(new String[]{baseFolderPath, worldName, String.valueOf(numThreads)});

            // Cleanup JVM
            System.gc();

            // Sleep for 1 minute to allow the JVM to cleanup
            try {
                Thread.sleep(60000);
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, "Thread interrupted while sleeping", e);
            }
        }
    }

    private static void setupLogger() {
        try {
            // Create a file handler that writes log record to a file called benchmark.log
            FileHandler fileHandler = new FileHandler("benchmark.log", true);
            fileHandler.setFormatter(new SimpleFormatter());

            // Add the file handler to the logger
            logger.addHandler(fileHandler);

            // Set the logger level to INFO
            logger.setLevel(Level.INFO);

            // Also log to the console
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(consoleHandler);

            // Redirect System.out and System.err to logger
            PrintStream logStream = new PrintStream(new DualOutputStream(System.out, new FileOutputStream("benchmark.log", true)));
            System.setOut(logStream);
            System.setErr(logStream);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to setup logger", e);
        }
    }
}
