package com.amadeus.common.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Result <T>{

    private int code;

    private String msg;

    private T data;

}
