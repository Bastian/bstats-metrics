import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.bungeecord.Metrics;

import java.util.concurrent.Callable;

public class ExamplePlugin extends Plugin {

    @Override
    public void onEnable() {
        // All you have to do is adding this line in your onEnable method:
        Metrics metrics = new Metrics(this);

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
    }

}