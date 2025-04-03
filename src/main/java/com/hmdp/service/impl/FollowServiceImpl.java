package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private IUserService userService;

    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 获取登陆用户ID
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        // 2.
        if (isFollow) {
            // 关注操作
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 把关注的用户id放到redis的集合中，key为follow:{当前用户id}，值为关注的用户id sadd userId followUserId
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            }
        } else {
            // 取关操作
            // 创建LambdaQueryWrapper
            QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
            // 指定字段等于某值
            queryWrapper.eq("user_id", userId).eq("follow_user_id", followUserId);
            boolean isSuccess = remove(queryWrapper);
            if (isSuccess) {
                // 把关注的用户id从redis集合中移除
                stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Long followUserId) {
        // 1. 获取登陆用户ID
        Long userId = UserHolder.getUser().getId();
        // 2. 先查redis缓存
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        Boolean member = stringRedisTemplate.opsForSet().isMember(key, followUserId.toString());
        if (member != null) {
            return Result.ok(member);
        }
        // 3. 再从数据库查询用户是否关注 SELECT COUNT(*) from `tb_follow` WHERE user_id = ? and follow_user_id = ?
        Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        // 4. 返回判断结果
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long id) {
        // 1. 获取登陆用户ID
        Long userId = UserHolder.getUser().getId();
        // 2. 查询redis缓存
        String key1 = RedisConstants.FOLLOW_USER_KEY + userId;
        String key2 = RedisConstants.FOLLOW_USER_KEY + id;
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(key1, key2);
        if (intersect == null || intersect.isEmpty()) {
            // 交集为空
            return Result.ok();
        }
        // 3. 解析交集中的id
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());
        List<UserDTO> userDTOs = userService.listByIds(ids)
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOs);
    }
}
