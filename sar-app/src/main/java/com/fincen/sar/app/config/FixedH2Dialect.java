package com.fincen.sar.app.config;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;

import java.sql.Types;

/**
 * H2 2.2+ removed TINYINT; Hibernate Envers uses it for the revtype column.
 * This dialect remaps TINYINT → SMALLINT so Envers audit tables can be created.
 */
public class FixedH2Dialect extends H2Dialect {

    public FixedH2Dialect(DialectResolutionInfo info) {
        super(info);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (sqlTypeCode == Types.TINYINT) {
            return "smallint";
        }
        return super.columnType(sqlTypeCode);
    }
}
