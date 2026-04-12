package com.atlas.common.core.idwork;

public class IdGen {

    private static SnowflakeIdWorker worker;

    public static void init(SnowflakeIdWorker snowflakeIdWorker) {
        worker = snowflakeIdWorker;
    }

    public static Long genId() {
        if (worker == null) {
            throw new RuntimeException("ID生成器尚未初始化");
        }
        return worker.nextId();
    }

}
