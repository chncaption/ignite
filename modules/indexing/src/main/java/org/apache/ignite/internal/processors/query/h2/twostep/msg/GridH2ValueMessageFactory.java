package org.apache.ignite.internal.processors.query.h2.twostep.msg;

import org.apache.ignite.internal.processors.cache.*;
import org.apache.ignite.internal.processors.query.h2.opt.*;
import org.apache.ignite.plugin.extensions.communication.*;
import org.h2.value.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * H2 Value message factory.
 */
public class GridH2ValueMessageFactory implements MessageFactory {
    /** {@inheritDoc} */
    @Nullable @Override public Message create(byte type) {
        switch (type) {
            case -4:
                return GridH2Null.INSTANCE;

            case -5:
                return new GridH2Boolean();

            case -6:
                return new GridH2Byte();

            case -7:
                return new GridH2Short();

            case -8:
                return new GridH2Integer();

            case -9:
                return new GridH2Long();

            case -10:
                return new GridH2Decimal();

            case -11:
                return new GridH2Double();

            case -12:
                return new GridH2Float();

            case -13:
                return new GridH2Time();

            case -14:
                return new GridH2Date();

            case -15:
                return new GridH2Timestamp();

            case -16:
                return new GridH2Bytes();

            case -17:
                return new GridH2String();

            case -18:
                return new GridH2Array();

            case -19:
                return new GridH2JavaObject();

            case -20:
                return new GridH2Uuid();

            case -21:
                return new GridH2Geometry();
        }

        return null;
    }

    /**
     * @param src Source values.
     * @param dst Destination collection.
     * @return Destination collection.
     */
    public static Collection<Message> toMessages(Collection<Value[]> src, Collection<Message> dst) {
        for (Value[] row : src) {
            for (Value val : row)
                dst.add(toMessage(val));
        }

        return dst;
    }

    /**
     * @param src Source iterator.
     * @param dst Array to fill with values.
     * @param coctx Cache object context.
     * @return Filled array.
     */
    public static Value[] fillArray(Iterator<Message> src, Value[] dst, CacheObjectContext coctx) {
        for (int i = 0; i < dst.length; i++) {
            Message msg = src.next();

            if (msg instanceof GridH2ValueMessage)
                dst[i] = ((GridH2ValueMessage)msg).value();
            else
                dst[i] = new GridH2ValueCacheObject(coctx, (CacheObject)msg);
        }

        return dst;
    }

    /**
     * @param v Value.
     * @return Message.
     */
    public static Message toMessage(Value v) {
        switch (v.getType()) {
            case Value.NULL:
                return GridH2Null.INSTANCE;

            case Value.BOOLEAN:
                return new GridH2Boolean(v);

            case Value.BYTE:
                return new GridH2Byte(v);

            case Value.SHORT:
                return new GridH2Short(v);

            case Value.INT:
                return new GridH2Integer(v);

            case Value.LONG:
                return new GridH2Long(v);

            case Value.DECIMAL:
                return new GridH2Decimal(v);

            case Value.DOUBLE:
                return new GridH2Double(v);

            case Value.FLOAT:
                return new GridH2Float(v);

            case Value.DATE:
                return new GridH2Date(v);

            case Value.TIME:
                return new GridH2Time(v);

            case Value.TIMESTAMP:
                return new GridH2Timestamp(v);

            case Value.BYTES:
                return new GridH2Bytes(v);

            case Value.STRING:
            case Value.STRING_FIXED:
            case Value.STRING_IGNORECASE:
                return new GridH2String(v);

            case Value.ARRAY:
                return new GridH2Array(v);

            case Value.JAVA_OBJECT:
                // TODO
//                if (v instanceof GridH2ValueCacheObject)
//                    return ((GridH2ValueCacheObject)v).getCacheObject();

                return new GridH2JavaObject(v);

            case Value.UUID:
                return new GridH2Uuid(v);

            case Value.GEOMETRY:
                return new GridH2Geometry(v);

            default:
                throw new IllegalStateException("Unsupported H2 type: " + v.getType());
        }
    }
}
