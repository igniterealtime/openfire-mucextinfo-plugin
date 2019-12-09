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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Representation of a field, as used in data forms.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class Field implements Serializable
{
    private final String varName;

    private final String label;

    private final String[] values;

    public Field( final String varName, final String label, String... values )
    {
        this.varName = varName;
        this.label = label;
        this.values = values;
    }

    public String getVarName()
    {
        return varName;
    }

    public String getLabel()
    {
        return label;
    }

    public String[] getValues()
    {
        return values;
    }

    @Override
    public boolean equals( final Object o )
    {
        if ( this == o ) { return true; }
        if ( o == null || getClass() != o.getClass() ) { return false; }
        final Field field = (Field) o;
        return Objects.equals(varName, field.varName) &&
            Objects.equals(label, field.label) &&
            Arrays.equals(values, field.values);
    }

    @Override
    public int hashCode()
    {
        int result = Objects.hash(varName, label);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}
