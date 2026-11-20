package com.example.personalblog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class NameForm {
    @NotBlank(message = "請輸入名稱")
    @Size(max = 50, message = "名稱最多 50 字")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.strip();
    }
}
