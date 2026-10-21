package com.exercise.contactBook.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPageDTO {
    private List<ContactResponseDTO> content;
    private int page;
    private int size;
    private boolean hasMore; // to check if more contacts exists
}
