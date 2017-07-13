import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.util.concurrent.Callable;

public class ExamplePlugin extends Plugin {

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