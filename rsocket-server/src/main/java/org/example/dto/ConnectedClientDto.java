package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.boot.jackson.JsonComponent;

import java.util.Date;

@JsonComponent
public class ConnectedClientDto {
    public String clientUUID;

    @JsonFormat(pattern="yyyy-MM-dd hh:mm:ss")
    public Date connectedTime;

    public ConnectedClientDto(){

    }

    public ConnectedClientDto(String uuid, Date date){
        this.clientUUID = uuid;
        this.connectedTime = date;
    }

}
