import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

public class Configuration {

    private static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static org.apache.commons.configuration2.Configuration CONFIG;

    private Configuration() {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<PropertiesConfiguration> builder =
                    new FileBasedConfigurationBuilder<>(
                            PropertiesConfiguration.class).configure(params.fileBased()
                            .setListDelimiterHandler(new DefaultListDelimiterHandler(','))
                            .setFile(new File("config.properties")));
            CONFIG = builder.getConfiguration();
        } catch (ConfigurationException cex) {
            LOG.error("Configuration Exception: ", cex);
        }
    }

    private static class LazyHolder {
        static final Configuration INSTANCE = new Configuration();
    }
    public static org.apache.commons.configuration2.Configuration getInstance() {
        return LazyHolder.INSTANCE.CONFIG;
    }
}
