package com.yourform.formbuilder.controller;

//import com.yourform.formbuilder.dto.ResponseDto;
import com.yourform.formbuilder.service.ResponseService;
import org.springframework.web.bind.annotation.*;
import com.yourform.formbuilder.dto.SubmitRequest;
@RestController
@RequestMapping("/api/responses")
public class ResponseController {

    private final ResponseService service;

    public ResponseController(ResponseService service) {
        this.service = service;
    }

    // @PostMapping
    // public String submit(@RequestBody ResponseDto dto) {
    //     return service.saveResponse(dto);
    // }

    @PostMapping("/submit")
public String submit(@RequestBody SubmitRequest request) {
    service.submitResponse(request);
    return "Response submitted successfully";
}
}