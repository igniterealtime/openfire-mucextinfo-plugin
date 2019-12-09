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
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a data form, which is used as an extension element to information returned in response to service
 * discovery request.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class ExtDataForm implements Serializable
{
    private final String formTypeName;

    private final ArrayList<Field> fields = new ArrayList<>();

    public ExtDataForm( final String formTypeName ) {this.formTypeName = formTypeName;}

    public String getFormTypeName()
    {
        return formTypeName;
    }

    public List<Field> getFields()
    {
        return fields;
    }
}
