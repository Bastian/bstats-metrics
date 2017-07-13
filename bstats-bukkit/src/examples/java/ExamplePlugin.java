import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Callable;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // All you have to do is adding this line in your onEnable method:
        Metrics metrics = new Metrics(this);

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", new Callable<String>() {
            @Override
            public String call() throws Exception {
                return "My value";
            }
        }));

        // If you use Java 8 you can use Lambdas:
        // metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
    }

}