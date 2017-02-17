import net.md_5.bungee.api.plugin.Plugin;
import org.bstats.Metrics;

public class ExamplePlugin extends Plugin {

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