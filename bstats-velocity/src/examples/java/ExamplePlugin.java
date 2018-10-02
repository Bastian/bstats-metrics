import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.bstats.velocity.Metrics;

@Plugin(id = "test", name = "Test", version = "1.0.0", description = "A test plugin", authors = { "me" })
public class ExamplePlugin {
    private ProxyServer proxy;
    private Logger logger;
    
    private static ExamplePlugin plugin;
    
    @Inject
    public ExamplePlugin(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        
        this.plugin = this;
        
        proxy.getEventManager().register(this, ProxyInitializeEvent.class, PostOrder.NORMAL, new ExamplePluginInitEventHandler());
    }
    
    public static ExamplePlugin getInstance() {
        return plugin;
    }
    
    public ProxyServer getProxy() {
        return proxy;
    }
    public Logger getLogger() {
        return logger;
    }
}