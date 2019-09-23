package com.fh;

import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.network.InetAddresses;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryAction;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.avg.Avg;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * 通过es原生java api操作
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringbootElasticsearchApplicationTests {

    // es集群名称
    private static final String ES_NAME = "my-application";
    // ip名称
    private static final String IP = "192.168.226.1";
    // 端口号
    private static final Integer PORT = 9300;

    // 查询某个文档
    @Test
    public void test01() {
        // 指定es集群
        Settings settings = Settings.builder().
                put("cluster.name", ES_NAME).put("client.transport.sniff", true).build();

        // 指定节点
        TransportClient transportClient = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(
                        new InetSocketAddress(InetAddresses.forString(IP), PORT)));

        // 数据查询
        GetResponse response = transportClient.prepareGet("lib", "_doc", "1").execute().actionGet();
        System.out.println(response.getSourceAsString());
        // 关闭连接
        transportClient.close();
    }

    // 添加文档
    @Test
    public void test02() throws IOException {
        // 指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 创建连接
        TransportClient client = new PreBuiltTransportClient(settings);
        // 指定节点 addTransportAddresses则是连接多个节点
        client.addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));

        // 创建文档
        XContentBuilder builder = XContentFactory.jsonBuilder().startObject()
                .field("name", "黄亚星")
                .field("age", 25)
                .field("address", "江苏省南通市如皋")
                .field("interests", "看电影,学习,打游戏")
                .field("birth", "1995-07-05")
                .endObject();// 一定要加startObject和endObject否则报错
        // 准备要发送的索引
        IndexRequestBuilder requestBuilder = client.prepareIndex("lib", "_doc", "4");
        // 添加文档并获取添加结果
        IndexResponse response = requestBuilder.setSource(builder).get();
        System.out.println(response.status());
        client.close();
    }

    // 删除文档
    @Test
    public void test03() throws Exception {
        // 指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 指定节点
        TransportClient client = new PreBuiltTransportClient(settings).
                addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));

        // 删除文档
        DeleteResponse response = client.prepareDelete("lib", "_doc", "4").get();
        System.out.println(response.status());

        // 关闭连接
        client.close();
    }

    // 修改文档 通过update方式
    @Test
    public void test04() throws Exception {
        // 指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 指定节点
        TransportClient client = new PreBuiltTransportClient(settings).
                addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 创建修改实例
        UpdateRequest updateRequest = new UpdateRequest();
        // 指定需要修改哪个索引下的哪个类型下的哪个文档，并指定修改内容
        updateRequest.index("lib").type("_doc").id("4");
        updateRequest.
                doc(XContentFactory.jsonBuilder().startObject().field("name", "黄亚星666").endObject());
        // 执行修改
        UpdateResponse response = client.update(updateRequest).get();
        System.out.println(response.status());

        // 关闭连接
        client.close();
    }

    // upsert方式修改添加文档
    @Test
    public void test05() throws Exception {
        // 指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 指定节点并连接
        TransportClient client = new PreBuiltTransportClient(settings).
                addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 创建修改实例
        UpdateRequest updateRequest = new UpdateRequest("lib", "_doc", "5");
        updateRequest.doc(XContentFactory.jsonBuilder().startObject().field("name", "蔡俊男666").endObject());

        // 2.指定updateRequest的更新方式为upsert
        // 2.1 创建添加实例
        IndexRequest indexRequest = new IndexRequest("lib", "_doc", "5");
        indexRequest.source(XContentFactory.jsonBuilder().startObject()
                .field("name", "蔡俊男")
                .field("age", 23)
                .field("address", "江苏省宿迁市")
                .field("interests", "看电影,学习,打游戏")
                .field("birth", "1997-07-05")
                .endObject());

        updateRequest.upsert(indexRequest);

        // 执行修改
        UpdateResponse updateResponse = client.update(updateRequest).get();
        System.out.println(updateResponse.status());
        // 关闭连接
        client.close();
    }

    // mget实现批量查询
    @Test
    public void test06() throws Exception {
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接，并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 通过mget批量查询文档
        MultiGetResponse multiGetResponse =
                client.multiGet(new MultiGetRequest().add("lib", "_doc", "1").add("lib", "_doc", "2")).get();
        for (MultiGetItemResponse response : multiGetResponse) {
            System.out.println(response.getResponse().getSourceAsString());
        }

        client.close();
    }

    // bulk实现批量操作
    @Test
    public void test07() throws Exception {
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.创建bulk实例
        BulkRequest bulkRequest = new BulkRequest();
        // 4.指定bulk要执行的操作
        bulkRequest.add(new IndexRequest("lib", "_doc", "6").source(XContentFactory.jsonBuilder().startObject()
                .field("name", "朱腾跃")
                .field("age", 23)
                .field("address", "江苏省镇江")
                .field("interests", "读书,看报,写文章")
                .field("birth", "1997-07-05")
                .endObject()));
        bulkRequest.add(new UpdateRequest("lib", "_doc", "1").
                doc(XContentFactory.jsonBuilder().startObject().field("address", "江苏省连云港市灌云县").endObject()));
        // 5.执行
        BulkResponse bulkResponse = client.bulk(bulkRequest).get();
        // 判断批量操作是否存在异常
        if (bulkResponse.hasFailures()) {
            System.out.println("出现异常!");
        } else {
            System.out.println(bulkResponse.status());
        }
        client.close();
    }

    // 查询删除,即先查询后删除
    @Test
    public void test08() throws Exception {
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        BulkByScrollResponse response = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
                .filter(QueryBuilders.matchQuery("address", "镇江")).source("lib").get();
        // 获取删除的数量
        System.out.println(response.getDeleted());
        client.close();
    }

    // 查询所有
    @Test
    public void test09() throws Exception {
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        // 4.执行查询 设置查询条件
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).setSize(3).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // match查询
    @Test
    public void test10() throws Exception {
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.执行查询模式
        QueryBuilder queryBuilder = QueryBuilders.matchQuery("address", "宿迁");
        // 4.执行查询
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // mult search
    @Test
    public void test11()throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.multiMatchQuery("宿迁", "address","name");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // term精准查询
    @Test
    public void test12()throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.termQuery("name", "吴兴玉");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // terms精准查询
    @Test
    public void test13()throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.termsQuery("name", "吴兴玉","周尔康");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // range查询
    @Test
    public void test14() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.rangeQuery("birth").from("1995-01-01").to("1997-07-10").format("YYYY-MM-DD");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // prefix查询
    @Test
    public void test15() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.prefixQuery("name", "吴");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // wildcard查询
    @Test
    public void test16() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.wildcardQuery("address", "?苏省");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // fuzzy查询
    @Test
    public void test17() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.fuzzyQuery("address", "江苏省");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // type查询
    @Test
    public void test18() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.typeQuery("_doc");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // id查询
    @Test
    public void test19() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.idsQuery("_doc").addIds("1","2");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }

    // 聚合查询
    // 查询最大
    @Test
    public void test20() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.max("maxAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Max max = searchResponse.getAggregations().get("maxAge");
        System.out.println(max.getValue());
        client.close();
    }

    // 查询最小
    @Test
    public void test21() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.min("minAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Min min = searchResponse.getAggregations().get("minAge");
        System.out.println(min.getValue());
        client.close();
    }

    // 查询总和
    @Test
    public void test22() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.sum("sumAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Sum sum = searchResponse.getAggregations().get("sumAge");
        System.out.println(sum.getValue());
        client.close();
    }

    // 查询平均
    @Test
    public void test23() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.avg("avgAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Avg avg = searchResponse.getAggregations().get("avgAge");
        System.out.println(avg.getValue());
        client.close();
    }

    // 查询基数
    @Test
    public void test24() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.cardinality("cardinalityAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Cardinality cardinality = searchResponse.getAggregations().get("cardinalityAge");
        System.out.println(cardinality.getValue());
        client.close();
    }

    // 查询分组
    @Test
    public void test25() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.聚合模式 maxAge为临时名称 age为对哪一个域进行聚合
        AggregationBuilder aggregationBuilder = AggregationBuilders.terms("termsAge").field("age");
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").addAggregation(aggregationBuilder).get();
        Terms terms = searchResponse.getAggregations().get("termsAge");
        System.out.println(terms.getName());
        client.close();
    }

    // 组合查询
    @Test
    public void test26() throws Exception{
        // 1.指定集群
        Settings settings = Settings.builder().put("cluster.name", ES_NAME).build();
        // 2.创建连接并指定节点
        TransportClient client = new PreBuiltTransportClient(settings)
                .addTransportAddress(new TransportAddress(InetAddress.getByName(IP), PORT));
        // 3.指定查询模式
        QueryBuilder queryBuilder = QueryBuilders.boolQuery()
                .must(QueryBuilders.matchQuery("address", "宿迁"))
                .mustNot(QueryBuilders.matchQuery("interests","看报" ))
                .filter(QueryBuilders.rangeQuery("age").gte(23));
        // 4.执行
        SearchResponse searchResponse = client.prepareSearch("lib").setQuery(queryBuilder).get();
        // 5.获取结果集
        SearchHits searchHits = searchResponse.getHits();
        for (SearchHit hit : searchHits) {
            Map<String, Object> map = hit.getSourceAsMap();
            for (String key : map.keySet()) {
                System.out.println(key + ":" + map.get(key));
            }
        }
        client.close();
    }
}
