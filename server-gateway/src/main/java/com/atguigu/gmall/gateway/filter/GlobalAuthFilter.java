package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.constant.SysRedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;

import com.atguigu.gmall.gateway.properties.AuthUrlProperties;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.stater.cache.utils.Jsons;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Webflux：响应式web编程【消息队列分布式】
 * 内存版的消息队列
 * //1
 * Future fu = aService.b();
 * //2
 * fu.get();
 * <p>
 * Servlet：阻塞式编程方式
 */
@Slf4j
@Component
public class GlobalAuthFilter implements GlobalFilter {

    @Autowired
    AuthUrlProperties authUrlProperties;

    AntPathMatcher matcher = new AntPathMatcher();

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange
            , GatewayFilterChain chain) {
        /**
         * ServerWebExchange: ServerWebExchange就相当于当前请求和响应的上下文。
         * GatewayFilterChain: 当前过滤器可以决定是否执行下一个过滤器的逻辑，由GatewayFilterChain#filter()是否被调用来决定
         */
        //1.获取请求路径
        String path = exchange.getRequest().getURI().getPath();//请求的路径
        String uri = exchange.getRequest().getURI().toString();

        //2.判断是那种路径，取得在配置文文件中配置的路径，进行遍历，和本次前端传送过来的路径进行匹配
        //2.无需登录就能访问的资源遍历

        for (String url : authUrlProperties.getNoAuthUrl()) {

            boolean match = matcher.match(url, path);//前端传送的路径和不需要登录就访问的路径是否匹配
            if (match){
                return chain.filter(exchange);//如果匹配的话就直接放行，并携带前端传送过来的数据
            }
        }
        //静态资源虽然带了token。不用校验token，直接放

        //能走到这儿，说明不是直接放行的资源

        //3.能走到这里说明不是直接放行的，到其他的路径条件中匹配  将GlobalFilter拦截下来的请求,进行校验,并封装错误信息然后返回前端JSON
        //访问/api/inner直接拒绝
        for (String url : authUrlProperties.getDenyUrl()) {

            boolean match = matcher.match(url, path);
            if (match){
                //直接给前端响应json数据

                Result<String> result
                        = Result.build("", ResultCodeEnum.PERMISSION);

                return responseResult(result,exchange);
            }
        }

        //4.需要登录的请求 ： 进行权限验证
        for (String url : authUrlProperties.getLoginAuthUrl()) {//遍历需要权限验证的路径
            boolean match = matcher.match(url, path);
            if (match) {
                //登录等校验
                //3.1获取token信息
                String tokenValue = getTokenValue(exchange);
                //3.2校验token
                UserInfo info = getTokenUserInfo(tokenValue);
                //3.3判断从redis中是否查到了信息
                if (info != null) {
                    //redis中有此token对应的用户，将userId存储到request的头中
                    ServerWebExchange webExchange = userIdOrTempIdTransport(info, exchange);
                    return chain.filter(webExchange);//放行
                } else {
                    //redis中无此用户【假令牌、token没有，没登录】
                    //重定向到登录页
                    return redirectToCustomPage(authUrlProperties.getLoginPage() + "?originUrl=" + uri, exchange);
                }
            }

        }
        //能走到这儿，既不是静态资源直接放行，也不是必须登录才能访问的，就一普通请求
        //普通请求只要带了 token，说明可能登录了。只要登录了，就透传用户id。
        String tokenValue = getTokenValue(exchange);
        UserInfo info = getTokenUserInfo(tokenValue);
        if(!StringUtils.isEmpty(tokenValue) && info == null){
            //假请求直接打回登录
            return redirectToCustomPage(authUrlProperties.getLoginPage()+"?originUrl="+uri,exchange);
        }

        //普通请求，透传用户id或者临时id
        exchange = userIdOrTempIdTransport(info, exchange);

        return chain.filter(exchange);

        //4、对登录后的请求进行user_id透传
//        Mono<Void> filter = chain.filter(exchange)
//                .doFinally((signalType) -> {
//                    log.info("{} 请求结束", path);
//                });
    }



    /**
     * 重定向到指定位置
     * @param
     * @param exchange
     * @return
     */
    private Mono<Void> redirectToCustomPage(String location, ServerWebExchange exchange) {
        //重定向需要两个元素：1.响应码为302   2.location ->新的地址
        ServerHttpResponse response = exchange.getResponse();

        //1、重定向【302状态码 + 响应头中 Location: 新位置】
        response.setStatusCode(HttpStatus.FOUND);
        response.getHeaders().add(HttpHeaders.LOCATION,location);

        //2、清除旧的错误的Cookie[token]（同名cookie并max-age=0）解决无限重定向问题
        ResponseCookie tokenCookie = ResponseCookie.from("token", "777")
                .maxAge(0)//存活时间
                .path("/")
                .domain(".gmall.com")//区域
                .build();

        response.getCookies().set("token",tokenCookie);//将浏览器中的错误的cookie覆盖掉，设置新的token的存活时间为0秒

        //3.响应结束
        return  response.setComplete();
    }

    /**
     * 将查询到的userID存储到请求头中  用户id透传
     * @param info
     * @param exchange
     * @return
     */
    private ServerWebExchange userIdOrTempIdTransport(UserInfo info, ServerWebExchange exchange) {
        //请求一旦发来，所有的请求数据是固定的，不能进行任何修改，只能读取
        ServerHttpRequest.Builder newReqbuilder = exchange.getRequest().mutate();

        //用户登录了
        if(info != null){
            newReqbuilder.header(SysRedisConst.USERID_HEADER,info.getId().toString());
        }
        //用户没登录
        String userTempId = getUserTempId(exchange);
        newReqbuilder.header(SysRedisConst.USERTEMPID_HEADER,userTempId);

        //放行的时候传改掉的exchange
        ServerWebExchange webExchange = exchange
                .mutate()
                .request(newReqbuilder.build())
                .response(exchange.getResponse())
                .build();
//            request.getHeaders().add();
        return webExchange;

    }
    /**
     * 获取临时id
     * @param exchange
     * @return
     */
    private String getUserTempId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        //1、尝试获取头中的临时id
        String tempId = request.getHeaders().getFirst("userTempId");
        //2、如果头中没有，尝试获取cookie中的值
        if(StringUtils.isEmpty(tempId)){
            HttpCookie httpCookie = request.getCookies().getFirst("userTempId");
            if(httpCookie!=null){
                tempId = httpCookie.getValue();
            }
        }

        return tempId;
    }


    /**
     * 从redis中查询
     * @param tokenValue
     * @return
     */
    private UserInfo getTokenUserInfo(String tokenValue) {
        String json = redisTemplate.opsForValue().get(SysRedisConst.LOGIN_USER + tokenValue);
        if (!StringUtils.isEmpty(json)){
            return Jsons.toObj(json,UserInfo.class);
        }


        return null;
    }

    /**
     * 获得exchange中的token
     * @param exchange
     * @return
     */
    private String getTokenValue(ServerWebExchange exchange) {
        //指定token的值
        String tokenValue = "";
        //token可以存在两个地方，一个是cookie，一个是头中，都需要判断
        //先判断cookie中有没有
        HttpCookie token = exchange.getRequest()
                .getCookies()
                .getFirst("token");

        if (token != null){
            tokenValue = token.getValue();
            return tokenValue;
        }

        //说明cookie中没有,到请求头中查找
        tokenValue = exchange.getRequest()
                .getHeaders()
                .getFirst("token");

        return tokenValue;
    }

    /**
     * 访问了拒绝访问的链接，响应结果
     * 将GlobalFilter拦截下来的请求,进行校验,并封装错误信息然后返回前端JSON
     * @param result
     * @param exchange
     * @return
     */
    private Mono<Void> responseResult(Result<String> result, ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();//获得响应的结果
        response.setStatusCode(HttpStatus.OK);//设置响应码
        Gson gson = new Gson();
        String jsonStr = gson.toJson(result);//将响应的结果转换为json

        //使用DataBuffer返回响应的结果
        DataBuffer dataBuffer = response.bufferFactory().wrap(jsonStr.getBytes());

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        return response.writeWith(Mono.just(dataBuffer));
    }


}
