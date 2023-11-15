package pocker;

import metrics.MistakeEstimator;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.stream.IntStream;

public class Recognizer {

    private final int CARD_SIZE_X = 60;
    private final int CARD_SIZE_Y = 80;

    private final int SUIT_SHIFT_X = 19;
    private final int SUIT_SHIFT_Y = 43;

    private final int[][] cardPositions = {
            {147, 590},
            {219, 590},
            {290, 590},
            {362, 590},
            {434, 590}
    };

    private final Map<String, int[][]> suitPatterns;
    private final Map<String, int[][]> rankPatterns;
    private final MistakeEstimator estimator;


    public Recognizer(Map<String, int[][]> suitPatterns, Map<String, int[][]> rankPatterns, MistakeEstimator estimator) {
        this.suitPatterns = suitPatterns;
        this.rankPatterns = rankPatterns;
        this.estimator = estimator;
    }

    public static int calculateSumOfElements(int[][] matrix) {

        // Use Arrays.stream and flatMap to convert the 2D matrix into a 1D stream of elements
        return Arrays.stream(matrix)
                .flatMapToInt(IntStream::of)
                .sum();
    }


    public List<String> recognize(BufferedImage image) {
        var cardList = findCards(image);
        List<String> result = new LinkedList<>();

        for (int[][] card : cardList) {
            var suit = recognizeCard(getSubMatrix(card, SUIT_SHIFT_Y, card.length, SUIT_SHIFT_X, card[0].length), suitPatterns);
            var rank = recognizeCard(getSubMatrix(card, 0, 50, 0, 40), rankPatterns);
            result.add(rank + suit);
        }
        return result;
    }

    private int[][] getSubMatrix(int[][] matrix, int stRow, int fhRow, int stCol, int fhCol) {
        int[][] subMatrix = new int[fhRow - stRow][fhCol - stCol];
        for (int i = stRow; i < fhRow; i++) {
            for (int j = stCol; j < fhCol; j++) {
                subMatrix[i - stRow][j - stCol] = matrix[i][j];
            }

        }
        return subMatrix;
    }

    public int[][] rgb2gray(BufferedImage image, boolean inverse, int threshold) {
        int[][] result = new int[image.getHeight()][image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                var pixel = image.getRGB(x, y);
                var grey = ((pixel & 16711680) >> 16) * 0.2989 + ((pixel & 65280) >> 8) * 0.5870 + (pixel & 255) * 0.1140;

                if (inverse) {
                    result[y][x] = (grey < threshold) ? 255 : 0;
                } else {
                    result[y][x] = (grey < threshold) ? 0 : 255;
                }
            }
        }
        return result;
    }

    private List<int[][]> findCards(BufferedImage image) {
        List<int[][]> cards = new ArrayList<>();
        for (int card = 0; card < cardPositions.length; card++) {
            BufferedImage cardImage = image.getSubimage(cardPositions[card][0], cardPositions[card][1], CARD_SIZE_X, CARD_SIZE_Y);
            var greyCard = rgb2gray(cardImage, true, 150);
            if (calculateSumOfElements(greyCard) != greyCard.length * greyCard[0].length * 255) {
                cards.add(greyCard);
            } else {
                greyCard = rgb2gray(cardImage, true, 100);
                if (calculateSumOfElements(greyCard) != greyCard.length * greyCard[0].length * 255) {
                    cards.add(greyCard);
                }
            }

        }
        return cards;
    }


    private double matchTemplate(int[][] image, int[][] pattern) {
        double bestError = -1;
        for (int y = 0; y < image.length - pattern.length; y++) {
            for (int x = 0; x < image[y].length - pattern[y].length; x++) {
                var subImage = getSubMatrix(image, y, y + pattern.length, x, x + pattern[y].length);
                var error = estimator.run(subImage, pattern);
                if (bestError < 0 || estimator.compare(bestError, error) > 0) {
                    bestError = error;
                }
            }
        }
        return bestError;
    }


    private String recognizeCard(int[][] image, Map<String, int[][]> patterns) {
        double bestError = -1;
        String label = "";
        for (String key : patterns.keySet()) {
            var error = matchTemplate(image, patterns.get(key));
            if (bestError < 0 || estimator.compare(bestError, error) > 0) {
                bestError = error;
                label = key;
            }
        }
        return label;
    }
}