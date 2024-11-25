package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    // private static final int MIN = 0;
    // private static final int MAX = 100;
    // private static final int ATTEMPTS = 10;

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *              the views to attach
     */
    public DrawNumberApp(final String configFileName, final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view : views) {
            view.setObserver(this);
            view.start();
        }

        final Configuration.Builder builder = new Configuration.Builder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResourceAsStream(configFileName)))) {
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(":");

                if (parts.length == 2) {
                    String value = parts[1].trim();

                    switch (parts[0]) {
                        case "minimum":
                            builder.setMin(Integer.parseInt(value));
                            break;
                        case "maximum":
                            builder.setMax(Integer.parseInt(value));
                            break;
                        case "attempts":
                            builder.setAttempts(Integer.parseInt(value));
                            break;
                        default:
                            break; // future settings in case they need to be parsed differently
                    }
                } else {
                    displayError("Malformed setting, unable to parse: " + line);
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            displayError(e.getMessage());
        }

        final Configuration config = builder.build();
        this.model = new DrawNumberImpl(config.getMin(), config.getMax(), config.getAttempts());
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view : views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view : views) {
                view.numberIncorrect();
            }
        }
    }

    private void displayError(final String err) {
        for (final DrawNumberView view : views) {
            view.displayError(err);
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *             ignored
     * @throws FileNotFoundException
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(
                "config.yml",
                new DrawNumberViewImpl(),
                new DrawNumberViewImpl(),
                new PrintStreamView(System.out),
                new PrintStreamView("output.log"));
    }

}
