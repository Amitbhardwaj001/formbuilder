package com.yourform.formbuilder.service;

import com.yourform.formbuilder.dto.ResponseDto;
import com.yourform.formbuilder.model.*;
import com.yourform.formbuilder.repository.*;
import org.springframework.stereotype.Service;
import com.yourform.formbuilder.dto.SubmitRequest;
@Service
public class ResponseService {

    private final ResponseRepository responseRepo;
    private final AnswerRepository answerRepo;
    private final QuestionRepository questionRepo;
    private final FormRepository formRepo;

    public ResponseService(ResponseRepository responseRepo,
                           AnswerRepository answerRepo,
                           QuestionRepository questionRepo,
                           FormRepository formRepo) {
        this.responseRepo = responseRepo;
        this.answerRepo = answerRepo;
        this.questionRepo = questionRepo;
        this.formRepo = formRepo;
    }

    // public String saveResponse(ResponseDto dto) {

    //     Form form = formRepo.findById(dto.getFormId()).orElseThrow();

    //     Response response = new Response();
    //     response.setForm(form);
    //     responseRepo.save(response);

    //     for (ResponseDto.AnswerDto a : dto.getAnswers()) {
    //         Answer ans = new Answer();

    //         ans.setAnswerText(a.getAnswerText());
    //         ans.setResponse(response);
    //         ans.setQuestion(
    //             questionRepo.findById(a.getQuestionId()).orElseThrow()
    //         );

    //         answerRepo.save(ans);
    //     }

    //     return "Response saved";
    // }
    public void submitResponse(SubmitRequest request) {

    Form form = formRepo.findById(request.getFormId())
            .orElseThrow(() -> new RuntimeException("Form not found"));

    Response response = new Response();
    response.setForm(form);
    responseRepo.save(response);

    for (SubmitRequest.AnswerDto dto : request.getAnswers()) {

        Question q = questionRepo.findById(dto.getQuestionId())
                .orElseThrow(() -> new RuntimeException("Question not found"));

        Answer ans = new Answer();
        ans.setQuestion(q);
        ans.setResponse(response);
        ans.setAnswerText(dto.getAnswerText());

        answerRepo.save(ans);
    }
}
}