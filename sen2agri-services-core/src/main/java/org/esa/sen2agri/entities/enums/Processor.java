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

package org.esa.sen2agri.entities.enums;

import ro.cs.tao.TaoEnum;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum(Integer.class)
public enum Processor implements TaoEnum<Integer> {
    L2A(1, "l2a", "L2A Atmospheric Corrections"),
    L3A(2, "l3a", "L3A Composite"),
    L3B(3, "l3b_lai", "L3B Vegetation Status"),
    L3E(4, "l3e_pheno", "L3E Pheno NDVI Metrics"),
    L4A(5, "l4a", "L4A Crop Mask"),
    L4B(6, "l4b", "L4B Crop Type");

    private final int value;
    private final String shortName;
    private final String description;

    Processor(int value, String shortName, String description) {
        this.value = value;
        this.shortName = shortName;
        this.description = description;
    }

    public String shortName() { return shortName; }

    @Override
    public String friendlyName() { return this.description; }

    @Override
    public Integer value() { return this.value; }
}
