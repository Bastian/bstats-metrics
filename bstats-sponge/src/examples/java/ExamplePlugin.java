import com.google.inject.Inject;
import org.bstats.sponge.Metrics2;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "exampleplugin", name = "ExamplePlugin", version = "1.0")
public class ExamplePlugin {

    // No need to call a constructor or any other method.
    // The metrics field gets initialised using @Inject :)
    // Check out https://docs.spongepowered.org/master/en/plugin/injection.html if you don't know what @Inject does
    @Inject
    private Metrics2 metrics;

    // Optional: Add custom charts
    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        metrics.addCustomChart(new Metrics.SimplePie("chart_id", () -> "My value"));
    }

}
