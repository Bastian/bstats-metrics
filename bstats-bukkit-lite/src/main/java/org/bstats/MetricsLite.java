package org.bstats;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

/**
 * bStats collects some data for plugin authors.
 *
 * Check out https://bStats.org/ to learn more about bStats!
 */
public class MetricsLite {
	
	
	
	/**
	 * 
	 * REGION Main Metrics loading
	 * 
	 */
	
	
	
    static {
    	// This entire block serves to make sure nobody just copy & pastes the example and uses the wrong package names
    	
    	// Maven's Relocate is clever and changes strings as well as packages. We have to use this little "trick" ... :D
        final String defaultPackage = new String(new byte[] { 'o', 'r', 'g', '.', 'b', 's', 't', 'a', 't', 's' });
        final String examplePackage = new String(new byte[] { 'y', 'o', 'u', 'r', '.', 'p', 'a', 'c', 'k', 'a', 'g', 'e' });
        
        // Get the current package & class name
        final String currentPackage = MetricsLite.class.getPackage().getName();
        
        // Is it the default or example package, as defined above?
        if (currentPackage.equals(defaultPackage) || currentPackage.equals(examplePackage)) {
            throw new IllegalStateException("bStats Metrics class has not been relocated correctly!");
        }
    }
    
    // The version of this bStats class
    public static final int B_STATS_VERSION = 1;
    
    // The url to which the data is sent
    private static final String URL = "https://bStats.org/submitData/bukkit";
    
    // Should failed requests be logged?
    private static boolean logFailedRequests;
    
    // The uuid of the server
    private static String serverUUID;
    
    // The plugin
    public final JavaPlugin plugin;
    private JavaPlugin masterPlugin;
    
    // Cached vars, for performance
    private ServicesManager servicesManager = Bukkit.getServicesManager();
    private Logger logger;
    
    /**
     * Class constructor.
     *
     * @param plugin The plugin whose stats should be submitted.
     */
    public MetricsLite(JavaPlugin plugin) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null!");
        }
        this.plugin = plugin;
        logger = plugin.getLogger();
        
        YamlConfiguration config = getConfig(plugin.getDataFolder().getParentFile());
        if (!config.getBoolean("enabled", true)) {
           return;
        }
        
        // Search for all other bStats Metrics classes to see if we are the first one
        boolean isFirstPlugin = getBStatsClasses().isEmpty();
        // Register the current plugin's metrics service
        servicesManager.register(MetricsLite.class, this, plugin, ServicePriority.Normal);
        
        if (isFirstPlugin) {
            // We are the first!
            startSubmitting();
        }
    }
    
    /**
     * Starts the Scheduler which submits our data every 30 minutes.
     */
    private void startSubmitting() {
    	masterPlugin = plugin;
    	
        final Timer timer = new Timer(true); // We use a timer because the Bukkit scheduler isn't accurate (relies on ticks, affected by server performance)
        
        /**
         * Submit the data every 30 minutes, first time after 5 minutes to give other plugins enough time to start
         * WARNING Changing the frequency has no effect but your plugin WILL be blocked/deleted!
         * WARNING Just don't do it!
         */
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!masterPlugin.isEnabled()) { // Plugin was disabled
                	// Try to get a new master plugin for sending stats
                    JavaPlugin newMasterPlugin = null;
                    for (Class<?> c : getBStatsClasses()) {
                    	try {
                    		Field javaPlugin = c.getField("plugin");
							newMasterPlugin = (JavaPlugin) javaPlugin.get(c);
						} catch (Exception ex) {
							continue;
						}
                    	
                    	// Found a new potential plugin! Is it enabled?
                    	if (!newMasterPlugin.isEnabled()) {
                    		continue;
                    	}
                    	
                    	break;
                    }
                    
                    if (newMasterPlugin == null || !newMasterPlugin.isEnabled()) {
                    	// No plugins with bStats are enabled. Try again later :(
                		return;
                	}
                    // Set the masterPlugin instance to the new one we've found
                    masterPlugin = newMasterPlugin;
                }
                
                // At this point, we can guarantee the masterPlugin is available for use
                
                // We eventually want our code to run in the Bukkit main thread, so we have to use the Bukkit scheduler
                // Don't be afraid! The connection to the bStats server is still async. Only the stats collection is sync ;)
                Bukkit.getScheduler().runTask(masterPlugin, submitData);
            }
        }, 1000 * 60 * 5, 1000 * 60 * 30);
    }
    
    /**
     * Gets the Metrics config file and sets defaults if it doesn't exist
     * 
     * @param pluginsFolder The parent (/plugins) folder in which the bStats folder lives 
     * @return The config file
     */
    private YamlConfiguration getConfig(File pluginsFolder) {
    	// Get the config file
        File configFile = new File(new File(pluginsFolder, "bStats"), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check if the config file exists
        if (!config.isSet("serverUuid")) {
        	
            // Add default values
            config.addDefault("enabled", true);
            // Every server gets it's unique random id.
            config.addDefault("serverUuid", UUID.randomUUID().toString());
            // Should failed request be logged?
            config.addDefault("logFailedRequests", false);
            
            // Inform the server owners about bStats
            config.options().header(
                    "bStats collects some data for plugin authors like how many servers are using their plugins.\n" +
                            "To honor their work, you should not disable it.\n" +
                            "This has nearly no effect on the server performance!\n" +
                            "Check out https://bStats.org/ to learn more :)"
            ).copyDefaults(true);
            
            try {
                config.save(configFile);
            } catch (IOException ignored) { }
        }
        
        // Load the data
        serverUUID = config.getString("serverUuid");
        logFailedRequests = config.getBoolean("logFailedRequests", false);
        
        return config;
    }
    
    
    
    /**
     * 
     * REGION Data collection
     * 
     */
    
    
    
    /**
     * Gets the plugin specific data.
     * This method is called using Reflection.
     *
     * @return The plugin specific data.
     */
    @SuppressWarnings("unchecked")
	public JSONObject getPluginData() {
        JSONObject data = new JSONObject();
        
        // Don't cache these values, as they can change at any time (plugin auto-updaters)
        data.put("pluginName", plugin.getDescription().getName()); // Append the name of the plugin
        data.put("pluginVersion", plugin.getDescription().getVersion()); // Append the version of the plugin
        JSONArray customCharts = new JSONArray();
        data.put("customCharts", customCharts);
        
        return data;
    }
    
    /**
     * Gets the server-specific data.
     *
     * @return A new JSONObject containing server-specific data.
     */
    @SuppressWarnings("unchecked")
	private JSONObject getServerData() {
        // Minecraft data
        int playerAmount;
        try {
            // Around MC 1.8 the return type was changed to a collection from an array,
            // This fixes java.lang.NoSuchMethodError: org.bukkit.Bukkit.getOnlinePlayers()Ljava/util/Collection;
            Method onlinePlayersMethod = Class.forName("org.bukkit.server").getMethod("getOnlinePlayers");
            playerAmount = onlinePlayersMethod.getReturnType().equals(Collection.class)
                    ? ((Collection<?>) onlinePlayersMethod.invoke(Bukkit.getServer())).size()
                    : ((Player[]) onlinePlayersMethod.invoke(Bukkit.getServer())).length;
        } catch (Exception ex) {
            playerAmount = Bukkit.getOnlinePlayers().size(); // Just use the new method if the Reflection failed
        }
        
        String bukkitVersion = Bukkit.getVersion();
        bukkitVersion = bukkitVersion.substring(bukkitVersion.indexOf("MC: ") + 4, bukkitVersion.length() - 1);
        
        JSONObject data = new JSONObject();
        
        data.put("serverUUID", serverUUID);
        data.put("playerAmount", playerAmount);
        data.put("onlineMode", (Bukkit.getOnlineMode()) ? 1 : 0);
        data.put("bukkitVersion", bukkitVersion);
        
        // OS/Java data
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("osName", System.getProperty("os.name"));
        data.put("osArch", System.getProperty("os.arch"));
        data.put("osVersion", System.getProperty("os.version"));
        data.put("coreCount", Runtime.getRuntime().availableProcessors());
        
        return data;
    }
    
    
    
    /**
     * 
     * REGION Data sending
     * 
     */
    
    
    
    /**
     * Collects all data and sends it to the server asynchronously
     * We're pretending this is a function
     */
    private Runnable submitData = new Runnable() {
        @SuppressWarnings("unchecked")
		@Override
        public void run() {
        	final JSONObject data = getServerData();
            
            JSONArray pluginData = new JSONArray();
            // Search for all bStats Metrics classes to get their plugin data
            for (Class<?> c : getBStatsClasses()) {
            	 for (RegisteredServiceProvider<?> provider : servicesManager.getRegistrations(c)) {
                     try {
                         pluginData.add(provider.getService().getMethod("getPluginData").invoke(provider.getProvider()));
                     } catch (Exception ignored) { }
                 }
            }
            
            data.put("plugins", pluginData);
            
            // Connections to bStats should be run asynchronously
            Bukkit.getScheduler().runTaskAsynchronously(masterPlugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        // Send the data
                        sendData(data);
                    } catch (Exception ex) {
                        // Something went wrong! :(
                        if (logFailedRequests) {
                            logger.log(Level.WARNING, "Could not submit plugin stats of " + plugin.getName(), ex);
                        }
                    }
                }
            });
        }
    };
    
    /**
     * Sends the data to the bStats server.
     *
     * @param data The data to send.
     * @throws Exception If the request failed.
     */
    private static void sendData(JSONObject data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null!");
        }
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalAccessException("This method must not be called from the main thread!");
        }
        HttpsURLConnection connection = (HttpsURLConnection) new URL(URL).openConnection();
        
        // Compress the data to save bandwidth
        byte[] compressedData = compress(data.toString());
        
        // Add headers
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Accept", "application/json");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Encoding", "gzip"); // We gzip our request
        connection.addRequestProperty("Content-Length", String.valueOf(compressedData.length));
        connection.setRequestProperty("Content-Type", "application/json"); // We send our data in JSON format
        connection.setRequestProperty("User-Agent", "MC-Server/" + B_STATS_VERSION);
        
        // Send data
        connection.setDoOutput(true);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(compressedData);
        outputStream.flush();
        outputStream.close();
        
        connection.getInputStream().close(); // We don't care about the response - Just send our data :)
    }
    
    
    
    /**
     * 
     * REGION Utilities
     * 
     */
    
    
    
    /**
     * Gzips a given String.
     *
     * @param str The string to gzip.
     * @return The gzipped String.
     * @throws IOException If the compression failed.
     */
    private static byte[] compress(final String str) throws IOException {
        if (str == null) {
            return null;
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(outputStream);
        gzip.write(str.getBytes("UTF-8"));
        gzip.close();
        
        return outputStream.toByteArray();
    }
    
    /**
     * Gets a list of all bStats services currently registered
     * 
     * @return A list of registered bStats classes
     */
    private List<Class<?>> getBStatsClasses() {
    	ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
    	
    	for (Class<?> service : servicesManager.getKnownServices()) {
        	Field[] fields = service.getFields();
        	for (int i = 0; i < fields.length; i++) {
        		if (fields[i].getName().equals("B_STATS_VERSION")) { // Our identifier :)
        			classes.add(service);
        		}
        	}
        }
    	
    	return classes;
    }
}