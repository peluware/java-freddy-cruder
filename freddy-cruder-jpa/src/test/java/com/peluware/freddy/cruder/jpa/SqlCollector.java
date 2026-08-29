package com.peluware.freddy.cruder.jpa;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Hibernate lo instancia por nombre de clase, de ahí que lo recolectado sea estático. */
public class SqlCollector implements StatementInspector {

    private static final List<String> STATEMENTS = new CopyOnWriteArrayList<>();

    @Override
    public String inspect(String sql) {
        STATEMENTS.add(sql);
        return sql;
    }

    static void clear() {
        STATEMENTS.clear();
    }

    static String lastSelect() {
        return STATEMENTS.stream()
            .filter(sql -> sql.stripLeading().toLowerCase().startsWith("select"))
            .reduce((first, second) -> second)
            .orElseThrow(() -> new AssertionError("No se capturó ningún SELECT"));
    }
}
