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

import org.junit.Test;
import org.xmpp.forms.DataForm;
import org.xmpp.forms.FormField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Unit tests that verify the functionality of {@link DiscoInfoProviderProxy#merge(Set, ExtDataForm)}
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DiscoInfoProviderProxyMergeTest
{
    /**
     * Verifies that an empty result is returned if the merged input consists of a null collection of original values
     * and a null extension.
     */
    @Test
    public void testNullOriginalNullExtensions()
    {
        // Setup test fixture.
        final Set<DataForm> originals = null;
        final ExtDataForm extension = null;

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertTrue( results.isEmpty() );
    }

    /**
     * Verifies that an empty result is returned if the merged input consists of a null collection of original values
     * and a null extension.
     */
    @Test
    public void testEmptyOriginalNullExtensions()
    {
        // Setup test fixture.
        final Set<DataForm> originals = new HashSet<>();
        final ExtDataForm extension = null;

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertTrue( results.isEmpty() );
    }

    /**
     * Verifies that a result is returned that includes the extension data if the merged input consists of a null
     * collection of original values and a non-null extension.
     */
    @Test
    public void testNullOriginalWithOneExtension()
    {
        // Setup test fixture.
        final Set<DataForm> originals = null;
        final ExtDataForm extension = new ExtDataForm( "testform" );
        extension.getFields().add( new Field( "testvar", "testlabel", "testvalue") );

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertEquals( 1, results.size() );
        final DataForm result = results.iterator().next();
        final List<FormField> fields = result.getFields();
        assertEquals( 2, fields.size() ); // should contain the field we added, as well as a 'FORM_TYPE' field.
        assertTrue( fields.stream().anyMatch( formField -> "FORM_TYPE".equals( formField.getVariable() )
                                                        && "testform".equals( formField.getFirstValue() )
                                                        && formField.getValues().size() == 1
        ));
        assertTrue( fields.stream().anyMatch( formField -> "testvar".equals( formField.getVariable() )
                                                        && "testlabel".equals( formField.getLabel() )
                                                        && "testvalue".equals( formField.getFirstValue() )
                                                        && formField.getValues().size() == 1
        ));
    }

    /**
     * Verifies that a result is returned that includes the extension data if the merged input consists of a collection
     * of one data form, and an extension that contains another data form.
     */
    @Test
    public void testOriginalWithDifferentExtension()
    {
        // Setup test fixture.
        final Set<DataForm> originals = new HashSet<>();
        final DataForm dataForm = new DataForm(DataForm.Type.result);
        dataForm.addField("FORM_TYPE", null, FormField.Type.hidden ).addValue( "origform");
        dataForm.addField("origvar", "origlabel", null ).addValue( "origvalue");
        originals.add( dataForm );

        final ExtDataForm extension = new ExtDataForm( "extform" );
        extension.getFields().add( new Field( "extvar", "extlabel", "extvalue") );

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertEquals( 2, results.size() );

        // dataform from original.
        final DataForm origResult = results.stream().filter( df -> df.getFields().stream().anyMatch( formField -> "FORM_TYPE".equals( formField.getVariable() ) && "origform".equals( formField.getFirstValue() ) ) ).findAny().orElse(null);
        assertNotNull( origResult );
        final List<FormField> origFields = origResult.getFields();
        assertEquals( 2, origFields.size() ); // should contain the field we added, as well as a 'FORM_TYPE' field.
        assertTrue( origFields.stream().anyMatch( formField -> "origvar".equals( formField.getVariable() )
            && "origlabel".equals( formField.getLabel() )
            && "origvalue".equals( formField.getFirstValue() )
            && formField.getValues().size() == 1
        ));

        // dataform from extension
        final DataForm extResult = results.stream().filter( df -> df.getFields().stream().anyMatch( formField -> "FORM_TYPE".equals( formField.getVariable() ) && "extform".equals( formField.getFirstValue() ) ) ).findAny().orElse(null);
        assertNotNull( extResult );
        final List<FormField> extFields = extResult.getFields();
        assertEquals( 2, extFields.size() ); // should contain the field we added, as well as a 'FORM_TYPE' field.
        assertTrue( extFields.stream().anyMatch( formField -> "extvar".equals( formField.getVariable() )
            && "extlabel".equals( formField.getLabel() )
            && "extvalue".equals( formField.getFirstValue() )
            && formField.getValues().size() == 1
        ));
    }

    /**
     * Verifies that a result is returned that includes the extension data if the merged input consists of a collection
     * of one data form, and an extension that contains different fields for the same form that's in the original data.
     */
    @Test
    public void testOriginalWithExtensionSameFormDifferentField()
    {
        // Setup test fixture.
        final Set<DataForm> originals = new HashSet<>();
        final DataForm dataForm = new DataForm(DataForm.Type.result);
        dataForm.addField("FORM_TYPE", null, FormField.Type.hidden ).addValue( "origform");
        dataForm.addField("origvar", "origlabel", null ).addValue( "origvalue");
        originals.add( dataForm );

        final ExtDataForm extension = new ExtDataForm( "origform" );
        extension.getFields().add( new Field( "extvar", "extlabel", "extvalue") );

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertEquals( 1, results.size() );

        // dataform from original.
        final DataForm origResult = results.stream().filter( df -> df.getFields().stream().anyMatch( formField -> "FORM_TYPE".equals( formField.getVariable() ) && "origform".equals( formField.getFirstValue() ) ) ).findAny().orElse(null);
        assertNotNull( origResult );
        final List<FormField> resultFields = origResult.getFields();
        assertEquals( 3, resultFields.size() ); // should contain the field we added, as well as a 'FORM_TYPE' field.
        assertTrue( resultFields.stream().anyMatch( formField -> "origvar".equals( formField.getVariable() )
            && "origlabel".equals( formField.getLabel() )
            && "origvalue".equals( formField.getFirstValue() )
            && formField.getValues().size() == 1
        ));
        assertTrue( resultFields.stream().anyMatch( formField -> "extvar".equals( formField.getVariable() )
            && "extlabel".equals( formField.getLabel() )
            && "extvalue".equals( formField.getFirstValue() )
            && formField.getValues().size() == 1
        ));
    }

    /**
     * Verifies that a result is returned that includes the extension data if the merged input consists of a collection
     * of one data form, and an extension that contains the same field for the same form that's in the original data,
     * using a different value. The expected result is one multi-valued field in the result.
     */
    @Test
    public void testOriginalWithExtensionSameFormSameFieldDifferentValue()
    {
        // Setup test fixture.
        final Set<DataForm> originals = new HashSet<>();
        final DataForm dataForm = new DataForm(DataForm.Type.result);
        dataForm.addField("FORM_TYPE", null, FormField.Type.hidden ).addValue( "origform");
        dataForm.addField("origvar", "origlabel", null ).addValue( "origvalue");
        originals.add( dataForm );

        final ExtDataForm extension = new ExtDataForm( "origform" );
        extension.getFields().add( new Field( "origvar", "origlabel", "extvalue") );

        // Execute system under test.
        final Set<DataForm> results = DiscoInfoProviderProxy.merge( originals, extension );

        // Verify results.
        assertNotNull( results );
        assertEquals( 1, results.size() );

        // dataform from original.
        final DataForm origResult = results.stream().filter( df -> df.getFields().stream().anyMatch( formField -> "FORM_TYPE".equals( formField.getVariable() ) && "origform".equals( formField.getFirstValue() ) ) ).findAny().orElse(null);
        assertNotNull( origResult );
        final List<FormField> resultFields = origResult.getFields();
        assertEquals( 2, resultFields.size() ); // should contain the field we added, as well as a 'FORM_TYPE' field.
        assertTrue( resultFields.stream().anyMatch( formField -> "origvar".equals( formField.getVariable() )
            && "origlabel".equals( formField.getLabel() )
            && formField.getType().equals(FormField.Type.text_multi)
            && formField.getValues().contains( "origvalue")
            && formField.getValues().contains( "extvalue")
            && formField.getValues().size() == 2
        ));
    }
}
