
import React from 'react';
import { Flex, Typography, theme, Divider, Checkbox, Table, Image } from 'antd';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';

const { Title, Text, Paragraph, Link } = Typography;

const MarkdownView = ({ content }) => {

    const { token } = theme.useToken()

    const markdownComponents = {
        // 1. 标题映射 (H1 - H4)
        h1: ({ children }) => <Title level={2} style={{ marginTop: 24, marginBottom: 16 }}>{children}</Title>,
        h2: ({ children }) => <Title level={3} style={{ marginTop: 20, marginBottom: 14 }}>{children}</Title>,
        h3: ({ children }) => <Title level={4} style={{ marginTop: 16, marginBottom: 12 }}>{children}</Title>,
        h4: ({ children }) => <Title level={5} style={{ marginTop: 12, marginBottom: 10 }}>{children}</Title>,

        // 2. 核心：解决缩进与标号缺失问题
        ul: ({ children }) => (
            <ul style={{
                paddingLeft: 28,
                marginBottom: 16,
                listStyleType: 'disc', // 强制开启圆点
                color: token.colorText
            }}>
                {children}
            </ul>
        ),
        ol: ({ children }) => (
            <ol style={{
                paddingLeft: 28,
                marginBottom: 16,
                listStyleType: 'decimal', // 强制开启数字 1. 2. 3.
                color: token.colorText
            }}>
                {children}
            </ol>
        ),
        li: ({ children }) => (
            <li style={{ marginBottom: 8, lineHeight: 1.8 }}>
                {children}
            </li>
        ),

        // 3. 段落与强调
        p: ({ children }) => <Paragraph style={{ marginBottom: '0', lineHeight: 1.8 }}>{children}</Paragraph>,
        strong: ({ children }) => <Text strong>{children}</Text>,
        em: ({ children }) => <Text italic>{children}</Text>,

        // 4. 链接与分割线
        a: ({ href, children }) => <Link href={href} target="_blank">{children}</Link>,
        hr: () => <Divider style={{ opacity: 0.6 }} />,

        // 引用块 (Blockquote)
        blockquote: ({ children }) => {

            return (
                <Flex
                    align="center"
                    style={{
                        margin: '16px 0',
                        padding: `${token.paddingXS}px ${token.paddingLG}px`,
                        background: token.colorFillAlter, // 自动适配暗色模式
                        borderLeft: `4px solid ${token.colorPrimaryBorder}`,
                        borderRadius: `0 ${token.borderRadiusSM}px ${token.borderRadiusSM}px 0`,
                    }}
                >
                    {/* 直接渲染 children，但通过 CSS 消除内部 p 标签的 margin */}
                    <div
                        style={{
                            color: token.colorTextSecondary,
                            fontSize: token.fontSize,
                            fontStyle: 'italic', // 增加一点设计感，符合“摘要”的语义
                        }}
                    >
                        {children}
                    </div>
                </Flex>
            )
        },
        table: MarkdownTable,
        // 任务列表适配
        input: ({ type, checked }) => {
            if (type === 'checkbox') {
                return <Checkbox checked={checked} style={{ marginRight: 8, verticalAlign: 'middle' }} readOnly />;
            }
            return null;
        },
        code: ({ className, children, ...props }) => {
            // 判定逻辑：
            // 1. 如果有 language- 前缀的 className，通常是代码块 (如 ```java)
            // 2. 如果 children 是多行文本，通常是代码块
            const isBlock = /language-(\w+)/.test(className || '') || String(children).includes('\n');

            if (!isBlock) {
                // --- 行内代码渲染 ---
                return (
                    <code
                        style={{
                            display: 'inline',
                            padding: '2px 4px',
                            margin: '0 4px',
                            borderRadius: token.borderRadiusSM,
                            backgroundColor: token.colorFillTertiary,
                            fontFamily: 'monospace',
                            fontSize: '0.9em',
                            border: `1px solid ${token.colorBorderSecondary}`,
                        }}
                        {...props}
                    >
                        {children}
                    </code>
                );
            }

            // --- 块级代码渲染 ---
            return (
                <pre
                    style={{
                        background: token.colorFillSecondary,
                        padding: 12,
                        borderRadius: token.borderRadius,
                        overflowX: 'auto',
                        marginBottom: 16,
                    }}
                >
                    <code className={className} {...props}>
                        {children}
                    </code>
                </pre>
            );
        },
        img: ({ src, alt }) => (
            <div style={{ textAlign: 'center', margin: '24px 0' }}>
                <Image
                    src={src}
                    alt={alt}
                    // 样式控制
                    style={{
                        maxWidth: '100%',
                        borderRadius: token.borderRadiusLG,
                        border: `1px solid ${token.colorBorderSecondary}`,
                        boxShadow: token.boxShadowTertiary,
                    }}
                />
                {/* 图片下方的描述文字 */}
                {alt && (
                    <div style={{ marginTop: 8 }}>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                            {alt}
                        </Text>
                    </div>
                )}
            </div>
        ),
    }

    return (
        <Flex vertical style={{ lineHeight: 1.8, overflowY: 'auto' }}>
            <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
                {content}
            </ReactMarkdown>
        </Flex>
    )
}

const MarkdownTable = ({ children }) => {
    // 1. 获取表头数据
    // children 通常包含 thead 和 tbody
    const thead = children.find(child => child.type === 'thead');
    const tbody = children.find(child => child.type === 'tbody');

    // 2. 解析列定义 (Columns)
    // 找到 thead -> tr -> th 列表
    const headerRow = thead?.props?.children;
    const headerCells = Array.isArray(headerRow) ? headerRow[0]?.props?.children : headerRow?.props?.children;

    const columns = React.Children.map(headerCells, (cell, index) => {
        return {
            title: cell.props.children,
            dataIndex: `col${index}`,
            key: `col${index}`,
            align: 'center',
            // 这里的 padding 和对齐可以根据你的设计感进行微调
            onHeaderCell: () => ({
                style: { fontWeight: 600 }
            })
        };
    });

    // 3. 解析行数据 (DataSource)
    // 找到 tbody -> tr 列表
    const bodyRows = tbody?.props?.children || [];

    const dataSource = React.Children.map(bodyRows, (row, rowIndex) => {
        const cells = row?.props?.children || [];
        const rowData = { key: rowIndex };

        React.Children.forEach(cells, (cell, cellIndex) => {
            rowData[`col${cellIndex}`] = cell.props.children;
        });

        return rowData;
    });

    return (
        <Table
            columns={columns}
            dataSource={dataSource}
            pagination={false}
            bordered
            size="middle"
            className="my-4 shadow-sm"
        />
    );
}

export default MarkdownView
