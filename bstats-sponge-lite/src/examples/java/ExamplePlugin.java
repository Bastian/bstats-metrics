import com.google.inject.Inject;
import org.bstats.sponge.MetricsLite2;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "exampleplugin", name = "ExamplePlugin", version = "1.0")
public class ExamplePlugin {

    // The metricsFactory parameter gets injected using @Inject :)
    // Check out https://docs.spongepowered.org/master/en/plugin/injection.html if you don't know what @Inject does
    @Inject
    public ExamplePlugin(MetricsLite2.Factory metricsFactory) {
        // You can find the plugin ids of your plugins on the page https://bstats.org/what-is-my-plugin-id
        int pluginId = 1234; // <-- Replace with the id of your plugin!
        metricsFactory.make(pluginId);
    }

}
