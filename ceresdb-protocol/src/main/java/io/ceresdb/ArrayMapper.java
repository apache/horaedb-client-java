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
package io.ceresdb;

import java.util.List;
import java.util.function.Function;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

/**
 * Map byte[] to Object[] by avro.
 *
 * @author jiachun.fjc
 */
public class ArrayMapper extends AvroMapper implements Function<byte[], Object[]> {

    ArrayMapper(Schema schema) {
        super(schema);
    }

    public static ArrayMapper getMapper(final String s) {
        final Schema schema = new Schema.Parser().parse(s);
        return new ArrayMapper(schema);
    }

    @Override
    public Object[] apply(final byte[] bytes) {
        final GenericRecord record = mapTo(bytes);
        final List<Schema.Field> fields = record.getSchema().getFields();
        final Object[] objects = new Object[fields.size()];
        int i = 0;
        for (final Schema.Field f : fields) {
            objects[i++] = record.get(f.name());
        }
        return objects;
    }
}
