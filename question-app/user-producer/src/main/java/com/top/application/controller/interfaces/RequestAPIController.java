package com.top.application.controller.interfaces;

import com.top.application.dto.RequestAPIDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;


public interface RequestAPIController {

    @Operation(summary = "SendRequest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User sent"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    ResponseEntity<Integer> sendRequestToAPI(RequestAPIDto requestAPIDto);

}