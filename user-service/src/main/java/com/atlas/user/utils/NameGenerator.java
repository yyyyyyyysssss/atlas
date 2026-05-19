package com.atlas.user.utils;

import net.datafaker.Faker;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Random;

/**
 * @Description
 * @Author ys
 * @Date 2026/5/19 10:48
 */
public class NameGenerator {

    private static final Faker faker = new Faker(new Locale("zh-CN"));

    static String[] expressions = {
            // === 基础经典款（你原本的） ===
            "#{mood.feeling}的#{animal.name}",         // 绝望的考拉、傲娇的火烈鸟
            "#{mood.feeling}的#{food.fruit}",          // 疯狂的猕猴桃、骄傲的水蜜桃
            "#{mood.feeling}的#{food.vegetableOrFruit}",// 忧郁的西兰花、快乐的胡萝卜
            "#{mood.feeling}的#{superhero.name}",       // 微醺的蜘蛛侠、纳闷的蝙蝠侠

            // === 新增：中二/玄幻角色款 ===
            "#{mood.feeling}的#{witcher.witcher}",      // 示例：傲娇的杰洛特、崩溃的叶奈法（巫师系列）
            "#{mood.feeling}的#{rickAndMorty.character}",// 示例：微醺的莫蒂、纳闷的姥爷（瑞克和莫蒂）
            "#{mood.feeling}的#{onePiece.character}",   // 示例：发呆的路飞、迷路的索隆（海贼王）
            "#{mood.feeling}的#{harryPotter.characters}",// 示例：焦虑的哈利、自信的赫敏（哈利波特）

            // === 新增：奇葩职业/身份款 ===
            "#{mood.feeling}的#{job.title}",            // 示例：绝望的程序员、优雅的保安、狂躁的产品经理
            "#{mood.feeling}的#{detective.name}",       // 示例：摸鱼的福尔摩斯、微醺的柯南
            "#{mood.feeling}的#{ancient.hero}",         // 示例：发呆的曹操、傲娇的诸葛亮（中国古代历史人物）

            // === 新增：吃货与日常物品款 ===
            "#{mood.feeling}的#{food.dish}",            // 示例：崩溃的宫保鸡丁、优雅的螺蛳粉、微醺的锅包肉
            "#{mood.feeling}的#{dessert.name}",         // 示例：傲娇的马卡龙、忧郁的提拉米苏
            "#{mood.feeling}的#{coffee.name}",          // 示例：焦虑的生椰拿铁、狂躁的美式

            // === 新增：科技与神秘自然款 ===
            "#{mood.feeling}的#{space.nasaSpaceCraft}",  // 示例：孤独的旅行者号、发呆的阿波罗号
            "#{mood.feeling}的#{weather.description}"    // 示例：傲娇的阵雨、疯狂的龙卷风
    };

    static Random random = new SecureRandom();

    public static String generateFunnyName() {
        int randomIndex = random.nextInt(expressions.length);
        String selectedExpression = expressions[randomIndex];
        return faker.expression(selectedExpression);
    }

}
