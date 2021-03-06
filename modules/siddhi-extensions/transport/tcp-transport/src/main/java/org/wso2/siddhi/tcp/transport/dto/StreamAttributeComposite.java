/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.siddhi.tcp.transport.dto;

import org.wso2.siddhi.tcp.transport.utils.EventDefinitionConverterUtil;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;

/**
 * Class to hold attribute type order array and size of attributes.
 */

public class StreamAttributeComposite {
    private Attribute.Type[] attributeTypes;
    private StreamDefinition streamDefinition;
    private int attributeSize;

    public StreamAttributeComposite(StreamDefinition streamDefinition) {
        this.streamDefinition = streamDefinition;
        this.attributeTypes = EventDefinitionConverterUtil.generateAttributeTypeArray(streamDefinition.getAttributeList());
        this.attributeSize = getSize(attributeTypes);

    }

    private int getSize(Attribute.Type[] attributeTypes) {
        int size = 0;
        if (attributeTypes != null) {
            size = attributeTypes.length;
        }
        return size;
    }

    public int getAttributeSize() {
        return attributeSize;
    }

    public void setAttributeSize(int attributeSize) {
        this.attributeSize = attributeSize;
    }

    public Attribute.Type[] getAttributeTypes() {
        return attributeTypes;
    }

    public void setAttributeTypes(Attribute.Type[] attributeTypes) {
        this.attributeTypes = attributeTypes;
    }

    public StreamDefinition getStreamDefinition() {
        return streamDefinition;
    }
}
