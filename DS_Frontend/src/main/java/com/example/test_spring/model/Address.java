package com.example.test_spring.model;



public class Address {
    private Integer id;

    private String country;
    private String street;
    private String streetNumber;
    private String postcode;

    public Address(){}

    public Address(String country, String street, String streetNumber, String postcode){
        this.country = country;
        this.postcode = postcode;
        this.streetNumber = streetNumber;
        this.street = street;
    }

    public String getCountry() {
        return country;
    }
    public void setCountry(String country) {
        this.country = country;
    }
    public String getStreet() {
        return street;
    }
    public void setStreet(String street) {
        this.street = street;
    }
    public String getStreetNumber() {
        return streetNumber;
    }
    public void setStreetNumber(String streetNumber) {
        this.streetNumber = streetNumber;
    }
    public String getPostcode() {
        return postcode;
    }
    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }
}

