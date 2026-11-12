import { Button, Flex, Form, Input } from "antd";
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';

const AttributeMappingForm = ({
    name = 'attributeMappings',
    label = '属性映射：',
    extra = '将外部身份提供商的用户属性映射为 Atlas 用户属性。',
    sourceField = 'sourceAttribute',
    sourceLabel = '外部属性',
    sourcePlaceholder = '例如 email',
    sourceRequiredMessage = '请输入外部属性名称',
    targetField = 'userAttribute',
    targetLabel = 'Atlas 用户属性',
    targetPlaceholder = '例如 email',
    minItems = 1,
}) => {


    return (
        <Form.Item label={label} extra={extra}>
            <Form.List
                name={name}
                rules={[
                    {
                        validator: async (_, mappings) => {
                            if (!mappings || mappings.length < minItems) {
                                throw new Error(
                                    `请至少添加 ${minItems} 条属性映射`,
                                );
                            }

                            const names = mappings
                                .map((item) =>
                                    item?.[sourceField]?.trim(),
                                )
                                .filter(Boolean);

                            if (new Set(names).size !== names.length) {
                                throw new Error(
                                    `${sourceLabel}名称不能重复`,
                                );
                            }
                        },
                    },
                ]}
            >
                {(fields, { add, remove }, { errors }) => (
                    <Flex vertical gap={12}>
                        {fields.map((field) => (
                            <Flex
                                key={field.key}
                                gap={8}
                                align="start"
                            >
                                <Form.Item
                                    name={[field.name, sourceField]}
                                    rules={[
                                        {
                                            required: true,
                                            whitespace: true,
                                            message: `请输入${sourceLabel}名称`,
                                        },
                                    ]}
                                    style={{
                                        flex: 1,
                                        minWidth: 0,
                                        marginBottom: 0,
                                    }}
                                >
                                    <Input
                                        placeholder={`${sourceLabel}，${sourcePlaceholder}`}
                                    />
                                </Form.Item>

                                <Form.Item
                                    name={[field.name, targetField]}
                                    rules={[
                                        {
                                            required: true,
                                            whitespace: true,
                                            message: '请输入 Atlas 用户属性',
                                        },
                                    ]}
                                    style={{
                                        flex: 1,
                                        minWidth: 0,
                                        marginBottom: 0,
                                    }}
                                >
                                    <Input
                                        placeholder={`${targetLabel}，${targetPlaceholder}`}
                                    />
                                </Form.Item>

                                <Button
                                    type="text"
                                    danger
                                    icon={<DeleteOutlined />}
                                    onClick={() => remove(field.name)}
                                />
                            </Flex>
                        ))}

                        <Form.ErrorList errors={errors} />

                        <Button
                            type="dashed"
                            icon={<PlusOutlined />}
                            onClick={() =>
                                add({
                                    [sourceField]: '',
                                    [targetField]: '',
                                })
                            }
                            block
                        >
                            添加属性映射
                        </Button>
                    </Flex>
                )}
            </Form.List>
        </Form.Item>
    );
}

export default AttributeMappingForm;