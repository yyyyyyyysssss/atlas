import { Typography } from "antd";
import CardRenderer from "./renderers/CardRenderer";
import FileRenderer from "./renderers/FileRenderer";
import TextRenderer from "./renderers/TextRenderer";
import React from "react";

const { Text } = Typography

const RENDERER_MAP = {
    TEXT: TextRenderer,
    FILE: FileRenderer,
    CARD: CardRenderer,
};

const MessageRenderer = ({ content, onClose, onAction }) => {

    const { renderType, body } = content

    const Renderer = RENDERER_MAP[renderType]

    if (!Renderer) {
        return <Text type="secondary">未知类型</Text>;
    }

    return (
        <Renderer
            content={body}
            onClose={onClose}
            onAction={(action) => onAction?.(content, action)}
        />
    )
}

export default React.memo(MessageRenderer)