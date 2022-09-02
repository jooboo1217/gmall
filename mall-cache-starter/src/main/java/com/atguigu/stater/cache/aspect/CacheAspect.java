package com.atguigu.stater.cache.aspect;


import com.atguigu.stater.cache.annotation.GmallCache;
import com.atguigu.stater.cache.service.CacheOpsService;
import jodd.util.StringUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.MethodExecutor;
import org.springframework.expression.ParserContext;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

@Aspect//这是一个切面
@Component
public class CacheAspect {
  
    @Autowired
    CacheOpsService cacheOpsService;

    //创建表达式解析器
    ExpressionParser parser =  new SpelExpressionParser();
    ParserContext context =  new TemplateParserContext();


    //创建一个环绕通知

    @Around("@annotation(com.atguigu.stater.cache.annotation.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {//连接点
        Object result = null;//从数据库查询到的数据
        //key不同，方法可能不同
        String cacheKey = determinCacheKey(joinPoint);

        //1.先查看缓存
        //获取目标方法的精确返回值类型
        Type returnType = getMethodGenericReturnType(joinPoint);
        //获取缓存
        Object cacheData = cacheOpsService.getCacheData(cacheKey, returnType);

        //2.判断查询到的缓存
        if (cacheData == null){
            //3.计划回源
            //4.先问布隆，有些场景并不需要布隆，比如：三家分类（只有一个数据）
            //要查询某个布隆过滤器中的数据，首先的知道布隆过滤器的名称
            String bloomName = determinBloomName(joinPoint);
            //判断布隆是否存在
            if (!StringUtil.isEmpty(bloomName)){
                //开启了布隆
                Object bVal = determinBloomValue(joinPoint);
                //判断这个布隆名布隆值表达式是否存在
                boolean contains = cacheOpsService.bloomContains(bloomName, bVal);
                if (!contains){
                    return  null;
                }
            }
        //5.布隆说有，回源，但是有缓存击穿的风险，可以加锁解决
            boolean lock = false;
            String lockName = "";
            try {
                //不同的场景用自己的锁,获得锁名
                lockName = determinLockName(joinPoint);
                //尝试加锁
                lock = cacheOpsService.tryLock(lockName);
                //判断是否加锁成功
                if (lock){
                    
                //6.获得锁，开始回源
                result = joinPoint.proceed(joinPoint.getArgs());    
                //7.将查询到的数据保存到缓存中
                    cacheOpsService.saveData(cacheKey,result);
                    //返回查询结果
                    return result;
                }else {
                    //没抢到锁的，睡一秒，从缓存中拿
                    Thread.sleep(1000);
                    return cacheOpsService.getCacheData(cacheKey,returnType);
                }
            }finally {
                //8.释放锁
                if (lock)//如果有锁就释放
                cacheOpsService.unlock(lockName);
            }

        }
        //缓存中有直接返回
        return cacheData;

    }

    /**
     * 回源前加锁，获得锁的名称
     * @param joinPoint
     * @return
     */
    private String determinLockName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache gmallCache = method.getDeclaredAnnotation(GmallCache.class);
        String lockName = gmallCache.lockName();
        if (StringUtils.isEmpty(lockName)){
            return "lock" +method.getName();
        }
        //4、计算锁值
        String lockNameVal = evaluationExpression(lockName, joinPoint, String.class);
        return lockNameVal;
    }

    /**
     * 得出布隆过滤器的名称
     * @param joinPoint
     * @return
     */
    private String determinBloomName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        GmallCache declaredAnnotation = method.getDeclaredAnnotation(GmallCache.class);
        String bloomName = declaredAnnotation.bloomName();
        return bloomName;
    }

    /**
     * 查询布隆过滤器的
     * @param joinPoint
     * @return
     */
    private Object determinBloomValue(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        //拿到注解
        GmallCache gmallCache = method.getDeclaredAnnotation(GmallCache.class);
        String bloomValue = gmallCache.bloomValue();

        //拿到布隆值表达式
        Object expression = evaluationExpression(bloomValue, joinPoint, Object.class);

        return expression;
    }

    /**
     *获取目标方法的精确返回值类型
     * @param joinPoint
     * @return
     */
    private Type getMethodGenericReturnType(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Type type = method.getGenericReturnType();
        return type;
    }

    /**
     * 根据当前连接点的信息，确定缓存用什么key,从连接点中查出
     * @param joinPoint
     * @return
     */
    private String determinCacheKey(ProceedingJoinPoint joinPoint) {
        //1.拿到目标方法上的GamllCache注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Method method = signature.getMethod();

        //2.拿到注解
       GmallCache cacheAnntation = method.getDeclaredAnnotation(GmallCache.class);

        String expression = cacheAnntation.cacheKey();

        //3.根据表达式计算缓存键
        String cacheKey =  evaluationExpression(expression,joinPoint,String.class);

        return cacheKey;
    }
        //将参数中的值通过计算表达式计算出来
    private <T> T evaluationExpression(String expression,
                                        ProceedingJoinPoint joinPoint,
                                        Class<T> clz) {
        //1.得到表达式
        Expression exp = parser.parseExpression(expression, context);
        //2.创建标准的计算上下文
        StandardEvaluationContext standardEvaluationContext = new StandardEvaluationContext();
        //3.去除所有的参数，绑定到上下文
        Object[] args = joinPoint.getArgs();
        standardEvaluationContext.setVariable("params",args);

        //4.得到表达式的值
        T expValue = exp.getValue(standardEvaluationContext, clz);

        return expValue;
    }
}
