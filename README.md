# Minecraft Java McRegion to Anvil Converter Map Converter

This tool converts Minecraft Java Edition McRegion (Beta 1.3 - 1.2.5) worlds to Anvil format.

The project is based on
the [McRegion to Anvil Converter](https://web.archive.org/web/20120302221152/https://www.mojang.com/2012/02/new-minecraft-map-format-anvil/)
created by Jens Bergensten at Mojang in 2012. It has been modified to support parallel processing (multithreading) to
speed up the conversion process.

## About

This fork was created by RhysB for use on [RetroMC](https://retromc.org/) to facilitate routine conversion of McRegion
worlds to Anvil format,
enabling modern tools like Overviewer to render the map.

## Performance Improvements

Performance improvements observed when converting RetroMC (approximately 30gb) from McRegion to Anvil format on a AMD
Ryzen 9 5950X:

| Cores           | Time (s) | Speedup |
|-----------------|----------|---------|
| Best Sequential | 5976     | N/A     |
| 1               | 6156     | 0.97    |
| 2               | 2976     | 2.01    |
| 4               | 1497     | 3.99    |
| 8               | 774      | 7.72    |
| 16              | 441      | 13.55   |
| 32              | 318      | 18.79   |

### Analysis

The performance data demonstrates significant improvements when increasing the number of threads for the conversion process. Key observations include:

- **Sequential vs. Single Thread**: The single-threaded conversion process is slower than the original sequential process. This is likely the result of additional overhead introduced by the multithreading implementation.
   - **ExecutorService Optimization**: The ExecutorService may incur some overhead even with a single thread, leading to a slight performance decrease.

- **Overheads and Limits**: Despite the impressive scaling, the speedup is slightly less than the ideal linear speedup at higher thread counts. Potential reasons include:
   - **I/O Bottlenecks**: Disk I/O operations might become a limiting factor, preventing perfect linear scaling.
   - **CPU Saturation**: As the CPU approaches full utilization, additional threads contribute less to overall performance improvement.

## Usage

1. Download the latest release from the [releases page](#).

2. Run the converter with the following command:
    ```bash
    java -jar AnvilConverter.jar <base folder> <world name> [thread count]
    ```
    - `<base folder>`: The full path to the folder containing Minecraft world folders.
    - `<world name>`: The folder name of the Minecraft world to be converted.
    - `[thread count]` (Optional): Number of threads to use for conversion. Defaults to 1 if not specified.

3. Examples:
    - Convert world using 1 thread:
      ```bash
      java -jar AnvilConverter.jar /home/jeb_/minecraft world
      ```
    - Convert world using 4 threads:
      ```bash
      java -jar AnvilConverter.jar /home/jeb_/minecraft world 4
      ```

## Example Worlds/Datasets

Example worlds and datasets, such as historical RetroMC and BetaLands world downloads, can be
found on [archive.johnymuffin.com](https://archive.johnymuffin.com/).

## License

Modifications made to this project are licensed under the MIT License. See [LICENSE](LICENSE) for more information.

The original McRegion to Anvil Converter was created by Jens Bergensten at Mojang and includes the following notice:

```/**
 * Copyright Mojang AB.
 * 
 * Don't do evil.
 */
```
