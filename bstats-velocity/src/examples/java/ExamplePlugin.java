import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.event.Subscribe;
import org.slf4j.Logger;
import org.bstats.velocity.Metrics;

@Plugin(id = "MyPlugin", name = "MyPlugin", version = "1.0.0", description = "A test plugin", authors = { "me" })
public class ExamplePlugin {

    private ProxyServer proxy;
    private Logger logger;

    @Inject
    public ExamplePlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {

        // All you have to do is add this here
        Metrics metrics = new Metrics(new Metrics.PluginData(this, proxy, logger));

        // Optional: Custom charts
        metrics.addCustomChart(new Metrics.SimplePie("mychart_id", () -> "Yay I am displayed!"));
    }

}