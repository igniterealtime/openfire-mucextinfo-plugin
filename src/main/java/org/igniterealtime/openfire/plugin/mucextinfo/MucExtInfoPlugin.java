/*
 * Copyright (C) 2019 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.openfire.plugin.mucextinfo;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.IQDiscoInfoHandler;
import org.jivesoftware.openfire.muc.MultiUserChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * An Openfire plugin that enriches responses to Service Discovery requests with custom data, using extensions as
 * described in XEP-0128: Service Discovery Extensions.
 *
 * The operation of this plugin is based on the premise of replacing the service discovery providers that are used by
 * the Multi User Chat implementation of Openfire. The replacement providers implement the delegate pattern, in which
 * they primarily delegate to the providers that they replace, and enrich data that is returned by the delegate.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="https://xmpp.org/extensions/xep-0128.html">XEP-0128: Service Discovery Extensions</a>
 */
public class MucExtInfoPlugin implements Plugin
{
    private static final Logger Log = LoggerFactory.getLogger(MucExtInfoPlugin.class);

    @Override
    public void initializePlugin( final PluginManager manager, final File pluginDirectory )
    {
        try
        {
            replaceMUCServiceProviders();
        }
        catch ( Exception e )
        {
            Log.error("An exception occurred while trying to replace MUC Service Disco Info Providers.", e);
        }
    }

    @Override
    public void destroyPlugin()
    {
        try
        {
            restoreMUCServiceProviders();
        }
        catch ( Exception e )
        {
            Log.error("An exception occurred while trying to restore MUC Service Disco Info Providers.", e);
        }
    }

    public static DiscoInfoProvider getProvider( final MultiUserChatService service ) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        final IQDiscoInfoHandler iqDiscoInfoHandler = XMPPServer.getInstance().getIQDiscoInfoHandler();

        final Method getMethod = iqDiscoInfoHandler.getClass().getDeclaredMethod("getProvider", String.class);
        try
        {
            getMethod.setAccessible(true);
            return (DiscoInfoProvider) getMethod.invoke(iqDiscoInfoHandler, service.getServiceDomain());
        }
        finally
        {
            getMethod.setAccessible(false);
        }
    }

    public static void setProvider( final MultiUserChatService service, DiscoInfoProvider provider ) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        final IQDiscoInfoHandler iqDiscoInfoHandler = XMPPServer.getInstance().getIQDiscoInfoHandler();

        final Method setMethod = iqDiscoInfoHandler.getClass().getDeclaredMethod("setProvider", String.class, DiscoInfoProvider.class);
        try
        {
            setMethod.setAccessible(true);
            setMethod.invoke(iqDiscoInfoHandler, service.getServiceDomain(), provider );
        }
        finally
        {
            setMethod.setAccessible(false);
        }
    }

    protected synchronized void replaceMUCServiceProviders() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException
    {
        Log.info("Replacing original IQ Disco Info handlers for all MUC services with proxies.");
        final List<MultiUserChatService> services = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatServices();
        for ( final MultiUserChatService service : services )
        {
            final DiscoInfoProvider old = getProvider( service );
            if ( old != null )
            {
                Log.trace("... replacing handler for MUC service '{}'.", service.getServiceDomain());
                setProvider( service, new DiscoInfoProviderProxy( old, service.getServiceDomain() ) );
            }
        }
        Log.debug("Finished replacing all relevant handlers.");
    }

    protected synchronized void restoreMUCServiceProviders() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException
    {
        Log.info("Restoring the original IQ Disco Info handlers for all MUC services (the proxies that were loaded by this plugin are discarded).");
        final List<MultiUserChatService> services = XMPPServer.getInstance().getMultiUserChatManager().getMultiUserChatServices();
        for ( final MultiUserChatService service : services )
        {
            final DiscoInfoProvider old = getProvider( service );
            if ( old instanceof DiscoInfoProviderProxy )
            {
                Log.trace("... restoring handler for MUC service '{}'.", service.getServiceDomain());
                setProvider( service, ((DiscoInfoProviderProxy) old).getDelegate() );
            }
        }

        Log.debug("Finished restoring all relevant handlers.");
    }
}
