package pocker;

import metrics.SQDIFF;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class App {


    private static Map<String, int[][]> loadPatterns(Path folder, String glob) {
        Map<String, int[][]> result = new HashMap<>();
        try {
            var stream = Files.newDirectoryStream(folder, glob);
            for (var path : stream) {
                var image = loadImage(path.toString());
                var pixels = convertTo2D(image);
                var label = FilenameUtils.getBaseName(path.getFileName().toString());
                result.put(label, pixels);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static List<String> splitCards(String baseName) {
        int index = 0;
        List<String> cardNames = new ArrayList<>();

        while (index < baseName.length()) {
            int step = 2;
            if (baseName.charAt(index) == '1') {
                step = 3;
            }
            cardNames.add(baseName.substring(index, index += step));
        }
        return cardNames;
    }

    public static BufferedImage loadImage(String filePath) {
        try (InputStream stream = new FileInputStream(filePath)) {
            BufferedImage image = ImageIO.read(stream);
            return image;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws Exception {
        Options options = getOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            boolean debug = cmd.hasOption("d");
            var default_resource_path = App.class.getClassLoader().getResource("patterns");
            var dataPath = Path.of(cmd.getOptionValue("p"));
            var homePath = cmd.getOptionValue("c", default_resource_path.getPath());
            var rankPath = Paths.get(homePath, "rank");
            var suitPath = Paths.get(homePath, "suit");

            var rankPatterns = loadPatterns(rankPath, "*.jpg");
            var suitPatterns = loadPatterns(suitPath, "*.jpg");
            Recognizer estimator = new Recognizer(suitPatterns, rankPatterns, new SQDIFF());

            List<String> target = new LinkedList<>();
            List<String> predictionList = new LinkedList<>();


            DirectoryStream<Path> stream = null;
            int amount_of_files = 0;
            stream = Files.newDirectoryStream(dataPath, "*.png");

            var start = System.currentTimeMillis();
            for (var path : stream) {
                amount_of_files++;
                var image = loadImage(path.toAbsolutePath().toString());
                var result = estimator.recognize(image);
                if (debug) {
                    var cardLabels = splitCards(FilenameUtils.getBaseName(path.getFileName().toString()));
                    for (int i = 0; i < cardLabels.size(); i++) {
                        target.add(cardLabels.get(i));
                        if (i < result.size()) {
                            predictionList.add(result.get(i));
                        } else {
                            predictionList.add("NaN");
                        }
                    }
                }
                System.out.println(path.getFileName().toString() + " - " + String.join("", result));
            }

            if (debug) {
                System.out.println("Time per screen: " + (System.currentTimeMillis() - start) / amount_of_files + " mc");
                if (target.equals(predictionList)) {
                    System.out.println("F1: 1.0");
                } else {
                    System.out.println("Not equal");
                }
            }

        } catch (MissingOptionException | UnrecognizedOptionException e) {
            System.out.println(e.getMessage());
            System.out.println("See usage");
            printUsage(options);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static Options getOptions() {
        Options options = new Options();
        Option opt = new Option("p", "path", true, "Path to the folder with pictures");
        opt.setRequired(true);
        options.addOption(opt);

        opt = new Option("c", "content", true, "Path to the folder with patterns");
        options.addOption(opt);

        opt = new Option("d", "debug", false, "Debug mode");
        options.addOption(opt);
        return options;
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("app", options);
    }

    private static int[][] convertTo2D(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[][] result = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                result[row][col] = image.getRaster().getSample(col, row, 0);
            }
        }

        return result;
    }

}
