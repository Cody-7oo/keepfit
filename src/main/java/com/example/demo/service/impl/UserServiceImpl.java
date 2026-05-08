package com.example.demo.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.exception.BusinessException;
import com.example.demo.common.dto.UserInfoUpdateDTO;
import com.example.demo.common.dto.UserLoginDTO;
import com.example.demo.common.dto.UserRegisterDTO;
import com.example.demo.common.enums.ResultCodeEnum;
import com.example.demo.common.vo.UserVO;
import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import com.example.demo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // ====================== 已删除 Spring Security 依赖 ======================

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String USER_INFO_CACHE = "user:info:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    /**
     * 用户注册
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        log.info("[用户-注册] 手机号：{}", dto.getPhone());
        long start = System.currentTimeMillis();
        try {
            // 校验手机号是否已存在
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone());
            User existUser = getOne(wrapper);

            if (existUser != null) {
                throw new BusinessException(ResultCodeEnum.USER_PHONE_EXIST);
            }

            User user = new User();
            BeanUtils.copyProperties(dto, user);

            // ====================== 🔥 修改这里：Hutool BCrypt 加密 ======================
            user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt()));

            save(user);

            log.info("[用户-注册] 成功");
        } catch (BusinessException e) {
            log.warn("[用户-注册] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[用户-注册] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[用户-注册] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 手机号登录
     */
    @Override
    public UserVO login(UserLoginDTO dto) {
        log.info("[用户-登录] 手机号：{}", dto.getPhone());
        long start = System.currentTimeMillis();
        try {
            // 根据手机号查询用户
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getPhone, dto.getPhone());
            User user = getOne(wrapper);

            if (user == null) {
                throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
            }

            // ====================== 🔥 修改这里：密码校验 ======================
            if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
                throw new BusinessException(ResultCodeEnum.USER_PASSWORD_ERROR);
            }

            UserVO userVO = new UserVO();
            BeanUtils.copyProperties(user, userVO);
            log.info("[用户-登录] 成功，用户ID：{}", user.getId());
            return userVO;
        } catch (BusinessException e) {
            log.warn("[用户-登录] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[用户-登录] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[用户-登录] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 修改用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInfo(UserInfoUpdateDTO dto) {
        log.info("[用户-修改资料] ID：{}", dto.getId());
        long start = System.currentTimeMillis();
        try {
            User user = getById(dto.getId());
            if (user == null) {
                throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
            }

            // ✅ 安全：只修改资料，绝对不修改 ID
            user.setUsername(dto.getUsername());
            user.setHeight(dto.getHeight());
            user.setWeight(dto.getWeight());
            user.setSportLevel(dto.getSportLevel());

            updateById(user);

            // 清除缓存
            redisTemplate.delete(USER_INFO_CACHE + dto.getId());
            log.info("[用户-修改资料] 成功，清除缓存");
        } catch (BusinessException e) {
            log.warn("[用户-修改资料] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[用户-修改资料] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[用户-修改资料] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }

    /**
     * 查询用户信息（带Redis缓存）
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        log.info("[用户-查询信息] ID：{}", userId);
        long start = System.currentTimeMillis();
        try {
            String cacheKey = USER_INFO_CACHE + userId;
            UserVO cacheUser = (UserVO) redisTemplate.opsForValue().get(cacheKey);

            if (cacheUser != null) {
                log.info("[用户-查询信息] 命中Redis缓存");
                return cacheUser;
            }

            User user = getById(userId);
            if (user == null) {
                throw new BusinessException(ResultCodeEnum.USER_NOT_EXIST);
            }

            UserVO vo = new UserVO();
            BeanUtils.copyProperties(user, vo);
            redisTemplate.opsForValue().set(cacheKey, vo, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);

            log.info("[用户-查询信息] 存入Redis缓存");
            return vo;
        } catch (BusinessException e) {
            log.warn("[用户-查询信息] 业务异常：{}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[用户-查询信息] 系统异常：", e);
            throw new BusinessException(ResultCodeEnum.SYSTEM_ERROR);
        } finally {
            log.info("[用户-查询信息] 耗时：{}ms", System.currentTimeMillis() - start);
        }
    }
}