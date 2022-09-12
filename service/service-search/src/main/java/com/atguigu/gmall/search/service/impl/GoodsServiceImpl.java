package com.atguigu.gmall.search.service.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.vo.search.*;
import com.atguigu.gmall.search.repository.GoodsRepository;
import com.atguigu.gmall.search.service.GoodsService;
import com.atguigu.stater.cache.constant.SysRedisConst;
import com.google.common.collect.Lists;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoodsServiceImpl implements GoodsService {
    @Autowired
    ElasticsearchRestTemplate restTemplate;
    
    @Autowired
    GoodsRepository goodsRepository;

    /**
     * 保存goods到es
     * @param goods
     */
    @Override
    public void saveGoods(Goods goods) {
        goodsRepository.save(goods);
    }

    /**
     * 通过skuId删除goods
     * @param skuId
     */
    @Override
    public void deleteGoods(Long skuId) {
        goodsRepository.deleteById(skuId);
    }


    /**
     * 去es检索商品
     * @param searchParamVo
     * @return
     */
    @Override
    public SearchResponseVo search(SearchParamVo searchParamVo) {
        //1.动态构建出搜索条件
        Query query = buildQueryDsl(searchParamVo);
        
        //2.搜索
        SearchHits<Goods> goods =
                restTemplate.search(query, Goods.class, IndexCoordinates.of("goods"));

        //3.将搜索结果进行转换
        SearchResponseVo responseVo = buildSearchResponseResult(goods,searchParamVo);
        return responseVo;
 
    }

    /**
     * 更新热度分，在访问了商品的详情页面，热度分就增加
     *
     * @param skuId
     * @param score
     */
    @Override
    public void updateHotScore(Long skuId, Long score) {
        
        //1.根据skuId查找到是哪个商品
        Goods goods = goodsRepository.findById(skuId).get();
        //2.更新得分
        goods.setHotScore(score);
        //3.同步都es
        goodsRepository.save(goods);

        //https://www.elastic.co/guide/en/elasticsearch/reference/7.17/rest-apis.html
        //ES可以发送修改DSL，只更新hotScore字段
        //esRestTemplate.update();
    }

    /**
     * 构建索引条件,从前端传送过来的数据中读取能作为检索的数据
     * @param searchParamVo
     * @return
     */
    private Query buildQueryDsl(SearchParamVo searchParamVo) {
        //根据dsl语句，编写
        //1.装备bool
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //2.给bool中准备must的各个条件
        //2.1需要逐级分析，前端是否传送了这个条件
        if (searchParamVo.getCategory1Id()!=null){//一级分类id
            boolQuery.must(QueryBuilders.termQuery("category1Id",searchParamVo.getCategory1Id()));
        }
        
        if (searchParamVo.getCategory2Id()!=null){//二级分类id
            boolQuery.must(QueryBuilders.termQuery("category2Id",searchParamVo.getCategory2Id()));
        }
        
        if (searchParamVo.getCategory3Id()!=null){//三级分类id
            boolQuery.must(QueryBuilders.termQuery("category3Id",searchParamVo.getCategory3Id()));
        }
        //2.2判断是否有关键字
        if (!StringUtils.isEmpty(searchParamVo.getKeyword())){
            boolQuery.must(QueryBuilders.matchQuery("title",searchParamVo.getKeyword()));//模糊匹配
        }
        //2.3判断是否有品牌数据
        if (!StringUtils.isEmpty(searchParamVo.getTrademark())){
            //trademark=4:小米  品牌参数的格式需要进行分割,并且转换为Long
            long tmId = Long.parseLong(searchParamVo.getTrademark().split(":")[0]);
            boolQuery.must(QueryBuilders.termQuery("tmId",tmId));
        }
        //2.4判断有哪些属性  前端传了属性 props=4:128GB:机身存储&props=5:骁龙730:CPU型号
        String[] props = searchParamVo.getProps();
        if (props != null && props.length > 0){
            //遍历所有的属性
            for (String prop : props) {
                //通过 : 分割
                String[] split = prop.split(":");
                //获取分割后的属性id和属性值
                long attrId = Long.parseLong(split[0]);
                String attrvalue = split[1];

                //因为属性是数据一个内部的数组，所以需要使用内部的检索
                BoolQueryBuilder nestedBool = QueryBuilders.boolQuery();
                nestedBool.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBool.must(QueryBuilders.termQuery("attrs.attrValue", attrvalue));

                NestedQueryBuilder nestedQuery
                        = QueryBuilders.nestedQuery("attrs", nestedBool, ScoreMode.None);

                //给最大的boolQuery里面放 嵌入式查询 nestedQuery

                boolQuery.must(nestedQuery);
            }
        }
        
        
        //准备一个原生的检索条件
        NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(boolQuery);
        
        //2.5判断是否传送了排序  order=2:asc
        if (!StringUtils.isEmpty(searchParamVo.getOrder())){
            //对排序的格式进行分割
            String[] split = searchParamVo.getOrder().split(":");
            //分析排序使用哪个字段
            String orderField = "hotScore";
            switch (split[0]){
                case "1" : orderField="hotScore";break;
                case "2" : orderField="price";break;
                case "3" : orderField="createTime";break;
                default  : orderField="hotScore";break;
            }
            Sort sort = Sort.by(orderField);
            if (split[1].equals("asc")){
                sort = sort.ascending();
            }else {
                sort = sort.descending();
            }
            nativeSearchQuery.addSort(sort);
        }
        
        //2.6 分析前端传的页码，如果没传就穿固定的
        //页码在Spring底层是从0开始，自己要计算 前端页码-1 后的结果
        PageRequest pageRequest =
                PageRequest.of(searchParamVo.getPageNo() - 1, SysRedisConst.SEARCH_PAGE_SIZE);
        nativeSearchQuery.setPageable(pageRequest);
        
        //2.7高亮,模糊查询高亮
        if (!StringUtils.isEmpty(searchParamVo.getKeyword())){
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("title")
                            .preTags("<span style='color:red'>")
                            .postTags("</span>");

            HighlightQuery highlightQuery = new HighlightQuery(highlightBuilder);
            nativeSearchQuery.setHighlightQuery(highlightQuery);
        }
        
        
        //TODO 聚合分析上面DSL检索到的所有商品涉及了多少种品牌和多少种平台属性
        //3.品牌聚合 
        TermsAggregationBuilder tmIdAgg = AggregationBuilders
                .terms("tmIdAgg")
                .field("tmId")
                .size(1000);
        //3.1品牌聚合-品牌名子聚合
        TermsAggregationBuilder tmNameAgg = AggregationBuilders.terms("tmNameAgg").field("tmName").size(1);
        //3.2品牌聚合-品牌logo聚合
        TermsAggregationBuilder tmLogoAgg = AggregationBuilders.terms("tmLogoAgg").field("tmLogoUrl").size(1);
        
        tmIdAgg.subAggregation(tmNameAgg);
        tmIdAgg.subAggregation(tmLogoAgg);
        
        //品牌id聚合条件拼装完成
        nativeSearchQuery.addAggregation(tmIdAgg);
        
        
        //4.属性聚合
        //4.1属性的整个嵌入式聚合
        //获得嵌入式的聚合
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attrAgg", "attrs");
        
        //将属性id 属性名  属性值聚合
        //4.2attrId聚合
        TermsAggregationBuilder attrIdAgg = AggregationBuilders.terms("attrIdAgg").field("attrs.attrId").size(100);
        
        //4.3attrName聚合
        TermsAggregationBuilder attrNameAgg = AggregationBuilders.terms("attrNameAgg").field("attrs.attrName").size(1);
        
        //4.4attrValue聚合
        TermsAggregationBuilder attrValueAgg = AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue").size(100);
        
        //id下添加属性名和属性值得聚合
        attrIdAgg.subAggregation(attrNameAgg);
        attrIdAgg.subAggregation(attrValueAgg);
        //嵌入式聚合下添加属性的聚合
        attrAgg.subAggregation(attrIdAgg);
        //在整个大的查询下添加聚合 
        nativeSearchQuery.addAggregation(attrAgg);
        
        return nativeSearchQuery;
    }

    /**
     * 将查询到的数据转换为检索后页面显示的样子
     * @param goods
     * @param searchParamVo
     * @return
     */
    private SearchResponseVo buildSearchResponseResult(SearchHits<Goods> goods, SearchParamVo searchParamVo) {
        SearchResponseVo vo = new SearchResponseVo();
        //1.当时检索前端传来的所有参数
        vo.setSearchParam(searchParamVo);
        //2.品牌面包屑 trademark=1:小米
        if (!StringUtils.isEmpty(searchParamVo.getTrademark())){
            vo.setTrademarkParam("品牌: "+searchParamVo.getTrademark().split(":")[1]);
        }
        //3.平台属性面包屑
        if (searchParamVo.getProps() != null && searchParamVo.getProps().length > 0){
            List<SearchAttr> propsParamList = new ArrayList<>();
            for (String prop : searchParamVo.getProps()) {
                //23:8G:运行内存
                String[] split = prop.split(":");
                //一个SearchAttr 代表一个属性面包屑
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(Long.parseLong(split[0]));
                searchAttr.setAttrName(split[2]);
                searchAttr.setAttrValue(split[1]);
                propsParamList.add(searchAttr);
            }
            vo.setPropsParamList(propsParamList);
        }
        //TODO 4、所有品牌列表 。需要ES聚合分析
        List<TrademarkVo> trademarkVos = buildTrademarkList(goods);
        vo.setTrademarkList(trademarkVos);
        
        
        
        //TODO 5、所有属性列表 。需要ES聚合分析
        List<AttrVo> attrsList = buildAttrList(goods);
        vo.setAttrsList(attrsList);
        
        //6.返回排序信息 order=1:desc
        if (!StringUtils.isEmpty(searchParamVo.getOrder())){
            String order = searchParamVo.getOrder();
            OrderMapVo orderMapVo = new OrderMapVo();
            orderMapVo.setType(order.split(":")[0]);
            orderMapVo.setSort(order.split(":")[1]);
            vo.setOrderMap(orderMapVo);
        }
        
        //7.所有搜索到的商品列表 
        List<Goods> goodsList = new ArrayList<>();
        List<SearchHit<Goods>> hits = goods.getSearchHits();
        for (SearchHit<Goods> hit : hits) {
            //这条命中记录的商品
            Goods content = hit.getContent();
            //如果模糊检索了，会有高亮标题
            if (!StringUtils.isEmpty(searchParamVo.getKeyword())){
                String highlightTitle = hit.getHighlightField("title").get(0);
                //设置高亮标题
                content.setTitle(highlightTitle);
            }
            goodsList.add(content);
        }
        vo.setGoodsList(goodsList);
    
        //8.页码
        vo.setPageNo(searchParamVo.getPageNo());
        //9.总页码
        long totalHits = goods.getTotalHits();
        long ps = totalHits%SysRedisConst.SEARCH_PAGE_SIZE == 0?
                totalHits/SysRedisConst.SEARCH_PAGE_SIZE:
                (totalHits/SysRedisConst.SEARCH_PAGE_SIZE+1);
        vo.setTotalPages(new Integer(ps+""));
        
        //10.老连接。。。   /list.html?category2Id=13
        String url = makeUrlParam(searchParamVo);
        vo.setUrlParam(url);

        return vo;
    }

    /**
     * 将从es中查询出来的属性装换为页面需要bean
     * @param goods
     * @return
     */
    private List<AttrVo> buildAttrList(SearchHits<Goods> goods) {
        //先创建一个bean来装属性的内容
        List<AttrVo> attrVos = new ArrayList<>();
        //从goods中拿出查询出来的数据,整个聚合的结果
        ParsedNested attrAgg = goods.getAggregations().get("attrAgg");
        
        
        //拿到属性的聚合结果
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");

        //遍历聚合结果
        for (Terms.Bucket bucket : attrIdAgg.getBuckets()) {
            //创建一个bean存储这一个属性id 属性名 属性值
            AttrVo attrVo = new AttrVo();
            
            //获得属性的id
            long attrId = bucket.getKeyAsNumber().longValue();
            attrVo.setAttrId(attrId);
            
            //获得属性名
            ParsedStringTerms attrNameAgg = bucket.getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            attrVo.setAttrName(attrName);
            
            //获得属性值
            List<String> attrValueList = new ArrayList<>();
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attrValueAgg");
            for (Terms.Bucket attrValueAggBucket : attrValueAgg.getBuckets()) {
                String attrVlue = attrValueAggBucket.getKeyAsString();
                attrValueList.add(attrVlue);
            }
            attrVo.setAttrValueList(attrValueList);
            attrVos.add(attrVo);
        }
        return attrVos;
    }

    /**
     * 将查询到的所有商品涉及到的品牌信息转换,装换为TrademarkVo
     * @param goods
     * @return
     */
    private List<TrademarkVo> buildTrademarkList(SearchHits<Goods> goods) {
        //存储所有的商品品牌
        List<TrademarkVo> trademarkVos = new ArrayList<>();
        
        //拿到tmIdAgg聚合
        ParsedLongTerms tmIdAgg = goods.getAggregations().get("tmIdAgg");
        
        //拿到品牌id桶中的每一个数据
        for (Terms.Bucket bucket : tmIdAgg.getBuckets()) {
            //创建一个装品牌id 品牌名 品牌logo的bean
            TrademarkVo trademarkVo = new TrademarkVo();
            //1.获取品牌id
            long tmId = bucket.getKeyAsNumber().longValue();
            trademarkVo.setTmId(tmId);
            //2.获取品牌名
            ParsedStringTerms tmNameAgg = bucket.getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            trademarkVo.setTmName(tmName);
            //3.获取品牌logo
            ParsedStringTerms tmLogoAgg = bucket.getAggregations().get("tmLogoAgg");
            String tmLogo = tmLogoAgg.getBuckets().get(0).getKeyAsString();
            trademarkVo.setTmLogoUrl(tmLogo);

            trademarkVos.add(trademarkVo);
        }
        return trademarkVos;
    }

    /**
     * 制造老连接
     * @param searchParamVo
     * @return
     */
    private String makeUrlParam(SearchParamVo searchParamVo) {
        // list.html?&k=v
        StringBuilder builder = new StringBuilder("list.html?");
        //1、拼三级分类所有参数
        if (searchParamVo.getCategory1Id() != null) {
            builder.append("&category1Id=" + searchParamVo.getCategory1Id());
        }
        if (searchParamVo.getCategory2Id() != null) {
            builder.append("&category2Id=" + searchParamVo.getCategory2Id());
        }
        if (searchParamVo.getCategory3Id() != null) {
            builder.append("&category3Id=" + searchParamVo.getCategory3Id());
        }

        //2、拼关键字
        if (!StringUtils.isEmpty(searchParamVo.getKeyword())) {
            builder.append("&keyword=" + searchParamVo.getKeyword());
        }

        //3、拼品牌
        if (!StringUtils.isEmpty(searchParamVo.getTrademark())) {
            builder.append("&trademark=" + searchParamVo.getTrademark());
        }

        //4、拼属性
        if (searchParamVo.getProps() != null && searchParamVo.getProps().length > 0) {
            for (String prop : searchParamVo.getProps()) {
                //props=23:8G:运行内存
                builder.append("&props=" + prop);
            }
        }
        String url = builder.toString();
        return url;
    }
}
