package com.fh.entity;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

@Data
@Document(indexName="lib",type="_doc")
public class Person {
    private String name;
    private String address;
    private int age;
    private String interests;
    private String birth;
}
