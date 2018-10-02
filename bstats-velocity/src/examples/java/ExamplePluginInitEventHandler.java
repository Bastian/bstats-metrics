import java.util.concurrent.Callable;

import com.velocitypowered.api.event.EventHandler;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;

import org.bstats.velocity.Metrics;

public class ExamplePluginInitEventHandler implements EventHandler<ProxyInitializeEvent> {
    public void execute(ProxyInitializeEvent event) {
        ExamplePlugin plugin = ExamplePlugin.getInstance();
        
        // All you have to do is adding this line in your onEnable method:
        Metrics metrics = new Metrics(plugin.getProxy().getPluginManager().fromInstance(plugin), plugin.getProxy(), plugin.getLogger());

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
