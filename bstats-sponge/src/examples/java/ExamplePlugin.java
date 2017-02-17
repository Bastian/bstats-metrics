import com.google.inject.Inject;
import org.bstats.Metrics;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "exampleplugin", name = "ExamplePlugin", version = "1.0")
public class ExamplePlugin {

    @Inject
    private Metrics metrics;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        // No need to call a constructor or any method.
        // The metrics field gets initialised using @Inject :)
        // Check out https://docs.spongepowered.org/master/en/plugin/injection.html if you don't know what @Inject does

        // Optional: Add custom charts
        metrics.addCustomChart(new Metrics.SimplePie("chart_id") {
            @Override
            public String getValue() {
                return "My value";
            }
        });
    }

}