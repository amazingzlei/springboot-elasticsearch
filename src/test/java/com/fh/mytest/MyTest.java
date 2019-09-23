package com.fh.mytest;

import com.fh.entity.Person;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * ElasticsearchTemplate api操作
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MyTest {

    @Autowired
    private ElasticsearchTemplate template;

    @Test
    public void test01(){
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        SearchQuery searchQuery = new NativeSearchQuery(queryBuilder);
        List<Person> list = template.queryForList(searchQuery, Person.class);
        System.out.println(list);
    }

    public void test02(){
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        SearchQuery searchQuery = new NativeSearchQueryBuilder().withQuery(queryBuilder)
                .withPageable(new PageRequest(0,2)).build();
        List<Person> list = template.queryForList(searchQuery, Person.class);
        System.out.println(list);
    }

}
