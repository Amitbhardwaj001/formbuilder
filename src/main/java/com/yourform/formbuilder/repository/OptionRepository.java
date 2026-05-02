package com.yourform.formbuilder.repository;

import com.yourform.formbuilder.model.Option;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionRepository
extends JpaRepository<Option,Long>{

   List<Option> findByQuestionId(
       Long questionId
   );

   void deleteByQuestionId(
       Long questionId
   );
}
