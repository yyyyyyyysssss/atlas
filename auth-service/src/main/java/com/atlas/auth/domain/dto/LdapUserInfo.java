package com.atlas.auth.domain.dto;

import lombok.Data;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * @Description
 * @Author ys
 * @Date 2026/6/12 14:25
 */
@Data
public class LdapUserInfo implements ThirdPartyUserIdentity {

    private String provider;

    private String sub;

    private String email;

    private Boolean emailVerified;

    private String fullName;

    private String avatar;

    private String phone;

    private Boolean phoneVerified;

    private Map<String, Object> extraInfo;

    public static LdapUserInfo fromAttributes(Attributes attributes, LdapProviderSettings.Mapping mappings) {
        if (attributes == null) {
            throw new IllegalArgumentException("LDAP attributes 不能为空");
        }
        if (mappings == null) {
            throw new IllegalArgumentException("LDAP mappings 不能为空");
        }
        LdapUserInfo ldapUserInfo = new LdapUserInfo();
        ldapUserInfo.setSub(readString(attributes, mappings.sub(), true));
        ldapUserInfo.setEmail(readString(attributes, mappings.email(), false));
        ldapUserInfo.setEmailVerified(true);
        ldapUserInfo.setPhone(readString(attributes, mappings.phone(), false));
        ldapUserInfo.setPhoneVerified(true);
        ldapUserInfo.setFullName(readString(attributes, mappings.fullName(), false));
        Map<String, Object> attributesMap = attributesToMap(attributes);
        ldapUserInfo.setExtraInfo(attributesMap);
        return ldapUserInfo;
    }

    /**
     * 读取单值属性。
     *
     * @param binaryGuid 是否将二进制值按 AD objectGUID 格式转换
     */
    private static String readString(Attributes attributes, String attributeName, boolean binaryGuid) {
        Object value = readValue(attributes, attributeName);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[] bytes) {
            return binaryGuid ? toGuid(bytes) : null;
        }
        return value.toString();
    }

    private static Boolean readBoolean(Attributes attributes, String attributeName) {
        String value = readString(attributes, attributeName, false);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        }
        throw new IllegalArgumentException("LDAP 布尔属性值不合法: " + attributeName + "=" + value);
    }

    private static String toGuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Invalid objectGUID");
        }
        // AD 的 objectGUID 使用 Windows GUID 字节布局：
        // Data1（4 字节）、Data2（2 字节）、Data3（2 字节）为小端序，
        // Data4（8 字节）保持原始顺序。
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        long data1 = Integer.toUnsignedLong(buffer.getInt());
        long data2 = Short.toUnsignedLong(buffer.getShort());
        long data3 = Short.toUnsignedLong(buffer.getShort());

        long mostSigBits = (data1 << 32) | (data2 << 16) | data3;
        long leastSigBits = buffer.order(ByteOrder.BIG_ENDIAN).getLong();

        return new UUID(mostSigBits, leastSigBits).toString();
    }

    private static String toSid(byte[] sid) {
        if (sid == null || sid.length < 8) {
            throw new IllegalArgumentException("Invalid SID");
        }

        int revision = Byte.toUnsignedInt(sid[0]);
        int subAuthorityCount = Byte.toUnsignedInt(sid[1]);

        // SID 结构：8 字节头部 + 每个 SubAuthority 4 字节。
        int expectedLength = 8 + subAuthorityCount * 4;

        if (sid.length != expectedLength) {
            throw new IllegalArgumentException("Invalid AD objectSid length: expected " + expectedLength + " bytes, got " + sid.length);
        }

        // Windows SID 当前定义的 Revision 为 1。
        if (revision != 1) {
            throw new IllegalArgumentException("Unsupported SID revision: " + revision);
        }

        // IdentifierAuthority 为 6 字节无符号大端整数。
        long identifierAuthority = 0;
        for (int i = 2; i < 8; i++) {
            identifierAuthority = (identifierAuthority << 8) | Byte.toUnsignedLong(sid[i]);
        }

        StringBuilder result = new StringBuilder()
                .append("S-")
                .append(revision)
                .append("-")
                .append(identifierAuthority);

        // SubAuthority 为 4 字节无符号小端整数。
        for (int i = 0; i < subAuthorityCount; i++) {
            int offset = 8 + i * 4;
            long subAuthority = Byte.toUnsignedLong(sid[offset])
                    | (Byte.toUnsignedLong(sid[offset + 1]) << 8)
                    | (Byte.toUnsignedLong(sid[offset + 2]) << 16)
                    | (Byte.toUnsignedLong(sid[offset + 3]) << 24);
            result.append("-").append(subAuthority);
        }

        return result.toString();
    }

    private static Object readValue(Attributes attributes, String attributeName) {

        if (isBlank(attributeName)) {
            return null;
        }
        try {
            Attribute attribute = attributes.get(attributeName);
            return attribute == null ? null : attribute.get();
        } catch (NamingException e) {
            throw new IllegalStateException("读取 LDAP 属性失败: " + attributeName, e);
        }
    }

    private static Map<String, Object> attributesToMap(Attributes attributes) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (attributes == null) {
            return result;
        }
        try {
            NamingEnumeration<? extends Attribute> all = attributes.getAll();
            while (all.hasMore()) {
                Attribute attribute = all.next();
                String name = attribute.getID();
                List<Object> values = new ArrayList<>();
                NamingEnumeration<?> enumeration = attribute.getAll();
                while (enumeration.hasMore()) {
                    Object value = enumeration.next();
                    if (value instanceof byte[] bytes) {
                        values.add(convertBinaryAttribute(name, bytes));
                    } else {
                        values.add(value);
                    }
                }

                if (values.isEmpty()) {
                    continue;
                }
                result.put(name, values.size() == 1 ? values.getFirst() : values);
            }

            return result;
        } catch (NamingException e) {
            throw new IllegalStateException("LDAP Attributes 转 Map 失败", e);
        }
    }

    private static Object convertBinaryAttribute(String name, byte[] bytes) {
        if ("objectGUID".equalsIgnoreCase(name)) {
            return toGuid(bytes);
        }
        if ("objectSid".equalsIgnoreCase(name)) {
            return toSid(bytes);
        }
        return bytes;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
