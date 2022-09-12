package com.atguigu.gmall.web.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.feign.search.SearchFeignClient;
import com.atguigu.gmall.model.vo.search.SearchParamVo;
import com.atguigu.gmall.model.vo.search.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class SearchController {
    @Autowired
    SearchFeignClient searchFeignClient;

    /**
     * 分析，检索列表页需要的参数
     * 1.分类，一级分类Category1Id,二级分类Category2Id，三级分类Category3Id
     * 2.品牌 trademark
     * 3.平台属性与属性值 attr  attrvalue
     * 4.排序  sort
     * 5.分页 page
     * @return
     */
    
    @GetMapping("/list.html")
    public String search(SearchParamVo searchParamVo,
                         Model model){
        //查询到上述分析得数据，给前端传过去，显示到页面上，通过查看检索首页，可以查看需要哪些数据
        Result<SearchResponseVo> search =  searchFeignClient.search(searchParamVo);
        SearchResponseVo data = search.getData();

        //把result数据展示到页面
        //1、以前检索页面点击传来的所有条件，原封不动返回给页面
        model.addAttribute("searchParam",data.getSearchParam());
        //2、品牌面包屑位置的显示
        model.addAttribute("trademarkParam",data.getTrademarkParam());
        //3、属性面包屑，是集合。集合里面每个元素是一个对象，拥有这些数据（attrName、attrValue、attrId）
        model.addAttribute("propsParamList",data.getPropsParamList());
        //4、所有品牌，是集合。集合里面每个元素是一个对象，拥有这些数据（tmId、tmLogoUrl、tmName）
        model.addAttribute("trademarkList",data.getTrademarkList());
        //5、所有属性，是集合。集合里面每个元素是一个对象，拥有这些数据（attrId，attrName，List<String> attrValueList， ）
        model.addAttribute("attrsList",data.getAttrsList());
        //6、排序信息。是对象。 拥有这些数据（type，sort）
        model.addAttribute("orderMap",data.getOrderMap());
        //7、所有商品列表。是集合。集合里面每个元素是一个对象,拥有这些数据(es中每个商品的详细数据)
        model.addAttribute("goodsList",data.getGoodsList());
        //8、分页信息
        model.addAttribute("pageNo",data.getPageNo());
        model.addAttribute("totalPages",data.getTotalPages());
        //9、url信息
        model.addAttribute("urlParam",data.getUrlParam());



        return "list/index";
    }
}
