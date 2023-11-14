package metrics;


public class SQDIFF implements MistakeEstimator {
    public double run(int[][] image, int[][] pattern) {
        int sum = 0;
        for (int i = 0; i < image.length; i++) {
            for (int j = 0; j < image[i].length; j++) {
                sum += Math.abs(pattern[i][j] - image[i][j]);
            }
        }
        return sum / 255.0;
    }

    public int compare(double current, double next) {
        return current > next ? 1 : -1;
    }
}
