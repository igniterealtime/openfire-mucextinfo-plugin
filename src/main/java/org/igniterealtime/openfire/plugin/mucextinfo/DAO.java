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

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.CacheableOptional;
import org.jivesoftware.util.cache.Cache;
import org.jivesoftware.util.cache.CacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * Data Access Object, responsible for storing and retrieving data in persistent storage.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DAO
{
    private final static Cache<JID, CacheableOptional<ArrayList<ExtDataForm>>> EXTENSIONS_BY_ROOM = CacheFactory.createLocalCache("MUC Extended Service Discovery");

    private static final String SQL_REMOVE_FORM = "DELETE FROM mucextinfo WHERE room = ? AND formtypename = ?";
    private static final String SQL_ADD_FIELD = "INSERT INTO mucextinfo (room, formtypename, varname, label, varvalue) VALUES (?, ?, ?, ?, ?)";
    private static final String SQL_REMOVE_FIELD = "DELETE FROM mucextinfo WHERE room = ? AND formtypename = ? AND varname = ?";
    private static final String SQL_GET_ROOM_FORMS = "SELECT formtypename, varname, label, varvalue FROM mucextinfo WHERE room = ? ORDER BY formtypename";

    private static final Logger Log = LoggerFactory.getLogger(DAO.class);

    /**
     * Adds an (empty) data form for 'extended' service discovery information that relates to a specific room to the
     * database.
     *
     * When the combination of room and data form name does already exist in the database, then the implementation will
     * not have a functional effect (although previously cached values will be reset and another row is added to the
     * database).
     *
     * @param room The address of the room of the data form to be modified.
     * @param formTypeName The identifier of the data form to be modified.
     */
    public static void addForm( JID room, String formTypeName )
    {
        room = room.asBareJID(); // normalize.
        Log.debug("Add Data Form with name '{}' for room: '{}'", formTypeName, room);

        Connection con = null;
        PreparedStatement pstmt = null;
        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SQL_ADD_FIELD);
            pstmt.setString(1, room.toBareJID());
            pstmt.setString(2, formTypeName);
            pstmt.setNull(3, Types.VARCHAR);
            pstmt.setNull(4, Types.VARCHAR);
            pstmt.setNull(5, Types.VARCHAR);
            pstmt.execute();
        }
        catch ( SQLException e )
        {
            Log.error("An exception occurred when trying to add a dataform (form type name: '{}') for room '{}' in the database.", formTypeName, room);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // Purge any cached values for this room (to be repopulated when information is retrieved again). Alternatively,
        // the cache could be 'updated', but that'd introduce code complexity that's probably not worth the minor
        // performance benefit that it'd bring.
        purgeCache(room);
    }

    /**
     * Removes one particular data form containing 'extended' service discovery information that relates to a
     * specific room. All fields related to the form will be removed from the database.
     *
     * When the combination of room and data form name does not exist in the database, then the implementation will
     * silently ignore the invocation (although previously cached values will be reset).
     *
     * @param room The address of the room of the data form to be modified.
     * @param formTypeName The identifier of the data form to be modified.
     */
    public static void removeForm( @Nonnull JID room, @Nonnull String formTypeName )
    {
        room = room.asBareJID(); // normalize.
        Log.debug("Remove Data Form with name '{}' for room: '{}'", formTypeName, room);

        Connection con = null;
        PreparedStatement pstmt = null;
        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SQL_REMOVE_FORM);
            pstmt.setString(1, room.toBareJID());
            pstmt.setString(2, formTypeName);
            pstmt.execute();
        }
        catch ( SQLException e )
        {
            Log.error("An exception occurred when trying to remove a dataform (form type name: '{}') for room '{}' in the database.", formTypeName, room, e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // Purge any cached values for this room (to be repopulated when information is retrieved again). Alternatively,
        // the cache could be 'updated', but that'd introduce code complexity that's probably not worth the minor
        // performance benefit that it'd bring.
        purgeCache(room);
    }

    /**
     * Adds a field of 'extended' service discovery information to a dataform that relates to a specific room.
     *
     * When the combination of room, data form name and field does not exist in the database, then the implementation
     * will implicitly create a data form.
     *
     * When a field by the same name already exists on the form, then the value provided will be added to the value that
     * already existed in the database, turning the field into a multi-valued field. If label values for both values
     * differ, this method makes no guarantees of what label value will be used in the resulting multi-valued field.
     *
     * @param room The address of the room of the data form to be modified.
     * @param formTypeName The identifier of the data form to be modified.
     * @param varName The identifier of the field to be added.
     * @param label The optional (human readable) label of the field to be added.
     * @param value The optional value of the field to be added.
     */
    public static void addField( @Nonnull JID room, @Nonnull String formTypeName, @Nonnull String varName, @Nullable String label, @Nullable String value )
    {
        room = room.asBareJID(); // normalize.
        Log.debug("Add Data Form with name '{}' for room: '{}'", formTypeName, room);

        Connection con = null;
        PreparedStatement pstmt = null;
        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SQL_ADD_FIELD);
            pstmt.setString(1, room.toBareJID());
            pstmt.setString(2, formTypeName);
            pstmt.setString(3, varName);
            if ( label == null || label.trim().isEmpty() )
            {
                pstmt.setNull(4, Types.VARCHAR);
            }
            else
            {
                pstmt.setString(4, label);
            }
            if ( value == null || value.trim().isEmpty() )
            {
                pstmt.setNull(5, Types.VARCHAR);
            }
            else
            {
                pstmt.setString(5, value);
            }
            pstmt.execute();
        }
        catch ( SQLException e )
        {
            Log.error("An exception occurred when trying to add a field (varname: '{}') to a dataform (form type name: '{}') for room '{}' in the database.", varName, formTypeName, room, e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // Purge any cached values for this room (to be repopulated when information is retrieved again). Alternatively,
        // the cache could be 'updated', but that'd introduce code complexity that's probably not worth the minor
        // performance benefit that it'd bring.
        purgeCache(room);
    }

    /**
     * Removes one particular field of 'extended' service discovery information from a dataform that relates to a
     * specific room.
     *
     * When the combination of room, data form name and field does not exist in the database, then the implementation
     * will silently ignore the invocation (although previously cached values will be reset).
     *
     * @param room The address of the room of the data form to be modified.
     * @param formTypeName The identifier of the data form to be modified.
     * @param varName The identifier of the field to be removed.
     */
    public static void removeField( @Nonnull JID room, @Nonnull String formTypeName, @Nonnull String varName )
    {
        room = room.asBareJID(); // normalize.
        Log.debug("Remove field '{}' from Data Form with name '{}' for room: '{}'", varName, formTypeName, room);

        Connection con = null;
        PreparedStatement pstmt = null;
        try
        {
            con = DbConnectionManager.getConnection();
            pstmt = con.prepareStatement(SQL_REMOVE_FIELD);
            pstmt.setString(1, room.toBareJID());
            pstmt.setString(2, formTypeName);
            pstmt.setString(3, varName);
            pstmt.execute();
        }
        catch ( SQLException e )
        {
            Log.error("An exception occurred when trying to remove a field (varname: '{}') from a dataform (form type name: '{}') for room '{}' in the database.", varName, formTypeName, room, e);
        }
        finally
        {
            DbConnectionManager.closeConnection(pstmt, con);
        }

        // Purge any cached values for this room (to be repopulated when information is retrieved again). Alternatively,
        // the cache could be 'updated', but that'd introduce code complexity that's probably not worth the minor
        // performance benefit that it'd bring.
        purgeCache(room);
    }

    /**
     * Retrieve data forms (that contain 'extended' service discovery information) for one particular MUC room.
     *
     * @param room The MUC room for which to return extended service discovery information.
     * @return The data forms containing the extended service discovery information for the room.
     */
    @Nullable
    public static List<ExtDataForm> retrieveExtensionElementsForRoom( @Nonnull JID room )
    {
        room = room.asBareJID(); // normalize.
        Log.debug("Get all data forms for room: '{}'", room);

        final Lock lock = EXTENSIONS_BY_ROOM.getLock(room);
        try
        {
            lock.lock();

            // Try to get a result from the cache.
            final CacheableOptional<ArrayList<ExtDataForm>> optionalResult = EXTENSIONS_BY_ROOM.get(room);
            if ( optionalResult != null )
            {
                Log.trace("Returning value from cache.");
                return optionalResult.get();
            }

            // No result in cache? Retrieve a result from the database (and add that to cache for future lookups).
            final ConcurrentMap<String, ArrayList<Field>> rows = new ConcurrentHashMap<>();

            Connection con = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try
            {
                con = DbConnectionManager.getConnection();
                pstmt = con.prepareStatement(SQL_GET_ROOM_FORMS);
                pstmt.setString(1, room.toBareJID());
                rs = pstmt.executeQuery();
                while ( rs.next() )
                {
                    // formtypename, varname, label, varvalue
                    final String formTypeName = rs.getString("formtypename");
                    final String varName = rs.getString("varname");
                    final String label = rs.getString("label");
                    final String value = rs.getString("varvalue");

                    // Ensure that there's a key in 'rows' for this current form. If nothing else, that can be used to
                    // return an empty form later.
                    final ArrayList<Field> fieldsForForm = rows.computeIfAbsent(formTypeName, s -> new ArrayList<>());

                    // If there's a field in the database, add that to the form.
                    if ( varName != null )
                    {
                        fieldsForForm.add(new Field(varName, label, value));
                    }
                }
            }
            catch ( SQLException e )
            {
                Log.error("An exception occurred when trying to retrieve all data forms for room '{}' in the database.", room, e);
            }
            finally
            {
                DbConnectionManager.closeConnection(rs, pstmt, con);
            }

            // Transform the raw database results into DataForm instances.
            final ArrayList<ExtDataForm> formsForRoom = rowsToDataForms(rows);

            // Record the end result in the cache.
            EXTENSIONS_BY_ROOM.put(room, CacheableOptional.of(formsForRoom));

            return formsForRoom;
        }
        finally
        {
            lock.unlock();
        }
    }

    /**
     * Marshalls raw database results into DataForm instances.
     *
     * The provided input is expected to relate to exactly one MUC room. The behavior of this class is unspecified if
     * data for more than one room is provided.
     *
     * @param rows the database results.
     * @return The DataForm instances parsed from the database.
     */
    @Nullable
    static ArrayList<ExtDataForm> rowsToDataForms( @Nullable final ConcurrentMap<String, ArrayList<Field>> rows )
    {
        if ( rows == null || rows.isEmpty() )
        {
            return null;
        }

        // Marshall 'rows' into DataForm instances.
        final ConcurrentMap<String, ExtDataForm> result = new ConcurrentHashMap<>();
        for ( final Map.Entry<String, ArrayList<Field>> entry : rows.entrySet() )
        {
            final String formTypeName = entry.getKey();
            final ArrayList<Field> fields = entry.getValue();

            // Get the data form for this row, create a new one if none exists yet.
            final ExtDataForm dataForm = result.computeIfAbsent(formTypeName, ExtDataForm::new);

            // Group fields by varName. Multi-valued fields will result in a mapped value of more than one element.
            final Map<String, List<Field>> groupedByVarName = fields.stream().collect(Collectors.groupingBy(Field::getVarName));

            // Merge/condense each multi-valued field into one.
            for ( final Map.Entry<String, List<Field>> groupedEntry : groupedByVarName.entrySet() )
            {
                final String varName = groupedEntry.getKey();
                final List<Field> unmergedFields = groupedEntry.getValue();
                final Field mergedField;
                if ( unmergedFields == null || unmergedFields.isEmpty() )
                {
                    mergedField = new Field(varName, null);
                }
                else
                {
                    final List<String> values = new ArrayList<>();
                    for ( final Field f : unmergedFields )
                    {
                        final String[] v = f.getValues();
                        if ( v != null && v.length > 0 )
                        {
                            values.addAll(Arrays.asList(v));
                        }
                    }

                    // Remove any 'null' values.
                    values.removeIf(Objects::isNull);

                    mergedField = new Field(varName, unmergedFields.get(0).getLabel(), values.toArray(new String[0]));
                }

                // Add the condensed field to the data form.
                dataForm.getFields().add(mergedField);
            }
        }

        return new ArrayList<>(result.values());
    }

    /**
     * Remove all entries for a particular room from the cache.
     *
     * @param room The room for which to remove all cached entries.
     */
    protected static void purgeCache( @Nonnull JID room )
    {
        final Lock lock = EXTENSIONS_BY_ROOM.getLock(room);
        try
        {
            lock.lock();
            EXTENSIONS_BY_ROOM.remove(room);
        }
        finally
        {
            lock.unlock();
        }
    }
}
