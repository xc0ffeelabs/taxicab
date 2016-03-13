package com.xc0ffeelabs.taxicab.models;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseUser;

@ParseClassName("_User")
public class User extends ParseUser {

    public static final String ROLE = "role";
    public static final String NAME = "name";
    public static final String PHONE = "phone";
    public static final String LICENSE = "license";
    public static final String CAR_MODEL = "carModel";
    public static final String CAR_NUMBER = "carNumber";
    public static final String CURRENT_LOCATION = "currentLocation";
    public static final String STATE = "state";

    public static final String USER_ROLE = "role";
    public static final String DRIVER_ROLE = "driver";

    public User() {
    }

    public void setRole(String role) {
        put(ROLE, role);
    }

    public void setName(String name) {
        put(NAME, name);
    }

    public void setPhone(String phone) {
        put(PHONE, phone);
    }

    public void setLicense(String license) {
        put(LICENSE, license);
    }

    public void setCarModel(String carModel) {
        put(CAR_MODEL, carModel);
    }

    public void setCarNumber(String carNumber) {
        put(CAR_NUMBER, carNumber);
    }

    public String getName() {
        return getString(NAME);
    }

    public String getEmail() {
        return getUsername();
    }

    public String getRole() {
        return getString(ROLE);
    }

    public String getPhone() {
        return getString(PHONE);
    }

    public String getLicense() {
        return getString(LICENSE);
    }

    public String getCarModel() {
        return getString(CAR_MODEL);
    }

    public String getCarNumber() {
        return getString(CAR_NUMBER);
    }

    public ParseGeoPoint getLocation() {
        return getParseGeoPoint(CURRENT_LOCATION);
    }
}
