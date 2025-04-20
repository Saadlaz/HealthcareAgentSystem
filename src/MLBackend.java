import java.util.Random;

public class MLBackend {
    public static String processSymptoms(String symptoms) {
        return new Random().nextBoolean() ? "High" : "Low";
    }
}