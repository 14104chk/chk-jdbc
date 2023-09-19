package org.chk.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.chk.jdbc.rsm.MappingConfig;
import org.chk.jdbc.rsm.MappingContext;
import org.chk.jdbc.rsm.Property;
import org.chk.jdbc.rsm.pr.JsonPropertyReader;
import org.chk.jdbc.rsm.pr.PropertyReader;
import org.chk.jdbc.rsm.pr.PropertyReaders;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

public abstract class BaseDAO {

    @Autowired
    protected Environment env;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected TransactionTemplate transactionTemplate;

    protected <T> ResultSetExtractor<List<T>> _extractor() {
        MappingContext context = new MappingContext();
        MappingConfig config = context.getConfig();
        config.setDefaultEntityPackage(env.getProperty("chk-jdbc.mapping-context.default-entity-package"));
        PropertyReaders propertyReaders = config.getPropertyReaders();
        propertyReaders.registerPropertyReader(LocalDateTime.class, new PropertyReader() {
            @Override
            public Object read(MappingContext context, Property property, Class propertyRawType, ResultSet resultSet) throws SQLException {
                Timestamp time = resultSet.getTimestamp(property.getColumnIndex());
                return time == null ? null : time.toLocalDateTime();
            }
        });
        Json json = Json.newInstance();
        propertyReaders.registerPropertyReader(List.class, new JsonPropertyReader(json));
        propertyReaders.registerPropertyReader(Map.class, new JsonPropertyReader(json));
        return (ResultSetExtractor) context::mapping;
    }

    protected JdbcExecutor _queryList(String sql) {
        return new JdbcExecutor(sql, psc -> jdbcTemplate.query(psc, _extractor()));
    }

    protected JdbcExecutor _queryOne(String sql) {
        return new JdbcExecutor(sql, psc -> {
            List list = jdbcTemplate.query(psc, _extractor());
            if (list == null || list.isEmpty()) {
                return null;
            } else if (list.size() > 1) {
                throw new SimpleDataAccessException("Resultset size > 1");
            } else {
                return list.get(0);
            }
        });
    }

    protected JdbcExecutor _queryInt(String sql) {
        return new JdbcExecutor(sql, psc -> jdbcTemplate.query(psc, (rs) -> rs.next() ? rs.getInt(1) : 0));
    }

    protected <T> Page<T> _queryPageForMySQL(String sql, BaseQueryCondition cdt) {
        return jdbcTemplate.execute((Connection conn) -> {
            String _sql;
            if (sql.toUpperCase().contains("SQL_CALC_FOUND_ROWS")) {
                _sql = sql;
            } else {
                _sql = sql.replaceFirst("^(?i)select ", "SELECT SQL_CALC_FOUND_ROWS ");
            }
            SortBuilder sb = cdt.getSortBuilder();
            if (sb != null) {
                _sql = _sql.replace("{sort}", sb.toSQLFromKey(cdt.getSort()));
            }
            List context = new JdbcExecutor(_sql, psc -> {
                try (PreparedStatement pstmt = psc.createPreparedStatement(conn)) {
                    try (ResultSet rs = pstmt.executeQuery()) {
                        return this.<T>_extractor().extractData(rs);
                    }
                } catch (SQLException ex) {
                    throw new SimpleDataAccessException(sql, ex);
                }
            }).executeWithBean(cdt);
            int total = 0;
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT FOUND_ROWS()")) {
                    if (rs.next()) {
                        total = rs.getInt(1);
                    }
                }
            }
            return newPage(total, total, context);
        });
    }

    protected <T> Page<T> newPage(int number, int total, List<T> content) {
        return new Page<>(number, total, content);
    }

    protected JdbcExecutor _update(String sql) {
        return new JdbcExecutor(sql, jdbcTemplate::update);
    }

    protected JdbcExecutor _insertAndSetKey(String sql, FailableConsumer<KeyHolder, DataAccessException> consumer) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        return new JdbcExecutor(sql, psc -> {
            int rows = jdbcTemplate.update(psc, keyHolder);
            consumer.accept(keyHolder);
            return rows;
        }, (conn, nativeSQL) -> conn.prepareStatement(nativeSQL, Statement.RETURN_GENERATED_KEYS));
    }

    protected JdbcExecutor _insertAndSetIntegerKey(String sql, Consumer<Integer> consumer) {
        return _insertAndSetKey(sql, h -> {
            List<Map<String, Object>> list = h.getKeyList();
            if (list.isEmpty()) {
                return;
            }
            while (list.size() > 1) {
                list.remove(list.size() - 1);
            }
            consumer.accept(h.getKey().intValue());
        });
    }

    protected <V> V doInTransactionTask(FailableFunction<TransactionStatus, V, Exception> function) {
        return transactionTemplate.execute(status -> {
            try {
                return function.apply(status);
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Exception ex) {
                LoggerFactory.getLogger(getClass()).error(null, ex);
                throw new RuntimeException(null, ex);
            }
        });
    }

    protected void doInTransactionAction(FailableConsumer<TransactionStatus, Exception> consumer) {
        doInTransactionTask((status) -> {
            consumer.accept(status);
            return null;
        });
    }
}
