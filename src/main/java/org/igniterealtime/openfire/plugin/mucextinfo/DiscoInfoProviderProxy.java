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

import org.dom4j.Element;
import org.jivesoftware.openfire.disco.DiscoInfoProvider;
import org.jivesoftware.openfire.disco.IQDiscoInfoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A DiscoInfoProvider that delegates to another, enriching the data that it receives from the delegate.
 *
 * This class is intended to be used for MUC rooms. The room's service discovery information is enriched with data
 * obtained from the persistent storage, if such data is available for the room that's being queried.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DiscoInfoProviderProxy implements DiscoInfoProvider
{
    private static final Logger Log = LoggerFactory.getLogger(DiscoInfoProviderProxy.class);

    @Nonnull
    private final DiscoInfoProvider delegate;

    @Nonnull
    private final String serviceDomain;

    public DiscoInfoProviderProxy( final DiscoInfoProvider delegate, final String serviceDomain ) {
        this.delegate = delegate;
        this.serviceDomain = serviceDomain;
    }

    public DiscoInfoProvider getDelegate()
    {
        return delegate;
    }

    @Override
    public Iterator<Element> getIdentities( final String name, final String node, final JID senderJID )
    {
        return delegate.getIdentities(name, node, senderJID);
    }

    @Override
    public Iterator<String> getFeatures( final String name, final String node, final JID senderJID )
    {
        return delegate.getFeatures(name, node, senderJID);
    }

    @Override
    public org.xmpp.forms.DataForm getExtendedInfo( final String name, final String node, final JID senderJID )
    {
        return IQDiscoInfoHandler.getFirstDataForm(this.getExtendedInfos(name, node, senderJID));
    }

    @Override
    public Set<org.xmpp.forms.DataForm> getExtendedInfos( final String name, final String node, final JID senderJID )
    {
        Log.debug("Getting Extended Info for name '{}', node '{}', senderJID '{}'.", name, node, senderJID);

        Set<org.xmpp.forms.DataForm> result = delegate.getExtendedInfos(name, node, senderJID);
        if ( result == null )
        {
            result = new HashSet<>();
        }
        Log.trace("... obtained {} data form(s) from the delegate.", result.size());

        final List<DataForm> dataForms = DAO.retrieveExtensionElementsForRoom( new JID( name, serviceDomain, null) );
        Log.trace("... obtained {} data form(s) from the this plugin.", dataForms == null ? 0 : dataForms.size());

        if ( dataForms != null )
        {
            for ( final DataForm extensionElement : dataForms )
            {
                // TODO DataForms obtained from this plugin potentially have the same Form Type Name as one that's obtained from the delegate. How these results are merged is something that should be looked into more carefully.
                final org.xmpp.forms.DataForm dataForm = new org.xmpp.forms.DataForm(org.xmpp.forms.DataForm.Type.result);
                dataForm.addField("FORM_TYPE", null, FormField.Type.hidden).addValue(extensionElement.getFormTypeName());

                for ( final Field field : extensionElement.getFields() )
                {
                    final FormField formField = dataForm.addField(field.getVarName(), field.getLabel(), null);
                    for ( final String value : field.getValues() )
                    {
                        formField.addValue(value);
                    }
                }
                result.add(dataForm);
            }
        }
        return result;
    }

    @Override
    public boolean hasInfo( final String name, final String node, final JID senderJID )
    {
        return delegate.hasInfo(name, node, senderJID);
    }
}
