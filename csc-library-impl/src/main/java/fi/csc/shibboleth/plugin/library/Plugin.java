package fi.csc.shibboleth.plugin.library;

import java.io.IOException;
import java.util.Collections;

import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.plugin.PropertyDrivenIdPPlugin;
import net.shibboleth.profile.module.ModuleException;
import net.shibboleth.profile.plugin.PluginException;

/**
 * Details about the user profile plugin.
 */
public class Plugin extends PropertyDrivenIdPPlugin {

    /**
     * Constructor.
     * 
     * @throws IOException     if the properties fail to load
     * @throws PluginException if other errors occur
     * @throws ModuleException 
     */
    public Plugin() throws IOException, PluginException, ModuleException {
        super(Plugin.class);
        try {
            final IdPModule module = new Module();
            setEnableOnInstall(Collections.singleton(module));
            setDisableOnRemoval(Collections.singleton(module));
        } catch (final IOException e) {
            throw e;
        }
    }

}