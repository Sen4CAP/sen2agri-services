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

package org.esa.sen2agri.entities;

import org.esa.sen2agri.entities.enums.ActivityStatus;

import java.time.LocalDateTime;
import java.util.Arrays;

public class Task {
    private int id;
    private int jobId;
    private String moduleShortName;
    private String parameters;
    private LocalDateTime submitTimestamp;
    private LocalDateTime startTimestamp;
    private LocalDateTime endTimestamp;
    private ActivityStatus status;
    private LocalDateTime statusTimestamp;
    private int[] precedingTasks;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getJobId() {
        return jobId;
    }

    public void setJobId(int id) {
        this.jobId = id;
    }

    public String getModuleShortName() {
        return moduleShortName;
    }

    public void setModuleShortName(String moduleShortName) {
        this.moduleShortName = moduleShortName;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public LocalDateTime getSubmitTimestamp() {
        return submitTimestamp;
    }

    public void setSubmitTimestamp(LocalDateTime submitTimestamp) {
        this.submitTimestamp = submitTimestamp;
    }

    public LocalDateTime getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(LocalDateTime startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public LocalDateTime getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(LocalDateTime endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public LocalDateTime getStatusTimestamp() {
        return statusTimestamp;
    }

    public void setStatusTimestamp(LocalDateTime statusTimestamp) {
        this.statusTimestamp = statusTimestamp;
    }

    public int[] getPrecedingTasks() {
        return precedingTasks;
    }

    public void setPrecedingTasks(int[] precedingTasks) {
        this.precedingTasks = precedingTasks;
    }

    public void addPrecedingTask(int taskId) {
        if (this.precedingTasks == null) {
            this.precedingTasks = new int[1];
        } else {
            this.precedingTasks = Arrays.copyOf(this.precedingTasks, this.precedingTasks.length + 1);
        }
        this.precedingTasks[this.precedingTasks.length - 1] = taskId;
    }
}
