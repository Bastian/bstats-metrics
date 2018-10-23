import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // All you have to do is adding this line in your onEnable method:
        Metrics metrics = new Metrics(this);

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
    }

}