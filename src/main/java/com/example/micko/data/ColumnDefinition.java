package com.example.micko.data;

import lombok.Data;

@Data
public class ColumnDefinition {
    private String name;
    private String type;
    private boolean secure;

}
