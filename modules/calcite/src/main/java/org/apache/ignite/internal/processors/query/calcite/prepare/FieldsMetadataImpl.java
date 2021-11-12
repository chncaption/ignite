/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.query.calcite.prepare;

import java.util.List;
import org.apache.calcite.rel.type.RelDataType;

/**
 * FieldsMetadataImpl.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
public class FieldsMetadataImpl implements FieldsMetadata {
    private final RelDataType rowType;

    private final List<List<String>> origins;

    /**
     * Constructor.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public FieldsMetadataImpl(RelDataType rowType, List<List<String>> origins) {
        this.rowType = rowType;
        this.origins = origins;
    }

    /** {@inheritDoc} */
    @Override
    public RelDataType rowType() {
        return rowType;
    }

    /** {@inheritDoc} */
    @Override
    public List<List<String>> origins() {
        return origins;
    }
}