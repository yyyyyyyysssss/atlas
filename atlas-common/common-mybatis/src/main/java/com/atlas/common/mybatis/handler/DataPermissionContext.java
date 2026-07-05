package com.atlas.common.mybatis.handler;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

public class DataPermissionContext implements AutoCloseable{

    private static final ThreadLocal<DataPermissionDescriptor> DESCRIPTOR_HOLDER = new ThreadLocal<>();


    public static DataPermissionContext open() {
        DESCRIPTOR_HOLDER.set(new DataPermissionDescriptor());
        return new DataPermissionContext();
    }

    public DataPermissionContext configure(Consumer<DataPermissionDescriptor> consumer) {
        DataPermissionDescriptor descriptor = DESCRIPTOR_HOLDER.get();
        if (descriptor != null) {
            consumer.accept(descriptor);
        }
        return this;
    }

    public static DataPermissionDescriptor getDescriptor() {
        return DESCRIPTOR_HOLDER.get();
    }

    public static boolean isEnabled() {
        return DESCRIPTOR_HOLDER.get() != null;
    }

    @Data
    @Accessors(chain = true)
    public static class DataPermissionDescriptor {

        private String orgField = "org_id";

        private String userField = "creator_id";

        private String alias = "";

        private String tableName = "";
    }

    @Override
    public void close() {
        DESCRIPTOR_HOLDER.remove();
    }
}
