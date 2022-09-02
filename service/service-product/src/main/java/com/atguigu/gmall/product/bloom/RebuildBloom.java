package com.atguigu.gmall.product.bloom;

public interface RebuildBloom {
    /**
     * 重建指定的布隆过滤器
     * 传两个参数
     * 1.要重建的布隆过滤器的名称
     * 2.布隆过滤器要修改的值
     */
    public void rebuildBloom(String bloomName,BloomDataQueryService bloomDataQueryService);
}
