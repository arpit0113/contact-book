package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PendingUserDTO {
   private Long userId;
   private String name;
   private String email;
}
