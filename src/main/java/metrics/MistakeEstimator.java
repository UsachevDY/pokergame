package metrics;

public interface MistakeEstimator {

    double run(int[][] image, int[][] pattern);

    int compare(double current, double next);
}
