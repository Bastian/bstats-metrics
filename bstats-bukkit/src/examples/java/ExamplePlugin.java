import org.bstats.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // All you have to do is adding this line in your onEnable method:
        Metrics metrics = new Metrics(this);

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id") {
            @Override
            public String getValue() {
                return "My value";
            }
        });
    }

}