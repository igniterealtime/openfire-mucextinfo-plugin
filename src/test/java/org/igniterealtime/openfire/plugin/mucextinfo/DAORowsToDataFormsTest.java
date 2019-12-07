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

import org.jivesoftware.util.InitializationException;
import org.jivesoftware.util.cache.CacheFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;

/**
 * Unit tests that verify the functionality of {@link DAO#rowsToDataForms(ConcurrentMap)}
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DAORowsToDataFormsTest
{
    @BeforeClass
    public static void beforeClass()
    {
        try {
            // The DAO class references a cache. The unit tests in this implementation don't use the cache, but it'll
            // need to be constructed for the implementation-under-test to be initialized.
            CacheFactory.initialize();
        } catch ( InitializationException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void beforeTest() {
        // reduces the chance that tests influence each-other.
        CacheFactory.clearCaches();
    }

    /**
     * Verifies that a null result is returned if the input didn't contain any rows (was null).
     */
    @Test
    public void testNullRows()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = null;

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNull( result );
    }

    /**
     * Verifies that a null result is returned if the input didn't contain any rows (was empty).
     */
    @Test
    public void testEmptyRows()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNull( result );
    }

    /**
     * Verifies that database content that consists of one form, (without field data), that an empty form is returned.
     */
    @Test
    public void testEmptyForm()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertTrue( result.get(0).getFields().isEmpty() );
    }

    /**
     * Verifies that database content that consists of one form, with one single valued field can be properly
     * transformed.
     */
    @Test
    public void testSimpleForm()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "my label", "my value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertEquals( "my label", result.get(0).getFields().get(0).getLabel() );
        assertEquals( 1, result.get(0).getFields().get(0).getValues().length );
        assertEquals( "my value", result.get(0).getFields().get(0).getValues()[0] );
    }

    /**
     * Verifies that database content that consists of one form, with one single valued field can be properly
     * transformed if there's no 'label' value (which should be optional).
     */
    @Test
    public void testOptionalLabel()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", null, "my value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertNull( result.get(0).getFields().get(0).getLabel() );
        assertEquals( 1, result.get(0).getFields().get(0).getValues().length );
        assertEquals( "my value", result.get(0).getFields().get(0).getValues()[0] );
    }

    /**
     * Verifies that database content that consists of one form can be properly transformed if there's no 'value' value
     * (which should be optional).
     */
    @Test
    public void testOptionalValue()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "label" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertEquals( "label", result.get(0).getFields().get(0).getLabel() );
        assertEquals( 0, result.get(0).getFields().get(0).getValues().length );
    }

    /**
     * Verifies that database content that consists of one form, with two single valued fields can be properly
     * transformed.
     */
    @Test
    public void testSimpleFormTwoFields()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "my label", "my value" ) );
        rows.add( new Field( "Var2", "another label", "another value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        final List<Field> fields = result.get(0).getFields();
        assertEquals(2, fields.size() );

        final Field myVarField = fields.stream().filter( field -> field.getVarName().equals("myVar")).findAny().orElse(null);
        assertNotNull(myVarField);
        assertEquals("myVar", myVarField.getVarName() );
        assertEquals("my label", myVarField.getLabel() );
        assertEquals(1, myVarField.getValues().length );
        assertEquals("my value", myVarField.getValues()[0] );

        final Field var2Field = fields.stream().filter( field -> field.getVarName().equals("Var2")).findAny().orElse(null);
        assertNotNull(var2Field);
        assertEquals("Var2", var2Field.getVarName() );
        assertEquals("another label", var2Field.getLabel() );
        assertEquals(1, var2Field.getValues().length );
        assertEquals("another value", var2Field.getValues()[0] );
    }

    /**
     * Verifies that database content that consists of one form, with one multi-valued field can be properly
     * transformed.
     */
    @Test
    public void testMultiValuedField()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "my label", "my value" ) );
        rows.add( new Field( "myVar", "my label", "my other value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertEquals( "my label", result.get(0).getFields().get(0).getLabel() );
        assertEquals( 2, result.get(0).getFields().get(0).getValues().length );
        assertTrue( Arrays.asList(result.get(0).getFields().get(0).getValues()).contains("my value") );
        assertTrue( Arrays.asList(result.get(0).getFields().get(0).getValues()).contains("my other value") );
    }

    /**
     * Verifies that database content that consists of one form, with one multi-valued field of which one has a null
     * value, results in a single-valued result.
     */
    @Test
    public void testMultiValuedFieldWithNull()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", null ) );
        rows.add( new Field( "myVar", "my label", "my other value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertEquals( 1, result.get(0).getFields().get(0).getValues().length );
        assertTrue( Arrays.asList(result.get(0).getFields().get(0).getValues()).contains("my other value") );
    }

    /**
     * Verifies that database content that consists of two forms, with one single valued field each can be properly
     * transformed.
     */
    @Test
    public void testTwoForms()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "my label", "my value" ) );
        input.put( "myForm", rows );
        final ArrayList<Field> rows2 = new ArrayList<>();
        rows2.add( new Field( "barfoo", "foobar", "test" ) );
        input.put( "myOtherForm", rows2 );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 2, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        assertEquals( 1, result.get(0).getFields().size() );
        assertEquals( "myVar", result.get(0).getFields().get(0).getVarName() );
        assertEquals( "my label", result.get(0).getFields().get(0).getLabel() );
        assertEquals( 1, result.get(0).getFields().get(0).getValues().length );
        assertEquals( "my value", result.get(0).getFields().get(0).getValues()[0] );

        assertEquals( "myOtherForm", result.get(1).getFormTypeName() );
        assertEquals( 1, result.get(1).getFields().size() );
        assertEquals( "barfoo", result.get(1).getFields().get(0).getVarName() );
        assertEquals( "foobar", result.get(1).getFields().get(0).getLabel() );
        assertEquals( 1, result.get(1).getFields().get(0).getValues().length );
        assertEquals( "test", result.get(1).getFields().get(0).getValues()[0] );
    }

    /**
     * Verifies that database content that consists of two forms, both multi-valued, can each can be properly
     * transformed.
     */
    @Test
    public void testTwoFormsMultiValued()
    {
        // Setup test fixture.
        final ConcurrentMap<String, ArrayList<Field>> input = new ConcurrentHashMap<>();
        final ArrayList<Field> rows = new ArrayList<>();
        rows.add( new Field( "myVar", "my label", "my value" ) );
        rows.add( new Field( "myVar", "my label", "my other value" ) );
        rows.add( new Field( "Var2", "another label", "another value" ) );
        rows.add( new Field( "Var2", "another label", "second value" ) );
        input.put( "myForm", rows );

        // Execute system under test.
        final ArrayList<DataForm> result = DAO.rowsToDataForms(input);

        // Verify results.
        assertNotNull( result );
        assertEquals( 1, result.size() );
        assertEquals( "myForm", result.get(0).getFormTypeName() );
        final List<Field> fields = result.get(0).getFields();
        assertEquals(2, fields.size() );

        final Field myVarField = fields.stream().filter( field -> field.getVarName().equals("myVar")).findAny().orElse(null);
        assertNotNull(myVarField);
        assertEquals("myVar", myVarField.getVarName() );
        assertEquals("my label", myVarField.getLabel() );
        assertEquals(2, myVarField.getValues().length );
        assertTrue( Arrays.asList(myVarField.getValues()).contains("my value") );
        assertTrue( Arrays.asList(myVarField.getValues()).contains("my other value") );

        assertEquals("my value", myVarField.getValues()[0] );

        final Field var2Field = fields.stream().filter( field -> field.getVarName().equals("Var2")).findAny().orElse(null);
        assertNotNull(var2Field);
        assertEquals("Var2", var2Field.getVarName() );
        assertEquals("another label", var2Field.getLabel() );
        assertEquals(2, var2Field.getValues().length );
        assertTrue( Arrays.asList(var2Field.getValues()).contains("another value") );
        assertTrue( Arrays.asList(var2Field.getValues()).contains("second value") );
    }
}
