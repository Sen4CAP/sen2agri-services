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

import org.esa.sen2agri.entities.Step;
import org.esa.sen2agri.entities.converters.ActivityStatusConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

public class StepRepository extends NonMappedRepository<Step> {

    StepRepository(PersistenceManager persistenceManager) {
        super(persistenceManager);
    }

    public List<Step> getTaskSteps(int taskId) {
        return new StepTemplate() {
            @Override
            protected String conditionsSQL() {
                return "WHERE task_id=?";
            }

            @Override
            protected void mapParameters(PreparedStatement statement) throws SQLException {
                statement.setInt(1, taskId);
            }
        }.list();
    }

    public Step getStep(int taskId, String name) {
        return new StepTemplate() {
            @Override
            protected String conditionsSQL() {
                return "WHERE task_id=? and name=?";
            }

            @Override
            protected void mapParameters(PreparedStatement statement) throws SQLException {
                statement.setInt(1, taskId);
                statement.setString(2, name);
            }
        }.single();
    }

    public int saveLog(String stepName, int taskId, String nodeName, long duration, String log, String error) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.update("INSERT INTO step_resource_log (step_name, task_id, node_name, entry_timestamp, duration_ms, stdout_text, stderr_text) " +
                                           "VALUES (?,?,?,?,?,?,?)",
                                   stepName, taskId, nodeName, Timestamp.valueOf(LocalDateTime.now()), duration, log, error);
    }

    public Step save(Step step) {
        DataSource dataSource = persistenceManager.getDataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        final boolean isUpdate = getStep(step.getTaskId(), step.getName()) != null;
        final int result = jdbcTemplate.update(connection -> {
            final PreparedStatement statement;
            if (isUpdate) {
                statement = connection.prepareStatement(updateQuery());
                statement.setString(1, step.getParameters());
                LocalDateTime dateTime = step.getSubmitTimestamp();
                statement.setTimestamp(2, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = step.getStartTimestamp();
                statement.setTimestamp(3, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = step.getEndTimestamp();
                statement.setTimestamp(4, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                statement.setInt(5, step.getExitCode());
                statement.setInt(6, step.getStatus().value());
                dateTime = step.getStatusTimestamp();
                statement.setTimestamp(7, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                statement.setString(8, step.getName());
                statement.setInt(9, step.getTaskId());
            } else {
                statement = connection.prepareStatement(insertQuery());
                statement.setString(1, step.getName());
                statement.setInt(2, step.getTaskId());
                statement.setString(3, step.getParameters());
                LocalDateTime dateTime = step.getSubmitTimestamp();
                statement.setTimestamp(4, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = step.getStartTimestamp();
                statement.setTimestamp(5, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                dateTime = step.getEndTimestamp();
                statement.setTimestamp(6, dateTime != null ? Timestamp.valueOf(dateTime) : null);
                statement.setInt(7, step.getExitCode());
                statement.setInt(8, step.getStatus().value());
                dateTime = step.getStatusTimestamp();
                statement.setTimestamp(9, dateTime != null ? Timestamp.valueOf(dateTime) : null);
            }
            return statement;
        });
        return result == 1 ? step : null;
    }

    @Override
    protected String selectQuery() {
        return "SELECT name, task_id, parameters, submit_timestamp, start_timestamp, end_timestamp, exit_code, status_id, status_timestamp " +
                "FROM step ";
    }

    @Override
    protected String insertQuery() {
        return "INSERT INTO step(name, task_id, parameters, submit_timestamp, start_timestamp, end_timestamp, exit_code, status_id, status_timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String updateQuery() {
        return "UPDATE step " +
                "SET parameters=?, submit_timestamp=?, start_timestamp=?, end_timestamp=?, exit_code=?, status_id=?, status_timestamp=? " +
                "WHERE name=? AND task_id=?";
    }

    @Override
    protected String deleteQuery() {
        return "DELETE FROM step WHERE name=? AND task_id=?";
    }

    private abstract class StepTemplate extends Template {
        private final ActivityStatusConverter converter = new ActivityStatusConverter();

        @Override
        protected String baseSQL() { return selectQuery(); }

        @Override
        protected RowMapper<Step> rowMapper() {
            return (resultSet, rowNum) -> {
                Step step = new Step();
                step.setName(resultSet.getString(1));
                step.setTaskId(resultSet.getInt(2));
                step.setParameters(resultSet.getString(3));
                Timestamp timestamp = resultSet.getTimestamp(4);
                if (timestamp != null) {
                    step.setSubmitTimestamp(timestamp.toLocalDateTime());
                }
                timestamp = resultSet.getTimestamp(5);
                if (timestamp != null) {
                    step.setStartTimestamp(timestamp.toLocalDateTime());
                }
                timestamp = resultSet.getTimestamp(6);
                if (timestamp != null) {
                    step.setEndTimestamp(timestamp.toLocalDateTime());
                }
                step.setExitCode(resultSet.getInt(7));
                step.setStatus(converter.convertToEntityAttribute(resultSet.getInt(8)));
                timestamp = resultSet.getTimestamp(9);
                if (timestamp != null) {
                    step.setStatusTimestamp(timestamp.toLocalDateTime());
                }
                return step;
            };
        }
    }
}
