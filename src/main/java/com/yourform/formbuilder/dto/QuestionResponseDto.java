package com.yourform.formbuilder.dto;

import com.yourform.formbuilder.model.Option;
import java.util.List;

public class QuestionResponseDto {

    private Long id;

    private String text;

    private String type;

    private boolean required;

    private List<Option> options;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }



    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }



    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



    public boolean isRequired() {
        return required;
    }

    public void setRequired(
       boolean required
    ){
       this.required=required;
    }



    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(
      List<Option> options
    ){
      this.options=options;
    }

}