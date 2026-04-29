package com.yourform.formbuilder.dto;

import java.util.List;

public class ChartDto {

 private List<String> labels;
 private List<Long> values;

 public List<String> getLabels() {
   return labels;
 }

 public void setLabels(
      List<String> labels){
   this.labels=labels;
 }

 public List<Long> getValues() {
   return values;
 }

 public void setValues(
      List<Long> values){
   this.values=values;
 }
}