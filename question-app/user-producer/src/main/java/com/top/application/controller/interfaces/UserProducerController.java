package com.top.application.controller.interfaces;

import com.top.application.dto.UserReduceRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.UUID;

public interface UserProducerController {
    @Operation(summary = "Add new question")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User sent"),
            @ApiResponse(responseCode = "400", description = "Bad request")
    })
    void addUser(UserReduceRequestDto userProducerRequestDtoDto);

    @Operation(summary = "Delete existent user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User sent")
    })
    void deleteUser(UUID idUser);
}
