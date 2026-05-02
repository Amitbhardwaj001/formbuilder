package com.yourform.formbuilder.repository;

//package com.example.formbuilder.repository;

import com.yourform.formbuilder.model.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Long> {
    Optional<Form> findByShareToken(
      String shareToken);

    List<Form> findByOwnerUsername(String ownerUsername);
}
