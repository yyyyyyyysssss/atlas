package com.atlas.user.utils;

import net.datafaker.Faker;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/19 10:48
 */
public class NameGenerator {

    private static final Faker FAKER = new Faker(Locale.CHINA);

    private static final String[] PREFIX = {
            "快乐", "幸运", "机智", "神秘",
            "勇敢", "闪耀", "自由", "温柔",
            "可爱", "元气", "咸鱼", "佛系"
    };

    private static final String[] SUFFIX = {
            "小猫", "小狐", "小熊", "海豚",
            "白鹿", "松鼠", "鲸鱼", "团子",
            "布丁", "星星", "奶茶", "月亮"
    };

    public static String generateFunnyName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        return PREFIX[random.nextInt(PREFIX.length)]
                + SUFFIX[random.nextInt(SUFFIX.length)];
    }

}
