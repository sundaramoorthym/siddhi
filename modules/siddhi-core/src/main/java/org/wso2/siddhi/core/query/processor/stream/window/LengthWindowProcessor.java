/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.siddhi.core.query.processor.stream.window;

import org.wso2.siddhi.annotation.Extension;
import org.wso2.siddhi.annotation.Parameter;
import org.wso2.siddhi.annotation.ReturnAttribute;
import org.wso2.siddhi.annotation.util.DataType;
import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.state.StateEvent;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.core.event.stream.StreamEventCloner;
import org.wso2.siddhi.core.executor.ConstantExpressionExecutor;
import org.wso2.siddhi.core.executor.ExpressionExecutor;
import org.wso2.siddhi.core.executor.VariableExpressionExecutor;
import org.wso2.siddhi.core.query.processor.Processor;
import org.wso2.siddhi.core.table.EventTable;
import org.wso2.siddhi.core.util.collection.operator.Finder;
import org.wso2.siddhi.core.util.collection.operator.MatchingMetaStateHolder;
import org.wso2.siddhi.core.util.parser.OperatorParser;
import org.wso2.siddhi.query.api.exception.ExecutionPlanValidationException;
import org.wso2.siddhi.query.api.expression.Expression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension(
        name = "length",
        namespace = "",
        description = "A sliding length window that holds the last windowLength events at a given time, " +
                "and gets updated for each arrival and expiry.",
        parameters = {
                @Parameter(name = "windowLength",
                        description = "The number of events that should be included in a sliding length window.",
                        type = {DataType.INT})
        },
        returnAttributes = @ReturnAttribute(
                description = "Returns current and expired events.",
                type = {})
)
public class LengthWindowProcessor extends WindowProcessor implements FindableProcessor {

    private int length;
    private int count = 0;
    private ComplexEventChunk<StreamEvent> expiredEventChunk;

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Override
    protected void init(ExpressionExecutor[] attributeExpressionExecutors, ExecutionPlanContext executionPlanContext) {
        expiredEventChunk = new ComplexEventChunk<StreamEvent>(false);
        if (attributeExpressionExecutors.length == 1) {
            length = (Integer) ((ConstantExpressionExecutor) attributeExpressionExecutors[0]).getValue();
        } else {
            throw new ExecutionPlanValidationException("Length window should only have one parameter (<int> windowLength), but found " + attributeExpressionExecutors.length + " input attributes");
        }
    }

    @Override
    protected void process(ComplexEventChunk<StreamEvent> streamEventChunk, Processor nextProcessor, StreamEventCloner streamEventCloner) {
        synchronized (this) {
            long currentTime = executionPlanContext.getTimestampGenerator().currentTime();
            while (streamEventChunk.hasNext()) {
                StreamEvent streamEvent = streamEventChunk.next();
                StreamEvent clonedEvent = streamEventCloner.copyStreamEvent(streamEvent);
                clonedEvent.setType(StreamEvent.Type.EXPIRED);
                if (count < length) {
                    count++;
                    this.expiredEventChunk.add(clonedEvent);
                } else {
                    StreamEvent firstEvent = this.expiredEventChunk.poll();
                    if (firstEvent != null) {
                        firstEvent.setTimestamp(currentTime);
                        streamEventChunk.insertBeforeCurrent(firstEvent);
                        this.expiredEventChunk.add(clonedEvent);
                    } else {
                        streamEventChunk.insertBeforeCurrent(clonedEvent);
                    }
                }
            }
        }
        nextProcessor.process(streamEventChunk);
    }

    @Override
    public synchronized StreamEvent find(StateEvent matchingEvent, Finder finder) {
        return finder.find(matchingEvent, expiredEventChunk, streamEventCloner);
    }

    @Override
    public Finder constructFinder(Expression expression, MatchingMetaStateHolder matchingMetaStateHolder, ExecutionPlanContext executionPlanContext,
                                  List<VariableExpressionExecutor> variableExpressionExecutors, Map<String, EventTable> eventTableMap) {
        return OperatorParser.constructOperator(expiredEventChunk, expression, matchingMetaStateHolder, executionPlanContext, variableExpressionExecutors, eventTableMap, queryName);
    }

    @Override
    public void start() {
        //Do nothing
    }

    @Override
    public void stop() {
        //Do nothing
    }


    @Override
    public Map<String, Object> currentState() {
        Map<String, Object> state = new HashMap<>();
        state.put("Count", count);
        state.put("ExpiredEventChunk", expiredEventChunk.getFirst());
        return state;
    }


    @Override
    public void restoreState(Map<String, Object> state) {
        count = (int) state.get("Count");
        expiredEventChunk.clear();
        expiredEventChunk.add((StreamEvent) state.get("ExpiredEventChunk"));
    }
}
