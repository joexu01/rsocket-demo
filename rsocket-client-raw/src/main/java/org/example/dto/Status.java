package org.example.dto;

public class Status {
    public String uuid;
    public String temperature;
    public String usage;

    public Status(String uuid, String temp, String usage){
        this.uuid = uuid;
        this.temperature = temp;
        this.usage = usage;
    }
}