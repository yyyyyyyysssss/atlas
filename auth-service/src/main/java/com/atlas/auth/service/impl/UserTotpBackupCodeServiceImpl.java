package com.atlas.auth.service.impl;

import com.atlas.auth.domain.entity.UserTotpBackupCode;
import com.atlas.auth.mapper.UserTotpBackupCodeMapper;
import com.atlas.auth.service.UserTotpBackupCodeService;
import com.atlas.common.core.idwork.IdGen;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;


/**
 * (UserTotpBackupCode)表服务实现类
 *
 * @author ys
 * @since 2026-05-28 14:22:20
 */
@Service("userTotpBackupCodeService")
@RequiredArgsConstructor
@Slf4j
public class UserTotpBackupCodeServiceImpl extends ServiceImpl<UserTotpBackupCodeMapper, UserTotpBackupCode> implements UserTotpBackupCodeService {

    private final UserTotpBackupCodeMapper userTotpBackupCodeMapper;


    private final PasswordEncoder passwordEncoder;

    private static final char[] CHAR_POOL = "23456789abcdefghjkmnpqrstuvwxyz".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_COUNT = 16;

    /**
     * 重新生成并刷新用户的备份码
     *
     * @param userId 用户 ID
     * @return 返回给前端展示的 16 个明文备份码列表（只有这一次机会获取明文）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> refreshBackupCodes(Long userId) {
        // 生成 16 个明文码
        List<String> plainCodes = new ArrayList<>(CODE_COUNT);
        for (int i = 0; i < CODE_COUNT; i++) {
            plainCodes.add(generateSingleBackupCode());
        }

        // 物理删除该用户之前残留的所有老旧备份码
        this.remove(Wrappers.<UserTotpBackupCode>lambdaQuery().eq(UserTotpBackupCode::getUserId, userId));

        // 批量哈希落库
        List<UserTotpBackupCode> entityList = new ArrayList<>(CODE_COUNT);
        for (String plain : plainCodes) {
            UserTotpBackupCode entity = new UserTotpBackupCode();
            entity.setId(IdGen.genId());
            entity.setUserId(userId);
            entity.setBackupCode(passwordEncoder.encode(plain));
            entityList.add(entity);
        }

        // 批量插入数据库
        this.saveBatch(entityList);

        log.info("用户 [{}] 成功重新生成了 16 个 安全备份码。", userId);

        // 返回明文给前端
        return plainCodes;
    }

    /**
     * 校验并核销备份码（用完即删）
     *
     * @param userId    用户 ID
     * @param inputCode 用户登录时输入的明文备份码（如 ab23c-def45）
     * @return 是否验证通过
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean verifyAndConsume(Long userId, String inputCode) {
        if (inputCode == null || inputCode.isBlank()) {
            return false;
        }

        // 允许用户不小心输入大写或前后带空格，清洗成标准小写
        String cleanInput = inputCode.trim().toLowerCase();

        // 查询该用户数据库中现存的所有哈希备份码
        List<UserTotpBackupCode> dbCodes = this.list(
                Wrappers.<UserTotpBackupCode>lambdaQuery().eq(UserTotpBackupCode::getUserId, userId)
        );

        // 遍历进行 BCrypt 匹配
        for (UserTotpBackupCode dbRecord : dbCodes) {
            if (passwordEncoder.matches(cleanInput, dbRecord.getBackupCode())) {
                // 匹配成功：执行极简设计，用完即删（物理删除）
                this.removeById(dbRecord.getId());

                log.info("用户 [{}] 成功使用备份码进行多因素登录验证，该备份码 ID [{}] 已执行物理销毁。", userId, dbRecord.getId());
                return true;
            }
        }
        log.warn("用户 [{}] 尝试使用备份码登录，但未匹配成功。", userId);
        return false;
    }

    @Override
    public int countRemainingCodes(Long userId) {
        if (userId == null) {
            return 0;
        }
        long count = this.count(Wrappers.<UserTotpBackupCode>lambdaQuery()
                .eq(UserTotpBackupCode::getUserId, userId));

        return (int) count;
    }

    @Override
    public boolean removeByUserId(Long userId) {
        if (userId == null) {
            return false;
        }
        return this.remove(new LambdaQueryWrapper<UserTotpBackupCode>()
                .eq(UserTotpBackupCode::getUserId, userId)
        );
    }

    private static String generateSingleBackupCode() {
        StringBuilder sb = new StringBuilder(11);
        // 前 5 位
        for (int i = 0; i < 5; i++) {
            sb.append(CHAR_POOL[SECURE_RANDOM.nextInt(CHAR_POOL.length)]);
        }
        // 连字符
        sb.append("-");
        // 后 5 位
        for (int i = 0; i < 5; i++) {
            sb.append(CHAR_POOL[SECURE_RANDOM.nextInt(CHAR_POOL.length)]);
        }
        return sb.toString();
    }

    
}

