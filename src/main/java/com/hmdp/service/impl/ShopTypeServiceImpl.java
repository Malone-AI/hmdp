package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 1. 从redis查询商户缓存
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        List<Object> shopTypeList = stringRedisTemplate.opsForHash().values(key);
        // 2. 判断是否存在
        if (!shopTypeList.isEmpty()) {
            // 刷新ttl
            stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
            // 3. 存在，转为ShopType
            List<ShopType> shopTypes = new ArrayList<>(shopTypeList.size());
            for (Object obj: shopTypeList) {
                ShopType shopType = JSONUtil.toBean((String)obj, ShopType.class);
                shopTypes.add(shopType);
            }
            // 排序
            shopTypes.sort(Comparator.comparing(ShopType::getSort));
            // 返回
            return Result.ok(shopTypes);
        }
        // 4. 不存在，查找数据库
        List<ShopType> shopTypes = query().orderByAsc("sort").list();
        // 5. 还是不存在，缓存空值后返回错误
        if (shopTypes == null) {
            // 缓存空值
            stringRedisTemplate.opsForHash().put(key, "", "");
            // 设置ttl
            stringRedisTemplate.expire(key,RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("商铺类型不存在");
        }
        // 6. 存在，写入redis
        Map<String, String> map = new HashMap<>(shopTypes.size());
        for (ShopType shopType: shopTypes) {
            map.put(shopType.getId().toString(), JSONUtil.toJsonStr(shopType));
        }
        // 写入redis
        stringRedisTemplate.opsForHash().putAll(key, map);
        // 设置ttl
        stringRedisTemplate.expire(key,RedisConstants.CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        // 7. 返回
        return Result.ok(shopTypes);
    }
}
