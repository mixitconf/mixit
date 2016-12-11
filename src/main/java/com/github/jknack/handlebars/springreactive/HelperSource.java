package com.github.jknack.handlebars.springreactive;

import com.github.jknack.handlebars.Helper;

public class HelperSource<T> {

    private String name;

    private Helper<T> helper;

    public HelperSource(String name, Helper<T> helper) {
        this.name = name;
        this.helper = helper;
    }

    public String getName() {
        return name;
    }

    public Helper<T> getHelper() {
        return helper;
    }

}
