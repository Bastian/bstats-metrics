import com.google.inject.Inject;
import org.bstats.sponge.MetricsLite2;
import org.spongepowered.api.plugin.Plugin;

@Plugin(id = "exampleplugin", name = "ExamplePlugin", version = "1.0")
public class ExamplePlugin {

    // No need to call a constructor or any other method.
    // The metrics field gets initialised using @Inject :)
    // Check out https://docs.spongepowered.org/master/en/plugin/injection.html if you don't know what @Inject does
    @Inject
    private MetricsLite2 metrics;

}