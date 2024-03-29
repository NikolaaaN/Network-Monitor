/*
Copyright (c) 2022, Nikola Nešković
All rights reserved.

This source code is licensed under the BSD-style license found in the
LICENSE file in the root directory of this source tree.
*/
package com.example.networkmonitor;

import android.graphics.drawable.Drawable;

import java.text.DecimalFormat;

public class RowObject implements  Comparable<RowObject>{

    private double usageTemp;
    private String usage;
    private Drawable slika;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getUsageTemp() {
        return usageTemp;
    }

    public void setUsageTemp(double usageTemp) {
        this.usageTemp = usageTemp;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public Drawable getSlika() {
        return slika;
    }

    public void setSlika(Drawable slika) {
        this.slika = slika;
    }

    @Override
    public String toString() {
        return "RowObject{" +
                "usageTemp=" + usageTemp +
                ", slika=" + slika +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public int compareTo(RowObject rowObject) {
        if (this.getUsageTemp() > rowObject.getUsageTemp())
            return -1;
        else if (this.getUsageTemp() == rowObject.getUsageTemp())
            return 0;
        else
            return 1;
    }

    public void formatUsage(){
        DecimalFormat df = new DecimalFormat("0.0");
        this.usageTemp=Double.parseDouble(df.format(this.usageTemp));
    }
}
