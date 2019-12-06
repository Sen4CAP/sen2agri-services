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
package org.esa.sen2agri.web;

import org.esa.sen2agri.commons.DownloadProgress;
import org.esa.sen2agri.entities.enums.Satellite;
import org.esa.sen2agri.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.tao.datasource.DataQuery;
import ro.cs.tao.datasource.DataSourceComponent;
import ro.cs.tao.datasource.param.CommonParameterNames;
import ro.cs.tao.datasource.param.QueryParameter;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ServiceResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Cosmin Cara
 */
@Controller
@RequestMapping("/downloader")
public class DownloadController extends ControllerBase {

    @Autowired
    private DataSourceService dataSourceService;
    @Autowired
    private DownloadService downloadService;
    @Autowired
    private SiteHelper siteHelper;
    @Autowired
    private ScheduleManager scheduleManager;

    /**
     * Returns information about all the downloads in progress.
     */
    @RequestMapping(value = "/", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<DownloadProgress>> getInProgress() {
        List<DownloadProgress> tasks = downloadService.getDownloadsInProgress((short) 0);
        if (tasks == null || tasks.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @RequestMapping(value = "/query", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> query(@RequestParam("satellite") String satellite,
                                   @RequestParam("dataSource") String dataSource,
                                   @RequestParam("startDate") Date startDate,
                                   @RequestParam("endDate") Date endDate,
                                   @RequestParam("wkt") String wkt,
                                   @RequestParam(name = "user", required = false) String user,
                                   @RequestParam(name = "password", required = false) String password) {
        try {
            DataSourceComponent component = new DataSourceComponent(satellite, dataSource);
            DataQuery query = component.createQuery();
            query.addParameter(CommonParameterNames.PLATFORM, satellite);
            query.addParameter(CommonParameterNames.START_DATE, startDate);
            query.addParameter(CommonParameterNames.END_DATE, endDate);
            query.addParameter(CommonParameterNames.FOOTPRINT, Polygon2D.fromWKT(wkt));
            query.setMaxResults(1);
            return prepareResult(query.execute());
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> test(@RequestParam("satellite") String satellite,
                                                   @RequestParam("dataSource") String dataSource,
                                                   @RequestParam(name = "user", required = false) String user,
                                                   @RequestParam(name = "password", required = false) String password) {
        try {
            final String platformName = satellite.contains("-") ? satellite : satellite.substring(0, satellite.length() - 1) + "-" + satellite.substring(satellite.length() - 1);
            final String dsSensor = satellite.contains("-") ? satellite.replace("-", "") : satellite;
            DataSourceComponent component = new DataSourceComponent(dsSensor, dataSource);
            component.setUserCredentials(user, password);
            DataQuery query = component.createQuery();
            query.addParameter(CommonParameterNames.PLATFORM, platformName);
            final LocalDate testDate = LocalDate.now().minus(2, ChronoUnit.MONTHS);
            final Date dateFrom = Date.from(LocalDateTime.of(testDate.getYear(), testDate.getMonth(), 1, 0, 0, 0, 0)
                                      .atZone(ZoneId.systemDefault()).toInstant());
            final Date dateTo = Date.from(LocalDateTime.of(testDate.getYear(), testDate.getMonth(), 15, 0, 0, 0, 0)
                                      .atZone(ZoneId.systemDefault()).toInstant());
            QueryParameter<Date> dateParam = query.createParameter(CommonParameterNames.START_DATE, Date.class);
            if ("Scientific Data Hub".equals(dataSource)) {
                dateParam.setMinValue(dateFrom);
                dateParam.setMaxValue(dateTo);
            } else {
                dateParam.setValue(dateFrom);
            }
            query.addParameter(dateParam);
            dateParam = query.createParameter(CommonParameterNames.END_DATE, Date.class);
            if ("Scientific Data Hub".equals(dataSource)) {
                dateParam.setMinValue(dateFrom);
                dateParam.setMaxValue(dateTo);
            } else {
                dateParam.setValue(dateTo);
            }
            query.addParameter(dateParam);
            query.addParameter(CommonParameterNames.FOOTPRINT,
                               Polygon2D.fromWKT("POLYGON((22.8042573604346 43.8379609098684," +
                                                         "24.83885442747927 43.8379609098684," +
                                                         "24.83885442747927 44.795645304033826," +
                                                         "22.8042573604346 44.795645304033826," +
                                                         "22.8042573604346 43.8379609098684))"));
            query.setMaxResults(1);
            final List<EOProduct> result = query.execute();
            return prepareResult(result.size(), "OK");
        } catch (Exception ex) {
            return handleException(ex);
        }
    }

    /**
     * Returns information about the downloads in progress for a specific site
     * @param siteId    The site identifier
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<DownloadProgress>> getInProgress(@PathVariable("id") short siteId) {
        List<DownloadProgress> tasks = downloadService.getDownloadsInProgress(siteId);
        if (tasks == null || tasks.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @RequestMapping(value = "/{code}/{satellite}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<SensorProgress>> getOverallProgress(@PathVariable("code") String shortName,
                                                                   @PathVariable("satellite") String satelliteName) {
        Short siteId = siteHelper.getSiteIdByShortName(shortName);
        Satellite satellite = Enum.valueOf(Satellite.class, satelliteName);
        List<SensorProgress> progress = null;
        if (siteId != null) {
            progress = downloadService.getProgress(siteId, satellite.value());
            if (progress == null) {
                progress = new ArrayList<>();
            }
        }
        return new ResponseEntity<>(progress, HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}/count", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Long> getCount(@PathVariable("id") short siteId) {
        long count = downloadService.getCount(siteId);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    /**
     * Stops all the downloads and marks the downloader as disabled for all sites
     */
    @RequestMapping(value = "/stop", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> stop() {
        downloadService.stop((short) 0);
        info("/downloader/stop received");
        return new ResponseEntity<>("Stop message sent", HttpStatus.OK);
    }

    /**
     * Stops the downloads and marks the downloader disabled for the specific site.
     * @param siteId    The site identifier
     */
    @RequestMapping(value = "/stop/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> stop(@PathVariable("id") short siteId) {
        downloadService.stop(siteId);
        info("/downloader/stop/%s received", siteId);
        return new ResponseEntity<>("Stop message sent", HttpStatus.OK);
    }

    /**
     * Stops the downloads of the specific satellite and marks the downloader disabled
     * only for the specific site and satellite
     * @param siteId    The site identifier
     * @param satelliteId   The satellite identifier
     */
    @RequestMapping(value = "/stop/{id}/{satelliteId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> stop(@PathVariable("id") short siteId,
                                  @PathVariable("satelliteId") short satelliteId) {
        downloadService.stop(siteId, satelliteId);
        info("/downloader/stop/%s/%s received", siteId, satelliteId);
        return new ResponseEntity<>("Stop message sent", HttpStatus.OK);
    }

    /**
     * Enables the downloader.
     */
    @RequestMapping(value = "/start", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> start() {
        downloadService.start((short) 0);
        scheduleManager.refresh();
        info("/downloader/start received");
        return new ResponseEntity<>("Start message sent", HttpStatus.OK);
    }

    /**
     * Enables the downloader for the specific site.
     * @param siteId    The site identifier
     */
    @RequestMapping(value = "/start/{id}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> start(@PathVariable("id") short siteId) {
        downloadService.start(siteId);
        scheduleManager.refresh();
        info("/downloader/start/%s received", siteId);
        return new ResponseEntity<>("Start message sent", HttpStatus.OK);
    }

    /**
     * Forces the downloader to start from the beginning for the specific site.
     * @param siteId    The site identifier
     */
    @RequestMapping(value = "/forcestart", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> forceStart(@RequestParam("job") String job, @RequestParam("siteId") short siteId,
                                        @RequestParam(name = "satelliteId", required = false) Short satelliteId) {
        if (satelliteId == null) {
            downloadService.forceStart(job, siteId);
        } else {
            downloadService.forceStart(job, siteId, satelliteId);
        }
        //scheduleManager.refresh();
        //info("/downloader/forcestart/%s received", siteId);
        return new ResponseEntity<>("Force start message sent", HttpStatus.OK);
    }

    /**
     * Enables the downloader for the specific site and satellite.
     * @param siteId    The site identifier
     * @param satelliteId   The satellite identifier
     */
    @RequestMapping(value = "/start/{id}/{satelliteId}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<?> start(@PathVariable("id") short siteId,
                                   @PathVariable("satelliteId") short satelliteId) {
        downloadService.start(siteId, satelliteId);
        scheduleManager.refresh();
        return new ResponseEntity<>("Start message sent", HttpStatus.OK);
    }
}
