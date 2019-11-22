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

package org.esa.sen2agri.services;

import org.esa.sen2agri.commons.Config;
import org.esa.sen2agri.db.PersistenceManager;
import org.esa.sen2agri.entities.*;
import org.esa.sen2agri.entities.enums.ActivityStatus;
import org.esa.sen2agri.entities.enums.JobStartType;
import ro.cs.tao.serialization.MediaType;
import ro.cs.tao.serialization.SerializationException;
import ro.cs.tao.serialization.SerializerFactory;

import java.time.LocalDateTime;
import java.util.List;

public class JobHelper {
    private static final JobHelper instance = new JobHelper(Config.getPersistenceManager());
    private final PersistenceManager persistenceManager;

    private JobHelper(PersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
    }

    public static Job createJob(Site site, Processor processor, String parameters) {
        Job job = new Job();
        LocalDateTime timeStamp = LocalDateTime.now();
        job.setProcessor(processor);
        job.setSite(site);
        job.setJobStartType(JobStartType.TRIGGERED);
        job.setSubmitTimestamp(timeStamp);
        job.setStartTimestamp(timeStamp);
        job.setStatus(ActivityStatus.RUNNING);
        job.setParameters(parameters);
        job.setStatusTimestamp(timeStamp);
        job = instance.persistenceManager.save(job);
        return job;
    }

    public static Task createTask(Job job, String name, String parameters, int...precedingTaskIds) {
        Task task = new Task();
        task.setJobId(job.getId());
        task.setModuleShortName(name);
        task.setParameters(parameters);
        LocalDateTime timeStamp = LocalDateTime.now();
        task.setSubmitTimestamp(timeStamp);
        task.setStartTimestamp(timeStamp);
        task.setStatus(ActivityStatus.SUBMITTED);
        task.setStatusTimestamp(timeStamp);
        if (precedingTaskIds != null) {
            task.setPrecedingTasks(precedingTaskIds);
        }
        instance.persistenceManager.save(task);
        return task;
    }

    public static void update(Job job, ActivityStatus status) {
        job.setStatus(status);
        final LocalDateTime dateTime = LocalDateTime.now();
        job.setStatusTimestamp(dateTime);
        if (status == ActivityStatus.FINISHED || status == ActivityStatus.ERROR) {
            job.setEndTimestamp(dateTime);
        }
        instance.persistenceManager.save(job);
    }

    public static void update(Task task, ActivityStatus status) {
        task.setStatus(status);
        final LocalDateTime dateTime = LocalDateTime.now();
        task.setStatusTimestamp(dateTime);
        if (status == ActivityStatus.RUNNING) {
            task.setStartTimestamp(dateTime);
        }
        if (status == ActivityStatus.FINISHED || status == ActivityStatus.ERROR) {
            task.setEndTimestamp(dateTime);
        }
        instance.persistenceManager.update(task);
    }

    public static void update(Step step, ActivityStatus status, Integer exitCode) {
        step.setStatus(status);
        final LocalDateTime dateTime = LocalDateTime.now();
        step.setStatusTimestamp(dateTime);
        if (status == ActivityStatus.RUNNING) {
            step.setStartTimestamp(dateTime);
        }
        if (exitCode != null) {
            step.setExitCode(exitCode);
            step.setEndTimestamp(dateTime);
        }
        instance.persistenceManager.save(step);
    }

    public static Step createStep(Task task, String name, List<String> parameters) {
        Step step = new Step();
        step.setTaskId(task.getId());
        step.setName(name);
        if (parameters != null) {
            try {
                final String arguments = SerializerFactory.create(String.class, MediaType.JSON).serialize(parameters, "arguments");
                step.setParameters(arguments);
            } catch (SerializationException e) {
                e.printStackTrace();
            }
        }
        LocalDateTime timeStamp = LocalDateTime.now();
        step.setSubmitTimestamp(timeStamp);
        step.setStartTimestamp(timeStamp);
        step.setStatus(ActivityStatus.SUBMITTED);
        step.setStatusTimestamp(timeStamp);
        return instance.persistenceManager.save(step);
    }
}
