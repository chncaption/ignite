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

package org.apache.ignite.internal.sql.engine.exec.rel;

import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.ignite.internal.sql.engine.exec.ArrayRowHandler;
import org.apache.ignite.internal.sql.engine.exec.ExecutionContext;
import org.apache.ignite.internal.sql.engine.exec.QueryTaskExecutorImpl;
import org.apache.ignite.internal.sql.engine.exec.RowHandler;
import org.apache.ignite.internal.sql.engine.metadata.FragmentDescription;
import org.apache.ignite.internal.sql.engine.util.BaseQueryContext;
import org.apache.ignite.internal.testframework.IgniteAbstractTest;
import org.apache.ignite.internal.testframework.IgniteTestUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * AbstractExecutionTest.
 * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
 */
public class AbstractExecutionTest extends IgniteAbstractTest {
    private Throwable lastE;

    private QueryTaskExecutorImpl taskExecutor;

    @BeforeEach
    public void beforeTest() {
        taskExecutor = new QueryTaskExecutorImpl("no_node");
        taskExecutor.start();
    }

    /**
     * AfterTest.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    @AfterEach
    public void afterTest() {
        taskExecutor.stop();

        if (lastE != null) {
            throw new AssertionError(lastE);
        }
    }

    protected ExecutionContext<Object[]> executionContext() {
        FragmentDescription fragmentDesc = new FragmentDescription(0, null, null, null);
        return new ExecutionContext<>(
                BaseQueryContext.builder()
                        .logger(log)
                        .build(),
                taskExecutor,
                UUID.randomUUID(),
                "fake-test-node",
                "fake-test-node",
                0,
                fragmentDesc,
                ArrayRowHandler.INSTANCE,
                ImmutableMap.of()
        );
    }

    private void handle(Thread t, Throwable ex) {
        log.error(ex.getMessage(), ex);
        lastE = ex;
    }

    protected Object[] row(Object... fields) {
        return fields;
    }

    /**
     * TestTable.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public static class TestTable implements Iterable<Object[]> {
        private int rowsCnt;

        private RelDataType rowType;

        private Function<Integer, Object>[] fieldCreators;

        TestTable(int rowsCnt, RelDataType rowType) {
            this(
                    rowsCnt,
                    rowType,
                    rowType.getFieldList().stream()
                            .map((Function<RelDataTypeField, Function<Integer, Object>>) (t) -> {
                                switch (t.getType().getSqlTypeName().getFamily()) {
                                    case NUMERIC:
                                        return TestTable::intField;

                                    case CHARACTER:
                                        return TestTable::stringField;

                                    default:
                                        assert false : "Not supported type for test: " + t;
                                        return null;
                                }
                            })
                            .collect(Collectors.toList()).toArray(new Function[rowType.getFieldCount()])
            );
        }

        TestTable(int rowsCnt, RelDataType rowType, Function<Integer, Object>... fieldCreators) {
            this.rowsCnt = rowsCnt;
            this.rowType = rowType;
            this.fieldCreators = fieldCreators;
        }

        private static Object field(Integer rowNum) {
            return "val_" + rowNum;
        }

        private static Object stringField(Integer rowNum) {
            return "val_" + rowNum;
        }

        private static Object intField(Integer rowNum) {
            return rowNum;
        }

        private Object[] createRow(int rowNum) {
            Object[] row = new Object[rowType.getFieldCount()];

            for (int i = 0; i < fieldCreators.length; ++i) {
                row[i] = fieldCreators[i].apply(rowNum);
            }

            return row;
        }

        /** {@inheritDoc} */
        @NotNull
        @Override
        public Iterator<Object[]> iterator() {
            return new Iterator<Object[]>() {
                private int curRow;

                @Override
                public boolean hasNext() {
                    return curRow < rowsCnt;
                }

                @Override
                public Object[] next() {
                    return createRow(curRow++);
                }
            };
        }
    }

    /**
     * RootRewindable.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     */
    public static class RootRewindable<RowT> extends RootNode<RowT> {
        /**
         * Constructor.
         * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
         */
        public RootRewindable(ExecutionContext<RowT> ctx, RelDataType rowType) {
            super(ctx, rowType);
        }

        /** {@inheritDoc} */
        @Override
        protected void rewindInternal() {
            IgniteTestUtils.setFieldValue(this, RootNode.class, "waiting", 0);
            IgniteTestUtils.setFieldValue(this, RootNode.class, "closed", false);
        }

        /** {@inheritDoc} */
        @Override
        public void closeInternal() {
            // No-op
        }

        /**
         * CloseRewindableRoot.
         * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
         */
        public void closeRewindableRoot() {
            super.closeInternal();
        }
    }

    protected RowHandler.RowFactory<Object[]> rowFactory() {
        return new RowHandler.RowFactory<>() {
            @Override
            public RowHandler<Object[]> handler() {
                return ArrayRowHandler.INSTANCE;
            }

            @Override
            public Object[] create() {
                throw new AssertionError();
            }

            @Override
            public Object[] create(Object... fields) {
                return fields;
            }
        };
    }
}