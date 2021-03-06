/*
 * Copyright 2004-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.core.test.formatter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.compass.core.CompassSession;
import org.compass.core.CompassTransaction;
import org.compass.core.Resource;
import org.compass.core.config.CompassEnvironment;
import org.compass.core.config.CompassSettings;
import org.compass.core.converter.basic.DateConverter;
import org.compass.core.test.AbstractTestCase;

/**
 * @author kimchy
 */
public class FormatterTests extends AbstractTestCase {

    protected String[] getMappings() {
        return new String[]{"formatter/formatter.cpm.xml"};
    }

    protected void addSettings(CompassSettings settings) {
        Locale.setDefault(Locale.ENGLISH);
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "myint",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.INTEGER, "0,000"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "myshort",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.SHORT, "0,000"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "mylong",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.LONG, "0,000"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "mybigdecimal",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.BIGDECIMAL, "0,000.00"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "mybiginteger",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.BIGINTEGER, "0,000"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "myfloat",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.FLOAT, "0,000.00"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "mydouble",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.DOUBLE, "0,000.00"});
        settings.setGroupSettings(CompassEnvironment.Converter.PREFIX,
                "mydate",
                new String[]{CompassEnvironment.Converter.TYPE, CompassEnvironment.Converter.Format.FORMAT},
                new String[]{CompassEnvironment.Converter.DefaultTypes.Simple.DATE, "yyyy-MM-dd-HH"});
    }

    public void testNumberFormatUsingLookupConverters() {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Long id = new Long(1);
        A a = new A();
        a.setId(id);
        a.setIntVal(12);
        a.setShortVal((short) 12);
        a.setLongVal(12);
        a.setDoubleVal(12.56789);
        a.setFloatVal(12.56789f);
        Date date = new Date();
        a.setDateVal(date);
        a.setBigIntegerVal(new BigInteger("12"));
        BigDecimal bigDecimal = new BigDecimal(12.56789d);
        a.setBigDecimalVal(bigDecimal);

        session.save("a", a);

        Resource r = session.loadResource("a", id);
        assertEquals("12", r.get("intSimple"));
        assertEquals("0,012", r.get("intFormatted"));
        assertEquals("12", r.get("shortSimple"));
        assertEquals("0,012", r.get("shortFormatted"));
        assertEquals("12", r.get("longSimple"));
        assertEquals("0,012", r.get("longFormatted"));
        assertEquals("12", r.get("bigIntegerSimple"));
        assertEquals("0,012", r.get("bigIntegerFormatted"));
        assertEquals("12.56789", r.get("doubleSimple"));
        assertEquals("0,012.57", r.get("doubleFormatted"));
        assertEquals("12.56789", r.get("floatSimple"));
        assertEquals("0,012.57", r.get("floatFormatted"));
        assertEquals(bigDecimal.toString(), r.get("bigDecimalSimple"));
        assertEquals("0,012.57", r.get("bigDecimalFormatted"));
        SimpleDateFormat sdf = new SimpleDateFormat(DateConverter.DEFAULT_DATE_FORMAT);
        assertEquals(sdf.format(date), r.get("dateSimple"));
        sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
        assertEquals(sdf.format(date), r.get("dateFormatted"));

        tr.commit();
    }

    public void testIntFormattedAsUnmarshallValue() throws Exception {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Long id = new Long(1);
        A a = new A();
        a.setId(id);
        a.setIntVal(12);
        a.setShortVal((short) 12);
        a.setLongVal(12);
        a.setDoubleVal(12.56789);
        a.setFloatVal(12.56789f);
        Date date = new Date();
        a.setDateVal(date);
        a.setTimeVal(new Time(date.getTime()));
        a.setTimestampVal(new Timestamp(date.getTime()));
        a.setBigIntegerVal(new BigInteger("12"));
        BigDecimal bigDecimal = new BigDecimal(12.56789d);
        a.setBigDecimalVal(bigDecimal);

        session.save("a1", a);

        a = (A) session.load("a1", id);
        assertEquals(12, a.getIntVal());
        assertEquals(12, a.getShortVal());
        assertEquals(12, a.getLongVal());
        assertEquals(12.57, a.getDoubleVal(), 0.0000001);
        assertEquals(12.57f, a.getFloatVal(), 0.0000001);

        tr.commit();
    }

    public void testNumberFormatUsingFormatAttribute() {
        CompassSession session = openSession();
        CompassTransaction tr = session.beginTransaction();

        Long id = new Long(1);
        A a = new A();
        a.setId(id);
        a.setIntVal(12);
        a.setShortVal((short) 12);
        a.setLongVal(12);
        a.setDoubleVal(12.56789);
        a.setFloatVal(12.56789f);
        Date date = new Date();
        a.setDateVal(date);
        a.setTimeVal(new Time(date.getTime()));
        a.setTimestampVal(new Timestamp(date.getTime()));
        a.setBigIntegerVal(new BigInteger("12"));
        BigDecimal bigDecimal = new BigDecimal(12.56789d);
        a.setBigDecimalVal(bigDecimal);

        session.save("a2", a);

        Resource r = session.loadResource("a2", id);
        assertEquals("12", r.get("intSimple"));
        assertEquals("0,012", r.get("intFormatted"));
        assertEquals("12", r.get("shortSimple"));
        assertEquals("0,012", r.get("shortFormatted"));
        assertEquals("12", r.get("longSimple"));
        assertEquals("0,012", r.get("longFormatted"));
        assertEquals("12", r.get("bigIntegerSimple"));
        assertEquals("0,012", r.get("bigIntegerFormatted"));
        assertEquals("12.56789", r.get("doubleSimple"));
        assertEquals("0,012.57", r.get("doubleFormatted"));
        assertEquals("12.56789", r.get("floatSimple"));
        assertEquals("0,012.57", r.get("floatFormatted"));
        assertEquals(bigDecimal.toString(), r.get("bigDecimalSimple"));
        assertEquals("0,012.57", r.get("bigDecimalFormatted"));
        SimpleDateFormat sdf = new SimpleDateFormat(DateConverter.DEFAULT_DATE_FORMAT);
        assertEquals(sdf.format(date), r.get("dateSimple"));
        sdf = new SimpleDateFormat("yyyy-MM-dd-HH");
        assertEquals(sdf.format(date), r.get("dateFormatted"));
        assertEquals(sdf.format(date), r.get("timeFormatted"));
        assertEquals(sdf.format(date), r.get("timestampFormatted"));

        tr.commit();
    }
}
