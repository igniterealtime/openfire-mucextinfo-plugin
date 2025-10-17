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
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;
import org.xmpp.packet.JID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

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
        Log.trace("... obtained {} data form(s) from the delegate.", result.size());

        final List<ExtDataForm> dataForms = DAO.retrieveExtensionElementsForRoom(new JID(name, serviceDomain, null) );
        Log.trace("... obtained {} data form(s) from the this plugin.", dataForms == null ? 0 : dataForms.size());

        if ( dataForms != null )
        {
            for ( final ExtDataForm extensionElement : dataForms )
            {
                result = merge( result, extensionElement );
            }
        }
        return result;
    }

    @Override
    public boolean hasInfo( final String name, final String node, final JID senderJID )
    {
        return delegate.hasInfo(name, node, senderJID);
    }

    @Nonnull
    static Set<org.xmpp.forms.DataForm> merge( @Nullable Set<org.xmpp.forms.DataForm> dataForms, @Nullable ExtDataForm extensionElement) {
        Set<org.xmpp.forms.DataForm> result;
        if ( dataForms == null ) {
            result = new HashSet<>();
        } else {
            result = new HashSet<>(dataForms);
        }

        if ( extensionElement == null ) {
            return result;
        }

        // Construct a new data form (that might go unused, see below).
        final DataForm newDataForm = new org.xmpp.forms.DataForm(org.xmpp.forms.DataForm.Type.result);
        newDataForm.addField("FORM_TYPE", null, FormField.Type.hidden).addValue(extensionElement.getFormTypeName());

        // Find from the results a data form for the variable, otherwise use the newly created one.
        final DataForm dataForm = result.stream().filter(df -> df.getFields().stream()
            .anyMatch(
                formField -> "FORM_TYPE".equals(formField.getVariable()) &&
                    extensionElement.getFormTypeName().equals(formField.getFirstValue())))
            .findAny()
            .orElse( newDataForm );

        // Now, add or merge fields from the extension data into the result.
        for ( final Field extensionField : extensionElement.getFields() )
        {
            FormField formField = dataForm.getField(extensionField.getVarName());
            if ( formField == null ) {
                formField = dataForm.addField( extensionField.getVarName(), extensionField.getLabel(), null);
            }

            for ( final String value : extensionField.getValues() )
            {
                formField.addValue(value);
            }

            // Default type is text-single, multiple values means it actually is a multi-field
            if(isMultiField(formField)) {
                formField.setType(FormField.Type.text_multi);
            }
        }

        result.add(dataForm);

        return result;
    }
    private static boolean isMultiField(FormField field) {
        return field.getValues() != null && field.getValues().size() > 1;
    }
}
