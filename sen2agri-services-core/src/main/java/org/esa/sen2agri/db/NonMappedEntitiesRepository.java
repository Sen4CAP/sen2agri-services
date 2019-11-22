/*
 * Copyright (C) 2018 CS ROMANIA
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.sen2agri.db;

import org.esa.sen2agri.entities.ProductCount;
import org.esa.sen2agri.entities.ProductDetails;
import org.esa.sen2agri.entities.SiteTiles;
import org.esa.sen2agri.entities.Task;
import org.esa.sen2agri.entities.converters.ActivityStatusConverter;
import org.esa.sen2agri.entities.enums.Satellite;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import ro.cs.tao.EnumUtils;

import javax.sql.DataSource;
import java.sql.Date;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Repository-like class for handling entities not mapped in the orm.xml file.
 *
 * @author Cosmin Cara
 */
class NonMappedEntitiesRepository {

    private PersistenceManager persistenceManager;

    NonMappedEntitiesRepository(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    SiteTiles getSiteTiles(short siteId, short satelliteId) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        SiteTiles siteTiles = new SiteTiles();
        try (Connection conn = dataSource.getConnection()) {
            jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement =
                                connection.prepareStatement("SELECT site_id, satellite_id, tiles FROM site_tiles " +
                                                                    "WHERE site_id = ? AND satellite_id = ?");
                        statement.setShort(1, siteId);
                        statement.setShort(2, satelliteId);
                        return statement;
                    },
                    (resultSet, i) -> {
                        siteTiles.setSiteId(resultSet.getShort(1));
                        siteTiles.setSatellite(EnumUtils.getEnumConstantByValue(Satellite.class, resultSet.getShort(2)));
                        siteTiles.setTiles((String[]) resultSet.getArray(3).getArray());
                        return siteTiles;
                    });
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return siteTiles;
    }

    List<ProductCount> getEstimatedProductCount(int siteId, int satelliteId) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            return jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement =
                                connection.prepareStatement("SELECT site_id, satellite_id, start_date, end_date, product_count FROM downloader_count " +
                                                                    "WHERE site_id = ? AND satellite_id = ?");
                        statement.setInt(1, siteId);
                        statement.setInt(2, satelliteId);
                        return statement;
                    },
                    (resultSet, i) -> {
                        ProductCount productCount = new ProductCount();
                        productCount.setSiteId(resultSet.getShort(1));
                        productCount.setSatellite(EnumUtils.getEnumConstantByValue(Satellite.class, resultSet.getShort(2)));
                        productCount.setStartDate(resultSet.getDate(3).toLocalDate());
                        productCount.setEndDate(resultSet.getDate(4).toLocalDate());
                        productCount.setCount(resultSet.getInt(5));
                        return productCount;
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    ProductDetails getProductStatistics(int productId) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection conn = dataSource.getConnection()){
            return jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement =
                                connection.prepareStatement("SELECT product_id, min_value, max_value, mean_value, std_dev, histogram FROM product_stats " +
                                                                    "WHERE product_id = ?");
                        statement.setInt(1, productId);
                        return statement;
                    }, resultSet -> {
                        ProductDetails productDetails = null;
                        if (resultSet.isFirst()) {
                            do {
                                productDetails = new ProductDetails();
                                productDetails.setId(resultSet.getInt(1));
                                productDetails.setMinValue(resultSet.getDouble(2));
                                productDetails.setMaxValue(resultSet.getDouble(3));
                                productDetails.setMeanValue(resultSet.getDouble(4));
                                productDetails.setStdDevValue(resultSet.getDouble(5));
                                Array array = resultSet.getArray(6);
                                if (array != null) {
                                    productDetails.setHistogram((Integer[]) array.getArray());
                                }
                            } while (resultSet.next());
                        }
                        return productDetails;
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    List<ProductDetails> getStatisticsForProducts(Set<Integer> productIds) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection conn = dataSource.getConnection()) {
            return jdbcTemplate.query(
                    connection -> {
                        PreparedStatement statement =
                                connection.prepareStatement("SELECT product_id, min_value, max_value, mean_value, std_dev, histogram FROM product_stats " +
                                                                    "WHERE product_id IN (?)");
                        statement.setString(1, productIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
                        return statement;
                    },
                    (resultSet, i) -> {
                        ProductDetails productDetails = new ProductDetails();
                        productDetails.setId(resultSet.getInt(1));
                        productDetails.setMinValue(resultSet.getDouble(2));
                        productDetails.setMaxValue(resultSet.getDouble(3));
                        productDetails.setMeanValue(resultSet.getDouble(4));
                        productDetails.setStdDevValue(resultSet.getDouble(5));
                        Array array = resultSet.getArray(6);
                        if (array != null) {
                            productDetails.setHistogram((Integer[]) array.getArray());
                        }
                        return productDetails;
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    Task getTask(int jobId, String name) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.queryForObject(String.format("SELECT id, job_id, module_short_name, parameters, submit_timestamp, start_timestamp, end_timestamp, " +
                                                                 "status_id, status_timestamp, preceding_task_ids FROM task WHERE job_id = %d AND module_short_name = '%s'",
                                                         jobId, name),
                                           (rs, i) -> {
                                               Task task = new Task();
                                               task.setId(rs.getInt(1));
                                               task.setJobId(rs.getInt(2));
                                               task.setModuleShortName(rs.getString(3));
                                               task.setParameters(rs.getString(4));
                                               Timestamp timestamp = rs.getTimestamp(5);
                                               task.setSubmitTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);
                                               timestamp = rs.getTimestamp(6);
                                               task.setStartTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);
                                               timestamp = rs.getTimestamp(7);
                                               task.setEndTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);
                                               task.setStatus(new ActivityStatusConverter().convertToEntityAttribute(rs.getInt(8)));
                                               timestamp = rs.getTimestamp(9);
                                               task.setStatusTimestamp(timestamp != null ? timestamp.toLocalDateTime() : null);
                                               Array array = rs.getArray(10);
                                               if (array != null) {
                                                   task.setPrecedingTasks(Arrays.stream(((Integer[])array.getArray())).mapToInt(Integer::intValue).toArray());
                                               }
                                               return task;
                                           });
    }

    void save(ProductDetails productDetails) throws SQLException {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            jdbcTemplate.batchUpdate("WITH upsert AS ( UPDATE product_stats SET min_value = ?, max_value = ?, mean_value = ?, std_dev = ?, histogram = ? " +
                                             "WHERE product_id = ? RETURNING *) " +
                                             "INSERT INTO product_stats (product_id, min_value, max_value, mean_value, std_dev, histogram) " +
                                             "SELECT ?, ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT * FROM upsert);",
                                     new BatchPreparedStatementSetter() {
                                         @Override
                                         public void setValues(PreparedStatement preparedStatement, int row) throws SQLException {
                                             preparedStatement.setDouble(1, productDetails.getMinValue());
                                             preparedStatement.setDouble(2, productDetails.getMaxValue());
                                             preparedStatement.setDouble(3, productDetails.getMeanValue());
                                             preparedStatement.setDouble(4, productDetails.getStdDevValue());
                                             Array array = null;
                                             if (productDetails.getHistogram() != null) {
                                                 array = connection.createArrayOf("integer", productDetails.getHistogram());
                                             }
                                             preparedStatement.setArray(5, array);
                                             preparedStatement.setInt(6, productDetails.getId());
                                             preparedStatement.setInt(7, productDetails.getId());
                                             preparedStatement.setDouble(8, productDetails.getMinValue());
                                             preparedStatement.setDouble(9, productDetails.getMaxValue());
                                             preparedStatement.setDouble(10, productDetails.getMeanValue());
                                             preparedStatement.setDouble(11, productDetails.getStdDevValue());
                                             preparedStatement.setArray(12, array);
                                         }

                                         @Override
                                         public int getBatchSize() {
                                             return 1;
                                         }
                                     });
        }
    }

    Task save(Task task) {
        DataSource dataSource = persistenceManager.getDataSource();
        SimpleJdbcInsert jdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("task").usingGeneratedKeyColumns("id");
        try(Connection connection = dataSource.getConnection()) {
            task.setId((int) jdbcInsert.executeAndReturnKey(new HashMap<String, Object>() {{
                put("job_id", task.getJobId());
                put("module_short_name", task.getModuleShortName());
                put("parameters", task.getParameters());
                LocalDateTime dateTime = task.getSubmitTimestamp();
                put("submit_timestamp", dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = task.getStartTimestamp();
                put("start_timestamp", dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = task.getEndTimestamp();
                put("end_timestamp", dateTime != null ? Timestamp.valueOf(dateTime) : null);
                put("status_id", new ActivityStatusConverter().convertToDatabaseColumn(task.getStatus()));
                dateTime = task.getStatusTimestamp();
                put("status_timestamp", dateTime != null ? Timestamp.valueOf(dateTime) : null);
                int[] ids = task.getPrecedingTasks();
                Array array = null;
                if (ids != null && ids.length > 0) {
                    final Integer[] integers = IntStream.of(ids).boxed().toArray(Integer[]::new);
                    array = connection.createArrayOf("integer", integers);
                }
                put("preceding_task_ids", array);
            }}));
        } catch (SQLException e ) {
            e.printStackTrace();
        }
        return task;
    }

    Task update(Task task) throws SQLException {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            jdbcTemplate.batchUpdate("UPDATE task SET module_short_name = ?, parameters = ?, submit_timestamp = ?, start_timestamp = ?, end_timestamp = ?, " +
                                             "status_id = ?, status_timestamp = ?, preceding_task_ids = ? " +
                                             "WHERE id = ?",
                                     new BatchPreparedStatementSetter() {
                                         @Override
                                         public void setValues(PreparedStatement preparedStatement, int row) throws SQLException {
                                             preparedStatement.setString(1, task.getModuleShortName());
                                             preparedStatement.setString(2, task.getParameters());
                                             LocalDateTime dateTime = task.getSubmitTimestamp();
                                             preparedStatement.setTimestamp(3, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                                             dateTime = task.getStartTimestamp();
                                             preparedStatement.setTimestamp(4, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                                             dateTime = task.getEndTimestamp();
                                             preparedStatement.setTimestamp(5, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                                             preparedStatement.setInt(6, new ActivityStatusConverter().convertToDatabaseColumn(task.getStatus()));
                                             dateTime = task.getStatusTimestamp();
                                             preparedStatement.setTimestamp(7, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                                             Array array = null;
                                             int[] ids = task.getPrecedingTasks();
                                             if (ids != null && ids.length > 0) {
                                                 final Integer[] integers = IntStream.of(ids).boxed().toArray(Integer[]::new);
                                                 array = connection.createArrayOf("integer", integers);
                                             }
                                             preparedStatement.setArray(8, array);
                                             preparedStatement.setInt(9, task.getId());
                                         }

                                         @Override
                                         public int getBatchSize() {
                                             return 1;
                                         }
                                     });
        }
        return task;
    }

    void save(ProductCount productCount) throws SQLException {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        try (Connection connection = dataSource.getConnection()) {
            jdbcTemplate.batchUpdate("WITH upsert AS ( UPDATE downloader_count SET product_count = ?, last_updated = ? " +
                                             "WHERE site_id = ? AND satellite_id = ? AND start_date = ? AND end_date = ? RETURNING *) " +
                                             "INSERT INTO downloader_count (site_id, satellite_id, start_date, end_date, product_count) " +
                                             "SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT * FROM upsert);",
                                     new BatchPreparedStatementSetter() {
                                         @Override
                                         public void setValues(PreparedStatement preparedStatement, int row) throws SQLException {
                                             preparedStatement.setInt(1, productCount.getCount());
                                             preparedStatement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                                             preparedStatement.setShort(3, productCount.getSiteId());
                                             preparedStatement.setShort(4, productCount.getSatellite().value());
                                             preparedStatement.setDate(5, Date.valueOf(productCount.getStartDate()));
                                             preparedStatement.setDate(6, Date.valueOf(productCount.getEndDate()));
                                             preparedStatement.setShort(7, productCount.getSiteId());
                                             preparedStatement.setShort(8, productCount.getSatellite().value());
                                             preparedStatement.setDate(9, Date.valueOf(productCount.getStartDate()));
                                             preparedStatement.setDate(10, Date.valueOf(productCount.getEndDate()));
                                             preparedStatement.setInt(11, productCount.getCount());
                                         }

                                         @Override
                                         public int getBatchSize() {
                                             return 1;
                                         }
                                     });
        }
    }
}
