/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ceresdb;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;

import com.ceresdb.common.OptKeys;
import com.ceresdb.common.util.SystemPropertyUtil;
import com.ceresdb.models.Record;

/**
 * Map byte[] to Record by avro.
 *
 * @author jiachun.fjc
 */
public class RecordMapper extends AvroMapper implements Function<byte[], Record> {

    private static final boolean NAME_VALIDATE = SystemPropertyUtil.getBool(OptKeys.NAME_VALIDATE, false);

    RecordMapper(Schema schema) {
        super(schema);
    }

    public static RecordMapper getMapper(final String s) {
        final Schema.Parser parser = new Schema.Parser();
        parser.setValidate(NAME_VALIDATE);
        final Schema schema = parser.parse(s);
        return new RecordMapper(schema);
    }

    @Override
    public Record apply(final byte[] bytes) {
        return new DefaultRecord(mapTo(bytes));
    }

    static final class DefaultRecord implements Record {

        private final GenericRecord gr;

        DefaultRecord(GenericRecord gr) {
            this.gr = gr;
        }

        @Override
        public Object get(final String key) {
            return mayCast(this.gr.get(key));
        }

        @Override
        public Object get(final int i) {
            return mayCast(this.gr.get(i));
        }

        @Override
        public boolean hasField(final String key) {
            return this.gr.hasField(key);
        }

        @Override
        public int getFieldCount() {
            return this.gr.getSchema().getFields().size();
        }

        @Override
        public List<FieldDescriptor> getFieldDescriptors() {
            return this.gr.getSchema() //
                    .getFields() //
                    .stream() //
                    .map(DefaultRecord::parseFd) //
                    .collect(Collectors.toList());
        }

        @Override
        public Optional<FieldDescriptor> getFieldDescriptor(final String field) {
            return Optional.ofNullable(parseFd(this.gr.getSchema().getField(field)));
        }

        private static FieldDescriptor parseFd(final Schema.Field f) {
            if (f == null) {
                return null;
            }
            final Schema sc = f.schema();
            final FieldType type = parseType(sc);
            final List<FieldType> subTypes = parseSubTypes(type.getType(), sc);
            return new FieldDescriptor(f.name(), type, subTypes);
        }

        private static FieldType parseType(final Schema sc) {
            final LogicalType logicalType = parseLogicalType(sc.getLogicalType());
            switch (sc.getType()) {
                case NULL:
                    return FieldType.of(Type.Null, logicalType);
                case DOUBLE:
                    return FieldType.of(Type.Double, logicalType);
                case FLOAT:
                    return FieldType.of(Type.Float, logicalType);
                case BYTES:
                    return FieldType.of(Type.Bytes, logicalType);
                case STRING:
                    return FieldType.of(Type.String, logicalType);
                case LONG:
                    return FieldType.of(Type.Long, logicalType);
                case INT:
                    return FieldType.of(Type.Int, logicalType);
                case BOOLEAN:
                    return FieldType.of(Type.Boolean, logicalType);
                case UNION:
                    return FieldType.of(Type.Union, logicalType);
                default:
                    return FieldType.of(Type.Unknown, logicalType);
            }
        }

        private static List<FieldType> parseSubTypes(final Type type, final Schema schema) {
            return type != Type.Union //
                    ? Collections.emptyList() //
                    : schema.getTypes().stream().map(DefaultRecord::parseType).collect(Collectors.toList());
        }

        private static LogicalType parseLogicalType(final org.apache.avro.LogicalType lt) {
            if (lt == null) {
                return LogicalType.Null;
            }

            if (lt instanceof LogicalTypes.TimestampMillis) {
                return LogicalType.TimestampMillis;
            }

            if (lt instanceof LogicalTypes.TimestampMicros) {
                return LogicalType.TimestampMicros;
            }

            return LogicalType.Unknown;
        }

        private static Object mayCast(final Object obj) {
            if (obj instanceof Utf8) {
                return ((Utf8) obj).toString();
            }
            if (obj instanceof ByteBuffer) {
                return getBytes((ByteBuffer) obj);
            }
            return obj;
        }

        private static byte[] getBytes(final ByteBuffer buf) {
            if (buf.hasArray()) {
                return buf.array();
            }
            final byte[] bytes = new byte[buf.remaining()];
            buf.get(bytes);
            return bytes;
        }
    }
}
